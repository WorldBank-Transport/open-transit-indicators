package opentransitgt

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.azavea.gtfs.slick._
import com.github.nscala_time.time.Imports._
import opentransitgt.DjangoAdapter._
import org.joda.time.PeriodType

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

  // Finds the average time between stops per mode
  lazy val avgTimeBetweenStopsPerMode: Map[Int, Double] = {
    durationsBetweenStopsPerRoute.toList
      .groupBy(kv => routeByID(kv._1).route_type.id)
      .mapValues(routesToDurations => {
        val durations = routesToDurations.map(_._2).flatten
        durations.sum / durations.size
      }
    )
  }

  // Finds the average time between stops per route
  lazy val avgTimeBetweenStopsPerRoute: Map[String, Double] = {
    durationsBetweenStopsPerRoute.map { case(routeID, durations) =>
      (routeID, durations.sum / durations.size)
    }
  }

  // Helper for getting a list of durations between stops per route
  lazy val durationsBetweenStopsPerRoute: Map[String, Seq[Double]] = {
    routesInPeriod.map(route =>
      route.id.toString -> {
        tripsInPeriod(route).map(trip => {
          calcStopDifferences(trip.stops).map(_ * 60.0)
        }).flatten
      }
    ).toMap
  }

  // Find the average headway for a route in a period
  lazy val headwayByRoute: Map[String, Double] = {
    routesInPeriod.map( route => route.id -> {
      tripsInPeriod( route ).map( trip => trip.stops).flatten
    }).toMap.mapValues(stops => calculateHeadway(stops.toArray))
    .mapValues( stop_differences => stop_differences.sum / stop_differences.size )
  }

  // Find average headway by mode
  lazy val headwayByMode: Map[Int, Double] = {
    routesInPeriod.groupBy(_.route_type.id.toInt).mapValues(routes => {
      routes.map(tripsInPeriod).flatten.map(trip => trip.stops).flatten
    }).mapValues(calculateHeadway).mapValues(diff_list => diff_list.sum / diff_list.size)
  }

  // Helper function to calculate headway for each stop + trip (hours per vehicle)
  def calculateHeadway(stops: Array[StopDateTime]) = {
    stops.groupBy(_.stop_id).mapValues(stops =>
      calcStopDifferences(stops.sortBy(_.arrival).toArray)).values.flatten
  }

  // Helper Function - takes an array of StopDateTimes and returns an array of doubles that represent
  // difference in arrival times
  def calcStopDifferences(stops: Array[StopDateTime]): Array[Double] = {
    stops.zip(stops.tail).map(pair =>
      new Period(pair._1.arrival, pair._2.arrival, PeriodType.seconds()).getSeconds / 60.0 / 60.0 )
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
