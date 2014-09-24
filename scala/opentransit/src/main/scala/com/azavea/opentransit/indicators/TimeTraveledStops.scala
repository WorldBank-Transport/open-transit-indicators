package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.DjangoAdapter._
import scala.slick.jdbc.JdbcBackend.DatabaseDef

import com.github.nscala_time.time.Imports._
import org.joda.time._

object TimeTraveledStops extends Indicator
                            with AggregatesByAll {
  val name = "time_traveled_stops"

  val calc =
    new PerTripIndicatorCalculation {
      type Intermediate = Seq[Int]

      def map(trip: Trip) =
          trip.schedule
            .zip(trip.schedule.tail)
            .map { case (stop1, stop2) =>
              Minutes.minutesBetween(stop1.arrivalTime, stop2.arrivalTime).getMinutes
             }
   
      def reduce(durations: Seq[Seq[Int]]) = {
        val (sum, count) =
          durations
            .flatten
            .foldLeft((0,0)) { case ((sum, count), minutes) =>
              (sum + minutes, count + 1)
             }
        sum.toDouble / count
      }
    }
}

// Time traveled between stops
// class TimeTraveledStops(val gtfsData: GtfsData, val calcParams: CalcParams, val db: DatabaseDef)
//     extends IndicatorCalculator {

//   val name = "time_traveled_stops"

//   def calcByRoute(period: SamplePeriod): Map[String, Double] = {
//     println("in calcByRoute in TimeTraveledStops")
//     durationsBetweenStopsPerRoute(period).map { case(routeID, durations) =>
//       (routeID, durations.sum / durations.size)
//     }
//   }

//   def calcByMode(period: SamplePeriod): Map[Int, Double] = {
//     println("in calcByMode for TimeTraveledStops")
//     durationsBetweenStopsPerRoute(period).toList
//       .groupBy(kv => routeByID(kv._1).route_type.id)
//       .map { case (key, routesToDurations) => key -> {
//         val durations = routesToDurations.map(_._2).flatten
//         durations.sum / durations.size.toDouble
//       }
//     }
//   }

//   def calcBySystem(period: SamplePeriod): Double = {
//     println("in calcBySystem for TimeTraveledStops")
//     val durations = durationsBetweenStopsPerRoute(period)
//       .toList
//       .flatMap(_._2)

//     durations.sum / durations.length
//   }

//   // Gets a list of durations between stops per route
//   def durationsBetweenStopsPerRoute(period: SamplePeriod): Map[String, Seq[Double]] = {
//     println("in durationsBetweenStopsPerRoute for TimeTraveledStops")
//     routesInPeriod(period).map(route =>
//       route.id.toString -> {
//         tripsInPeriod(period, route).map(trip => {
//           calcStopDifferences(trip.stops).map(_ * 60.0)
//         }).flatten
//       }
//     ).toMap
//   }
// }
