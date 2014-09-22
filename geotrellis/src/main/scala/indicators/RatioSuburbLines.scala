package opentransitgt.indicators

import scala.slick.jdbc.JdbcBackend.DatabaseDef
import grizzled.slf4j.Logging

import com.azavea.gtfs.data._
import com.azavea.gtfs.{ScheduledTrip, Route => GtfsRoute}
import geotrellis.vector.{Line, MultiPolygon}

import opentransitgt._
import opentransitgt.data._
import opentransitgt.DjangoAdapter._

// Number of stops
class RatioSuburbLines(val gtfsData: GtfsData, val calcParams: CalcParams, val db: DatabaseDef) extends IndicatorCalculator with BoundaryCalculatorComponent with Logging{
  val name = "ratio_suburban_lines"

  val cityBounds: Boundary = boundary(calcParams.city_boundary_id)

  def getTripShape(trip: ScheduledTrip): Option[Line] = {
    trip.rec.shape_id match {
      case Some(shapeID) => gtfsData.shapesById.get(shapeID) map (_.line)
      case None => None
    }
  }
  def isSuburban(route: GtfsRoute, period: SamplePeriod): Boolean = {
    tripsInPeriod(period, route).exists { x: ScheduledTrip =>
      getTripShape(x) match {
        case Some(tripShape) => !(cityBounds.geom.contains(tripShape))
        case None => false
      }
    }
  }

  def calcByRoute(period: SamplePeriod): Map[String, Double] = {
    debug(s"calculating per route for $this")
    val routesThisPeriod: Array[GtfsRoute] = routesInPeriod(period)

    routesThisPeriod.map {(r: GtfsRoute) =>
      (r.id.toString -> (isSuburban(r, period) match {
        case true => 1.0
        case false => 0.0
      }))
    }.toMap
  }

  def calcByMode(period: SamplePeriod): Map[Int, Double] = {
    debug(s"calculating per mode for $this")
    // find number of routes (for some mode) with stops outside the city; divide that number
    // by the total number of routes for said mode
    val routesThisPeriod: Array[GtfsRoute] = routesInPeriod(period)

    val suburbanRouteCount: Map[Int, Double] = {
      routesThisPeriod.groupBy(_.route_type.id)
        .map { case (k, v) => k -> v.filter((x: GtfsRoute) => isSuburban(x, period)) }
        .map { case (k, v) => k -> v.size.toDouble }
    }

    val urbanRouteCount: Map[Int, Double] = {
      routesThisPeriod.groupBy(_.route_type.id)
        .map { case (k, v) => k -> v.filter((x: GtfsRoute) => !isSuburban(x, period)) }
        .map { case (k, v) => k -> v.size.toDouble }
    }

    val ratios: Map[Int, Double] = routesThisPeriod.groupBy(_.route_type.id).map {
      case (k, v) => k -> (suburbanRouteCount(k) / (urbanRouteCount(k) + suburbanRouteCount(k)))
    }
    ratios
  }

  def calcBySystem(period: SamplePeriod): Double = {
    debug(s"calculating by system for $this")
    val routesThisPeriod: Array[GtfsRoute] = routesInPeriod(period)
    val suburbanRouteCount: Double = (routesThisPeriod filter (x => isSuburban(x, period))).size.toDouble
    val urbanRouteCount: Double = (routesThisPeriod filter (x => !isSuburban(x, period))).size.toDouble
    val ratio: Double = (suburbanRouteCount/(urbanRouteCount + suburbanRouteCount))
    ratio
  }

}
