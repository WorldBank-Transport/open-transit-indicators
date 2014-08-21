package opentransitgt

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.github.nscala_time.time.Imports._
import geotrellis.proj4._
import geotrellis.vector.Line
import org.joda.time.PeriodType
import opentransitgt.DjangoAdapter._
import opentransitgt.indicators._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.{Database, Session}

// Calculator interface for a particular indicator
trait IndicatorCalculator {
  val gtfsData: GtfsData
  val calcParams: CalcParams
  val name: String

  // Per-period calculations by each aggregation type
  def calcByRoute(period: SamplePeriod): Map[String, Double]
  def calcByMode(period: SamplePeriod): Map[Int, Double]

  // System calculation
  def calcBySystem(period: SamplePeriod): Double = {
    // TODO: system calculations have not been implemented yet.
    0.0
  }

  // Overall aggregation, taking into account all periods
  def calcOverall: Double = {
    // TODO: overall calculations have not been implemented yet.
    0.0
  }

  // Stores all calculation results for this indicator
  def storeIndicator = {
    calcParams.sample_periods.foreach(period => {
      // Per route
      for ((route, value) <- calcByRoute(period)) {
        djangoClient.postIndicator(calcParams.token, Indicator(
          `type`=name, sample_period=period.`type`, aggregation="route",
          route_id=route, version=calcParams.version, value=value,
          the_geom=stringGeomForRouteId(period, route))
        )
      }

      // Per mode
      for ((mode, value) <- calcByMode(period)) {
        djangoClient.postIndicator(calcParams.token, Indicator(
          `type`=name, sample_period=period.`type`, aggregation="mode",
          route_type=mode, version=calcParams.version, value=value)
        )
      }
    })
  }

  // Return a text geometry with SRID 4326 for a given routeID
  def stringGeomForRouteId(period: SamplePeriod, routeID: String) = {
    lineForRouteIDLatLng(period)(routeID) match {
      case None => ""
      case Some(routeLine) => routeLine.toString
    }
  }

  // Returns Option[Line] for the given routeID in lat/long coordinates.
  // Assumes that all shapes for a given trip are the same, may not be valid.
  def lineForRouteIDLatLng(period: SamplePeriod): Map[String, Option[Line]] = {
    GeoTrellisService.db withSession { implicit session: Session =>
      // Find the SRID of the UTM column and construct a CRS.
      //
      // Note: this is only needed because there is currently a bug
      //   in the GTFS parser that causes the SRID on the geometry object
      //   to be set to -1. If this is fixed, it can be obtained from
      //   the object directly.
      val sridQ = Q.queryNA[Int]("""SELECT Find_SRID('public', 'gtfs_shape_geoms', 'geom');""")
      val utmCrs = CRS.fromName(s"EPSG:${sridQ.list.head}")

      routesInPeriod(period).map(route =>
        route.id.toString -> {
          val shape_id : Option[String] = tripsInPeriod(period, route).head.rec.shape_id

          val shapeTrip : Option[TripShape] = shape_id.flatMap {
            shapeID => gtfsData.shapesById.get(shapeID)
          }
          val shapeLine : Option[Line] = shapeTrip.map {
            tripShape => tripShape.line.reproject(utmCrs, LatLng)(4326)
          }
          shapeLine
        }
      ).toMap
    }
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

  // Gets a list of durations between stops per route
  def durationsBetweenStopsPerRoute(period: SamplePeriod): Map[String, Seq[Double]] = {
    routesInPeriod(period).map(route =>
      route.id.toString -> {
        tripsInPeriod(period, route).map(trip => {
          calcStopDifferences(trip.stops).map(_ * 60.0)
        }).flatten
      }
    ).toMap
  }

  // Gets the differences between stop times
  def calcStopDifferences(stops: Array[StopDateTime]): Array[Double] = {
    stops.zip(stops.tail).map(pair =>
      new Period(pair._1.arrival, pair._2.arrival, PeriodType.seconds()).getSeconds / 60.0 / 60.0 )
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
