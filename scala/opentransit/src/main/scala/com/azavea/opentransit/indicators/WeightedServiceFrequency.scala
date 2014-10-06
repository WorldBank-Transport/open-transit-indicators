package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit.indicators.parameters._

import com.github.nscala_time.time.Imports._
import org.joda.time._

/** Average service frequency.
  * This indicator calculates the frequency of a vehicle arriving
  * at a stop in a route.
  */
class WeightedServiceFrequency(params: Boundaries with StopBuffers with Demographics)
    extends Indicator
    with AggregatesByAll {
  type Intermediate = Map[Stop, Seq[LocalDateTime]]

  val name = "weighted_service_freq"

  def calculation(period: SamplePeriod) = {
    val periodDuration = Seconds.secondsBetween(period.start, period.end).getSeconds

    def map(trips: Seq[Trip]): Map[Stop, Seq[LocalDateTime]] = {
      trips
        .map(_.schedule)
        .flatten
        .groupBy(_.stop)
        .map { case (stop, schedules) =>
          (stop, schedules.map(_.arrivalTime))
        }.toMap
    }

    /** This takes the headway between all schedules stops per stop and
      * calculates the headway between those scheduled stops. This means that
      * if we are calculating for the whole system, if a bus were to stop
      * at Stop A and then 5 minutes later a train stopped at stop A, then it
      * would calculate a headway of 5 minutes. Then all headways for all stops
      * are averaged.
      */
    def reduce(stopSchedules: Seq[Map[Stop, Seq[LocalDateTime]]]): Double = {
      // Average all the headways between each stop with weight.
      val freqsForPop =
        stopSchedules
          .combineMaps
          .map { case (stop, schedules) =>
            val stopBuffer = params.bufferForStop(stop)
            val popInBuffer = params.populationMetricForBuffer(
                stopBuffer,
                "populationMetric1"
            )
            val timesVisited = schedules.length
            val avgSvcFreq = (1 / (timesVisited / periodDuration)) * 60 * 60
            (avgSvcFreq * popInBuffer)
          }
      val (total, count) =
        freqsForPop.foldLeft((0.0, 0)) { case ((total, count), diff) =>
          (total + diff, count + 1)
        }
      val avgFreqForPop = if (count > 0) total / count else 0.0

      avgFreqForPop / totalPop
    }

    perRouteCalculation(map, reduce)
  }
}
