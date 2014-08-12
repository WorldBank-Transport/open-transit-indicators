package opentransitgt

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.azavea.gtfs.slick._
import opentransitgt.DjangoAdapter._

class IndicatorsCalculator(val gtfsData: GtfsData, val period: SamplePeriod) {
  // The GTFS parser uses local date times.
  // Need to verify that the GTFS date time input will always be in
  // local time. Otherwise we may need a way for the user to specify it.
  val startDT = period.period_start.toLocalDateTime()
  val endDT = period.period_end.toLocalDateTime()

  // Counts the number of routes per mode
  lazy val numRoutesPerMode: Map[Int, Int] = {
    // get all routes, group by route type, and count the size of each group
    routesInPeriod
      .groupBy(_.route_type.id)
      .mapValues(_.size)
  }

  // Counts the maximum number of stops per route
  lazy val maxStopsPerRoute: Map[String, Int] = {
    // for each route, find the maximum number of stops across all trips
    routesInPeriod.map(route =>
      route.id.toString -> tripsInPeriod(route)
        .foldLeft(0) {(max, trip) =>
          val stops = stopsInPeriod(trip)
          if (stops.isEmpty || stops.length < max) {
            max
          } else {
            stops.length
          }
        }).toMap
  }

  // Counts the number of stops per mode
  lazy val numStopsPerMode: Map[Int, Int] = {
    // get all routes, group by route type, and find the unique stop ids per route (via trips)
    routesInPeriod
      .groupBy(_.route_type.id)
      .mapValues(_.map(_.id)
        .map(routeID => tripsInPeriod(routeByID(routeID))
          .map(stopsInPeriod(_).map(_.stop_id))
          .flatten
      ).flatten.distinct.length)
  }

  // Finds the max transit system length per route
  // TODO: for now, this assumes shapes exist. Calculate by other means if that's not the case.
  lazy val maxTransitLengthPerRoute: Map[String, Double] = {
    routesInPeriod.map(route =>
      route.id.toString -> tripsInPeriod(route)
        .foldLeft(0.0) {(max, trip) => trip.rec.shape_id match {
          case None => max
          case Some(shapeID) => {
            gtfsData.shapesById.get(shapeID) match {
              case None => max
              case Some(tripShape) => {
                math.max(max, tripShape.line.length)
              }
            }
          }
        }
      }
    ).toMap
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

  // Filtered list of routes that fall within the period
  lazy val routesInPeriod = {
    gtfsData.routes.filter { tripsInPeriod(_).size > 0 }
  }

  // Returns all stops occuring during the period for the specified scheduled trip
  def stopsInPeriod(trip: ScheduledTrip) = {
    trip.stops.filter(stop =>
      stop.arrival.isAfter(startDT) && stop.arrival.isBefore(endDT)
    )
  }

  // Returns all scheduled trips for this route during the period
  def tripsInPeriod(route: Route) = {
    // Importing the context within this scope adds additional functionality to Routes
    import gtfsData.context._

    route.getScheduledTripsBetween(startDT, endDT)
  }
}
