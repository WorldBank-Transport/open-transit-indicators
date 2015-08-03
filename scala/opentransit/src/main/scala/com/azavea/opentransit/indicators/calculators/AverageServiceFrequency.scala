package com.azavea.opentransit.indicators.calculators

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._

import com.github.nscala_time.time.Imports._
import org.joda.time._

/** Average service frequency.
  * This indicator calculates the frequency of a vehicle arriving
  * at a stop in a route.
  */
object AverageServiceFrequency extends Indicator
                                  with AggregatesByAll {
  type Intermediate = (Double, Int)

  val name = "avg_service_freq"

  def calculation(period: SamplePeriod) = {
    def map(route: Seq[Trip]): Intermediate =
      route                                     // Seq[Trip]
        .groupBy(_.direction).values            // Seq[List[Trip]]
        // Now generate a Seq[Intermediate] for each List[Trip] in the sequence, which
        // corresponds to each direction. This prevents trips in opposite directions from
        // being included together in the average service frequency because they will be
        // considered separately until the very end.
        .map(
          _.flatMap(_.schedule).toList          // List[ScheduledStops]
            .groupBy(_.stop)                    // Map[Stop, List[ScheduledStops]]
            .values.toList                      // List[List[ScheduledStop]]
            .map(
              _.map(_.arrivalTime).sorted)      // List[List[LocalDateTime]]
            .map {
              _.sliding(2).toList.filter(_.size > 1).map { dt: List[LocalDateTime] =>
                Seconds.secondsBetween(dt(0), dt(1)).getSeconds.abs
              }.foldLeft((0.0, 0)) { case ((total, count), diff) =>
                (total + diff, count + 1)       // Count up the interservice spaces and add up times
              }
            }
            .foldLeft((0.0, 0)) { case ((total, count), (tdiff, cdiff)) =>
              (total + tdiff, count + cdiff)    // Sum up all counts and totals
            }                                   // (Double, Int)
        )                                       // List[(Double, Int)]
        .foldLeft((0.0, 0)) {
          case ((totalSum, countSum), (directionTotal, directionCount)) =>
            (totalSum + directionTotal, countSum + directionCount)
        }


    /** This takes the headway between all scheduled stops (per stop and per route)
     * and calculates average. This means that
     * if we are calculating for the whole system, if a bus were to stop
     * at Stop A and then 5 minutes later the same bus line were to stop at stop A, it
     * would calculate a headway of 5 minutes.
     */
    def reduce(stopSchedules: Seq[Intermediate]): Double = {
      // Average all the headways between each stop.
      val (total, count) =
        stopSchedules
          .foldLeft((0.0, 0)) { case ((total, count), (tdiff, cdiff)) =>
            (total + tdiff, count + cdiff)          // Sum again
          }

      if (count > 0) (total / count) / 60 else 0.0  // div60 for minutes
    }

    perRouteCalculation(map, reduce)
  }
}
