package opentransitgt.indicators

import com.azavea.gtfs.data._
import com.azavea.gtfs.{ScheduledTrip, Route => GtfsRoute}
import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.JdbcBackend.DatabaseDef
import grizzled.slf4j.Logging

import geotrellis.vector._
import geotrellis.slick._
import opentransitgt.DjangoAdapter._

import opentransitgt._
import opentransitgt.data._

// Number of stops
class RatioTransToRoads(val gtfsData: GtfsData, val calcParams: CalcParams, val db: DatabaseDef) extends IndicatorCalculator with OSMCalculatorComponent with Logging {
  val name = "lines_roads"
  // Wrap Slick persistence items to prevent potential naming conflicts.
  import sbSlick.profile.simple._
  import sbSlick.gis._

  /**
  Save processing by memoizing prior calculations in this function block:
  (This doesn't actually save us calculations right now - the opposite - we need to refactor)
  */
  var storedData: (Map[String,Double], Map[Int, Double], Double) = (Map(), Map(), -1.7)
  def saveSomeTime(period: SamplePeriod, f: => (Map[String, Double], Map[Int, Double], Double)): (Map[String, Double], Map[Int, Double], Double) = {
    storedData match {
      case (_, _, -1.7) => storedData = f
    }
    storedData
  }

  def memoizedDerivation(period: SamplePeriod): (Map[String, Double], Map[Int, Double], Double) = {
    debug("In memoized derivation of all indicators")
    debug("Grabbing road lengths...")
    /**
    Lazily grab the length of roads
    */
    lazy val roadLength: Double = {
      debug("getting road length")
      val roadLines: List[Line] = allRoads map ((x: Road)  => x.geom.geom) // the second geom to project
      val distinctRoadLines: Array[Line] = MultiLine(roadLines: _*).lines
      debug("Length of roadlines:")
      debug(distinctRoadLines.map(x => x.length).sum/1000)
      distinctRoadLines.map(x => x.length).sum/1000
    }

    /**
    Retrieve trip's shape (maybe)
    */
    def getTripShape(trip: ScheduledTrip): Option[Line] = {
      trip.rec.shape_id match {
        case Some(shapeID) => gtfsData.shapesById.get(shapeID) map (_.line)
        case None => None
      }
    }

    def uniqueLines(ls: Seq[Line]): Array[Line] = {
      val mlFull = MultiLine(MultiLine(ls).jtsGeom.union.asInstanceOf[com.vividsolutions.jts.geom.MultiLineString])
      mlFull.lines
    }

    /**
    Generate Mapping from Route to route length for use in later calculations
    */
    val allRouteLengths: Map[GtfsRoute, Double] = {
      debug("Determining all route lengths...")
      val routes: Array[GtfsRoute] = routesInPeriod(period)
      val routeTrips: Map[GtfsRoute, Seq[ScheduledTrip]] = routes.map { x: GtfsRoute =>
        x -> tripsInPeriod(period, x)
      }.toMap
      val routeTripLines: Map[GtfsRoute, Seq[Line]] = routeTrips.map { case (k, v) =>
        k -> v.flatMap(getTripShape(_))
      }.filterNot { case (k, v) => v.isEmpty }
      val uniqueRouteTripLines: Map[GtfsRoute, Array[Line]] = routeTripLines.map { case (k, v) =>
        k -> uniqueLines(v)
      }
      val routeLengths: Map[GtfsRoute, Array[Double]] = uniqueRouteTripLines.map { case (k, v) =>
        k -> v.map(_.length)
      }
      val uniqueRouteLengths: Map[GtfsRoute, Double] = routeLengths map { case (k, v) =>
        val maxTrip = v.reduce(_ max _)
        k -> maxTrip/1000
      }
      uniqueRouteLengths.map({case (k, v) => debug(f"Route Length calculated: ${v}km")})
      uniqueRouteLengths
    }

    debug("Writing route value for trans-to-road ratio")
    val sumByRoute: Map[String, Double] = allRouteLengths map { case (k, v) =>
      k.id.toString -> v
    }

    debug("Writing mode value for trans-to-road ratio")
    val sumByMode: Map[Int, Double] = allRouteLengths.groupBy { case (k, v) =>
      k.route_type.id }.map { case (k, v) => k -> v.values.sum }

    debug("Writing system value for trans-to-road ratio")
    val sumBySystem: Double = sumByMode.values.sum


    //Return tuple for our calcBys to call
    (sumByRoute.map({case (k,v) => k -> v/roadLength}),
      sumByMode.map({case (k,v) => k -> v/roadLength}),
      sumBySystem/roadLength)
  }

  def calcByRoute(period: SamplePeriod): Map[String, Double] = {
    debug("Get calculation by route")
    memoizedDerivation(period)._1
  }

  def calcByMode(period: SamplePeriod): Map[Int, Double] = {
    debug("Get calculation by mode")
    memoizedDerivation(period)._2
  }

  def calcBySystem(period: SamplePeriod): Double = {
    debug("Get calculation by system")
    memoizedDerivation(period)._3
  }

}
