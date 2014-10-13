package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit.indicators.parameters._
import geotrellis.vector._

import com.github.nscala_time.time.Imports._
import org.joda.time._

/** Average service frequency.
  * This indicator calculates the frequency of a vehicle arriving
  * at a stop in a route.
  */
class WeightedServiceFrequency(params: StopBuffers with Demographics)
    extends Indicator
    with AggregatesByAll {
  type Intermediate = Map[Stop, Seq[LocalDateTime]]

  val name = "weighted_service_freq"

  def calculation(period: SamplePeriod) = {
    /** For each trip, return a Map of stops to trip arrival times at said stops */
    def map(trips: Seq[Trip]): Map[Stop, Seq[LocalDateTime]] = {
      trips
        .map(_.schedule)
        .flatten
        .groupBy(_.stop)
        .map { case (stop, schedules) =>
          (stop, schedules.map(_.arrivalTime))
        }.toMap
    }

    /** This is average service frequency weighted by the number of people served
     *  at each stop. So, the more people at a stop, the more that frequency
     *  contributes to the weighted average service frequency.
     */
    def reduce(stopSchedules: Seq[Map[Stop, Seq[LocalDateTime]]]): Double = {
      // Average all the headways between each stop with weight.
      // Total population over which our weighting will be averaged
      val allStops = params.bufferForStops {
        stopSchedules.map { mapping => mapping.keys }
          .flatten
          .distinct
      }
      val allStopsPop = params.populationMetricForBuffer(
        allStops,
        "populationMetric1"
      )

      val freqsForPop =
        stopSchedules
          .combineMaps
          .map { case (stop, schedules) =>
            val thisStopBuffer = params.bufferForStop(stop)
            val popInBuffer = params.populationMetricForBuffer(
                thisStopBuffer,
                "populationMetric1"
            )

            val orderedArrivalTimes = schedules.sorted
            // Zip using indexes: (i1, i2), (i2, i3), etc.
            val (total, count) =
              orderedArrivalTimes.zip(orderedArrivalTimes.tail)
                .map { case (i1, i2) => Seconds.secondsBetween(i1, i2).getSeconds }
                .foldLeft((0.0, 0)) { case ((total, count), diff) =>
                  ((total + diff), count+1)
                }
            // Calculate the average for each stop in this aggregation
            if (count > 0) (total / count) * popInBuffer else 0.0
            }
            .sum

      // Return the weighted average
      freqsForPop / allStopsPop / 60 / 60
    }

    perRouteCalculation(map, reduce)
  }
}
