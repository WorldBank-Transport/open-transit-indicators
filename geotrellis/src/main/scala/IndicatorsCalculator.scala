package opentransitgt

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.azavea.gtfs.slick._
import opentransitgt.DjangoAdapter._

class IndicatorsCalculator(val gtfsData: GtfsData, val period: SamplePeriod) {
  // Counts the number of routes per mode
  lazy val numRoutesPerMode: Map[Int, Int] = {
    // get all routes, group by route type, and count the size of each group
    gtfsData.routes
      .groupBy(_.route_type.id)
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
  lazy val numStopsPerMode: Map[Int, Int] = {
    // get all routes, group by route type, and find the unique stop ids per route (via trips)
    gtfsData.routes
      .groupBy(_.route_type.id)
      .mapValues(_.map(_.id)
        .map(gtfsData.tripsByRoute(_)
          .map(_.stopTimes.map(_.stop_id))
          .flatten
      ).flatten.distinct.length)
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
            gtfsData.shapesById.get(shapeID) match {
              case None => 0
              case Some(tripShape) => {
                tripShape.line.length
              }
            }
          }
        }
      ).max
    )
  }

  // Finds the average transit system length per mode
  lazy val avgTransitLengthPerMode: Map[Int, Double] = {
    // get the transit length per route, group by route type, and average all the lengths
    maxTransitLengthPerRoute.toList
      .groupBy(kv => routeByID(kv._1).route_type.id)
      .mapValues(v => v.map(_._2).sum / v.size)
  }

  // Helper map of route id -> route
  lazy val routeByID: Map[String, Route] = {
    gtfsData.routes.map(route => route.id.toString -> route).toMap
  }
}
