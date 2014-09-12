package opentransitgt.indicators

import com.azavea.gtfs.data._
import opentransitgt._
import opentransitgt.DjangoAdapter._
import scala.slick.jdbc.JdbcBackend.DatabaseDef

// Time traveled between stops
class TimeTraveledStops(val gtfsData: GtfsData, val calcParams: CalcParams, val db: DatabaseDef)
    extends IndicatorCalculator {

  val name = "time_traveled_stops"

  def calcByRoute(period: SamplePeriod): Map[String, Double] = {
    durationsBetweenStopsPerRoute(period).map { case(routeID, durations) =>
      (routeID, durations.sum / durations.size)
    }
  }

  def calcByMode(period: SamplePeriod): Map[Int, Double] = {
    durationsBetweenStopsPerRoute(period).toList
      .groupBy(kv => routeByID(kv._1).route_type.id)
      .map { case (key, routesToDurations) => key -> {
        val durations = routesToDurations.map(_._2).flatten
        durations.sum / durations.size.toDouble
      }
    }
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
}
