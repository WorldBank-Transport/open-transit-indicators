package opentransitgt

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.github.nscala_time.time.Imports._
import geotrellis.proj4._
import geotrellis.vector.Line
import geotrellis.vector.MultiLine
import org.joda.time.PeriodType
import opentransitgt.DjangoAdapter._
import opentransitgt.indicators._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.{Database, Session, DatabaseDef}

// Calculator interface for a particular indicator
trait IndicatorCalculator {
  val gtfsData: GtfsData
  val calcParams: CalcParams
  val name: String
  val db: DatabaseDef

  // Per-period calculations by each aggregation type
  // TODO: calculator results should be cached to prevent recalculating them during aggregations
  def calcByRoute(period: SamplePeriod): Map[String, Double]
  def calcByMode(period: SamplePeriod): Map[Int, Double]

  // Overall aggregation by route, taking into account all periods
  def calcOverallByRoute: Map[String, Double] = {
    // Map of route id -> calculation sums
    val sumsByRoute = collection.mutable.Map[String, Double]() ++
      gtfsData.routes.map(route => route.id.toString -> 0.0).toMap

    calcParams.sample_periods.foreach(period => {
      for ((route, value) <- calcByRoute(period)) {
        sumsByRoute(route) +=  value * getPeriodMultiplier(period)
      }
    })

    // divide by the number of hours in a week for the overall average
    sumsByRoute.map{ case (route, sum) => route -> sum / (24 * 7) }.toMap
  }

  // Overall aggregation by mode, taking into account all periods
  def calcOverallByMode: Map[Int, Double] = {
    // Map of route type -> calculation sums
    val sumsByMode = collection.mutable.Map[Int, Double]() ++
      gtfsData.routes.groupBy(_.route_type.id).keys.map(_ -> 0.0).toMap

    calcParams.sample_periods.foreach(period => {
      for ((routeType, value) <- calcByMode(period)) {
        sumsByMode(routeType) += value * getPeriodMultiplier(period)
      }
    })

    // divide by the number of hours in a week for the overall average
    sumsByMode.map{ case (routeType, sum) => routeType -> sum / (24 * 7) }.toMap
  }

  // This can be uncommented only when the proper methods are added to each indicator
  // until then, uncommenting this bit of code will prevent compilation
  //def calcBySystem(period: SamplePeriod): Double
  /*def calcOverallBySystem: Double = {
    // Double of systemic value
    calcParams.sample_periods.map(period => {
      calcBySystem(period) * getPeriodMultiplier(period)
    }).foldLeft(0.0){ (a, b) => a + b }
  }*/

  // Gets the multiplier for weighting a period in an aggregation
  def getPeriodMultiplier(period: SamplePeriod): Double = {
    val hours = hoursDifference(period.period_start.toLocalDateTime,
      period.period_end.toLocalDateTime)

    // The multiplier is the number of hours * (2 if weekend, 5 otherwise).
    val dayMultiplier = if (period.`type` == "weekend") 2 else 5
    dayMultiplier * hours
  }

  // Store all route indicators for all periods
  lazy val routeIndicators = for {
    period <- calcParams.sample_periods
    (route, value) <- calcByRoute(period)
  } yield {
    Indicator(
      `type`=name,
      sample_period=period.`type`,
      aggregation="route",
      route_id=route,
      version=calcParams.version,
      value=value,
      the_geom=stringGeomForRouteId(period, route)
    )
  }

  // Store all mode indicators for all periods
  lazy val modeIndicators = for {
    period <- calcParams.sample_periods
    (mode, value) <- calcByMode(period)
  } yield {
    Indicator(
      `type`=name,
      sample_period=period.`type`,
      aggregation="mode",
      route_type=mode,
      version=calcParams.version,
      value=value,
      the_geom=stringGeomForRouteMode(period, mode)
    )
  }

  // See note above regarding uncommenting
  /*lazy val systemIndicators = for {
    period <- calcParams.sample_periods
    value = calcBySystem(period)
  } yield {
    Indicator(
      `type`=name,
      sample_period=period.`type`,
      aggregation="system",
      version=calcParams.version,
      value=value,
      the_geom=stringGeomForSystem(period)
    )
  }*/

  // Store aggregate route indicators
  lazy val aggRouteIndicators = for {
    (route, value) <- calcOverallByRoute
  } yield {
    Indicator(
      `type`=name,
      sample_period="alltime",
      aggregation="route",
      route_id=route,
      version=calcParams.version,
      value=value
    )
  }

  // Store aggregate mode indicators
  lazy val aggModeIndicators = for {
    (mode, value) <- calcOverallByMode
  } yield {
    Indicator(
      `type`=name,
      sample_period="alltime",
      aggregation="mode",
      route_type=mode,
      version=calcParams.version,
      value=value
    )
  }

  // Post all indicators at once
  def storeIndicators = {
    djangoClient.postIndicators(calcParams.token, routeIndicators ++ modeIndicators ++
      aggRouteIndicators ++ aggModeIndicators)
  }

  // Return a text geometry with SRID 4326 for a given routeID
  def stringGeomForRouteId(period: SamplePeriod, routeID: String) = {
    lineForRouteIDLatLng(period)(routeID) match {
      case None => ""
      case Some(routeLine) => routeLine.toString
    }
  }

  // Return a text geometry with SRID 4326 for a given routeMode
  def stringGeomForRouteMode(period: SamplePeriod, routeMode: Int) = {
    multiLineForRouteModeLatLng(period)(routeMode) match {
      case None => ""
      case Some(modeLines) => modeLines.toString
    }
  }

  // Return a text geometry with SRID 4326 for all routes
  def stringGeomForSystem(period: SamplePeriod) = {
    multiLineForSystemLatLng(period) match {
      case None => ""
      case Some(allLines) => allLines.toString
    }
  }

  def routeToLine(period: SamplePeriod, route: Route): Option[Line] = {
    db withSession { implicit session: Session =>
      // Find the SRID of the UTM column and construct a CRS.
      //
      // Note: this is only needed because there is currently a bug
      //   in the GTFS parser that causes the SRID on the geometry object
      //   to be set to -1. If this is fixed, it can be obtained from
      //   the object directly.
      val sridQ = Q.queryNA[Int]("""SELECT Find_SRID('public', 'gtfs_shape_geoms', 'geom');""")
      val utmCrs = CRS.fromName(s"EPSG:${sridQ.list.head}")

      val shape_id : Option[String] = tripsInPeriod(period, route).head.rec.shape_id
      val shapeTrip : Option[TripShape] = shape_id.flatMap {
        shapeID => gtfsData.shapesById.get(shapeID)
      }
      val maybeShapeLine : Option[Line] = shapeTrip map {
        tripShape => tripShape.line.reproject(utmCrs, LatLng)(4326)
      }
      maybeShapeLine
    }
  }

  // Returns a maybe Multiline for the whole system in lat/long coordinates.
  // Assumes that all shapes for a given trip are the same, may not be valid.
  def multiLineForSystemLatLng(period: SamplePeriod): Option[MultiLine] = {
    val routesThisPeriod = routesInPeriod(period)
    val allRouteLines = routesThisPeriod map (route => routeToLine(period, route).get)
    Some(MultiLine(allRouteLines: _*))
    }

  // Returns map from route type to some multiline in lat/long coordinates.
  // Assumes that all shapes for a given trip are the same, may not be valid.
  def multiLineForRouteModeLatLng(period: SamplePeriod): Map[Int, Option[MultiLine]] = {
    val routesThisPeriod = routesInPeriod(period)
    val linesByMode = routesThisPeriod.map(
      route => route.route_type.id -> routeToLine(period, route).get
    ).toList
    // Group lines by type and feed the grouped lines as params to the MultiLine constructor
    val groupedLinesByMode = linesByMode groupBy (_._1)
    groupedLinesByMode map { groupedLines =>
      val routeMode = groupedLines._1
      def groupOfLines = { // this block breaks routeLines out of their nested map
        val theLines = for {
          p <- groupedLines._2
          q = p._2
        } yield q
        theLines.toList
      }
      routeMode -> Some(MultiLine(groupOfLines: _*))
    }
  }

  // Returns Option[Line] for the given routeID in lat/long coordinates.
  // Assumes that all shapes for a given trip are the same, may not be valid.
  def lineForRouteIDLatLng(period: SamplePeriod): Map[String, Option[Line]] = {
    val routesThisPeriod = routesInPeriod(period)
    val linesByMode = routesThisPeriod.map(
      route => route.id.toString -> routeToLine(period, route)
    ).toMap
    linesByMode
  }

  // Returns all scheduled trips for this route during the period
  def tripsInPeriod(period: SamplePeriod, route: Route): Seq[ScheduledTrip] = {
    // Importing the context within this scope adds additional functionality to Routes
    import gtfsData.context._

    val startDT = period.period_start.toLocalDateTime()
    val endDT = period.period_end.toLocalDateTime()

    route.getScheduledTripsBetween(startDT, endDT)
  }

  // Returns all stops occuring during the period for the specified scheduled trip
  def stopsInPeriod(period: SamplePeriod, trip: ScheduledTrip): Array[StopDateTime] = {
    val startDT = period.period_start.toLocalDateTime()
    val endDT = period.period_end.toLocalDateTime()

    trip.stops.filter(stop =>
       stop.arrival.isAfter(startDT) && stop.arrival.isBefore(endDT)
    )
  }

  // Gets the hours difference between two periods
  def hoursDifference(t1: LocalDateTime, t2: LocalDateTime): Double = {
      new Period(t1, t2, PeriodType.seconds()).getSeconds / 60.0 / 60.0
  }

  // Gets the differences between stop times
  def calcStopDifferences(stops: Array[StopDateTime]): Array[Double] = {
    stops.zip(stops.tail) map (pair => hoursDifference(pair._1.arrival, pair._2.arrival))
  }

  // Routes that fall within each period
  lazy val routesInPeriod: Map[SamplePeriod, Array[Route]] = {
    calcParams.sample_periods.map(period =>
      (period, gtfsData.routes.filter { tripsInPeriod(period, _).size > 0 })
    ).toMap
  }

  // Routes by route ID
  lazy val routeByID: Map[String, Route] = {
    gtfsData.routes.map(route => route.id.toString -> route).toMap
  }
}
