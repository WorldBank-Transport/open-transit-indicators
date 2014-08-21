package opentransitgt.indicators

import com.azavea.gtfs.data._
import opentransitgt._
import opentransitgt.DjangoAdapter._

// Number of stops
class NumStops(val gtfsData: GtfsData, val calcParams: CalcParams) extends IndicatorCalculator {
  val name = "num_stops"

  def calcByRoute(period: SamplePeriod): Map[String, Double] = {
    // for each route, find the maximum number of stops across all trips
    routesInPeriod(period).map(route =>
      route.id.toString -> tripsInPeriod(period, route)
        .foldLeft(0.0) {(max, trip) =>
        val stops = stopsInPeriod(period, trip)
        if (stops.isEmpty || stops.length < max) {
          max
        } else {
          stops.length
        }
      }).toMap
  }

  def calcByMode(period: SamplePeriod): Map[Int, Double] = {
     // get all routes, group by route type, and find the unique stop ids per route (via trips)
    routesInPeriod(period)
      .groupBy(_.route_type.id)
      .mapValues(_.map(_.id)
        .map(routeID => tripsInPeriod(period, routeByID(routeID))
          .map(stopsInPeriod(period, _).map(_.stop_id))
          .flatten
      ).flatten.distinct.length)
  }
}
