package opentransitgt

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.azavea.gtfs.slick._

class IndicatorsCalculator(val gtfsData: GtfsData) {
  // Counts the number of routes per mode
  lazy val numRoutesPerMode: Map[String, Int] = {
    // get all routes, group by route type, and count the size of each group
    gtfsData.routes
      .groupBy(_.route_type.toString)
      .mapValues(_.size)
  }

  // Counts the maximum number of stops per route
  lazy val maxStopsPerRoute: Map[String, Int] = {
    // for each route, find the maximum number of stops across all trips
    gtfsData.routes.map(route =>
      route.id.toString -> gtfsData.tripsByRoute(route.id)
        .foldLeft(0) {(max, trip) =>
          if (trip.stopTimes.isEmpty || trip.stopTimes.length < max) {
            max
          } else {
            trip.stopTimes.length
          }
        }).toMap
  }

  // Counts the number of stops per mode
  lazy val numStopsPerMode: Map[String, Int] = {
    // get the number of stops per route, group by route type, and count the stops in each
    maxStopsPerRoute.toList
      .groupBy(kv => routeByID(kv._1).route_type.toString)
      .mapValues(_.foldLeft(0) {(sum, pair) => sum + pair._2 })
  }

  // Finds the max transit system length per route
  // TODO: for now, this assumes shapes exist. Calculate by other means if that's not the case.
  lazy val maxTransitLengthPerRoute: Map[String, Double] = {
    gtfsData.trips
      .groupBy(_.route_id.toString)
      .mapValues(_.map(t =>
        t.shape_id match {
          case None => 0
          case Some(shapeID) => {
            gtfsData.shapesById(shapeID).line.length
          }
        }
      ).max
    )
  }

  // Finds the average transit system length per mode
  lazy val avgTransitLengthPerMode: Map[String, Double] = {
    // get the transit length per route, group by route type, and average all the lengths
    maxTransitLengthPerRoute.toList
      .groupBy(kv => routeByID(kv._1).route_type.toString)
      .mapValues(v => v.map(_._2).sum / v.size)
  }

  // Helper map of route id -> route
  lazy val routeByID: Map[String, Route] = {
    gtfsData.routes.map(route => route.id.toString -> route).toMap
  }
}
