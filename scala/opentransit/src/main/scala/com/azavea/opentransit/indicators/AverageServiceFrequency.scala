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
          .map { case (k, schedules) =>
            (k, schedules.map(_.arrivalTime))
          }.toMap

      /** This takes the headway between all schedules stops per stop and
        * calculates the headway between those scheduled stops. This means that
        * if we are calculating for the whole system, if a bus were to stop
        * at Stop A and then 5 minutes later a train stopped at stop A, then it
        * would calculate a headway of 5 minutes. Then all headways for all stops
        * are averaged.
        */
      def reduce(stopSchedules: Seq[Map[Stop, Seq[LocalDateTime]]]): Double = {
        // We are counting the number of time periods between stops,
        // which is one less the amount of visits to a stop
        val (totalSeconds, totalPeriods) =
          stopSchedules
            .combineMaps
            .map { case (stop, schedules) =>
              val orderedArrivalTimes = schedules.sorted
              val seconds:Double = Seconds.secondsBetween(orderedArrivalTimes.head,
                                                   orderedArrivalTimes.last).getSeconds
              val visits = orderedArrivalTimes.size
              // Need at least two visits to determine a freq
              // Also, return the amount of time periods
              // accounted for, not amount of stops
              if (visits > 1) (seconds, visits-1) else (0.0,0)
            }
            .foldLeft((0.0,0)) {
              case ((totalSeconds, totalPeriods),(seconds,visits)) =>
                (totalSeconds+seconds, totalPeriods+visits)
            }
        // amount of time periods / total time = freq
        if (totalSeconds > 0) totalPeriods / (totalSeconds/60/60) else 0.0
      }
    }
}
