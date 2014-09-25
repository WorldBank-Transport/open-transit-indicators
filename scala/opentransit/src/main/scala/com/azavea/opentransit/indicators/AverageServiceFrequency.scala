package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

import com.github.nscala_time.time.Imports._
import org.joda.time._

/** Average service frequency.
  * This indicator calculates the frequency of a vehicle arriving 
  * at a stop in a route.
  */
object AverageServiceFrequency extends Indicator 
                                  with AggregatesByAll {
  type Intermediate = Map[Stop, Seq[LocalDateTime]]

  val name = "avg_service_freq"

  val calculation = 
    new PerRouteIndicatorCalculation[Map[Stop, Seq[LocalDateTime]]] {
      def map(trips: Seq[Trip]): Map[Stop, Seq[LocalDateTime]] =
        trips
          .map(_.schedule)
          .flatten
          .groupBy(_.stop)
          .mapValues { schedules =>
            schedules.map(_.arrivalTime)
           }

      /** This takes the headway between all schedules stops per stop and
        * calculates the headway between those scheduled stops. This means that
        * if we are calculating for the whole system, if a bus were to stop
        * at Stop A and then 5 minutes later a train stopped at stop A, then it
        * would calculate a headway of 5 minutes. Then all headways for all stops
        * are averaged.
        */
      def reduce(stopSchedules: Seq[Map[Stop, Seq[LocalDateTime]]]) = {
        // Average all the headways between each stop.
        val (total, count) =
          stopSchedules
            .combineMaps
            .map { case (stop, schedules) =>
              val orderedArrivalTimes =
                schedules.sorted

              // Calculate the headways for each stop.
              orderedArrivalTimes
                .zip(orderedArrivalTimes.tail)
                .map { case (a1, a2) =>
                  Seconds.secondsBetween(a1, a2).getSeconds * 60 * 60
                 }
              }
             .flatten
             .foldLeft((0.0,0)) { case ((total, count), diff) =>
                (total + diff, count + 1)
              }

        total / count
      }
    }
}
