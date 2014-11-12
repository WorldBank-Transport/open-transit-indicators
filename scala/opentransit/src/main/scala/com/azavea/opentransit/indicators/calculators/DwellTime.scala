package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators.parameters._

import com.github.nscala_time.time.Imports._
import org.joda.time._

/** This indicator calculates the average deviation from scheduled dwell time
 *  in minutes.
 */
class DwellTimePerformance(params: ObservedStopTimes) extends Indicator with AggregatesByAll {
  type Intermediate = Seq[Double]

  val name = "dwell_time"

  def calculation(period: SamplePeriod) = {
    def map(trip: Trip): Seq[Double] = {
      params.observedStopsByTrip(trip.id).map { case (sched, obsvd) =>
          dwellTimeDeviation(sched, obsvd)
      }
    }

    def reduce(deviations: Seq[Seq[Double]]): Double = {
      val (sum, count) =
        deviations
          .flatten
          .foldLeft((0.0, 0)) { case ((sum, count), minutes) =>
            (sum + minutes, count + 1)
           }
      if (count > 0) (sum / count) / 60 else 0.0 // div60 for minutes
    }

    perTripCalculation(map, reduce)
  }

  def dwellTimeDeviation(s1: ScheduledStop, s2: ScheduledStop): Double = {
    (Seconds.secondsBetween(s1.arrivalTime, s1.departureTime).getSeconds -
     Seconds.secondsBetween(s2.arrivalTime, s2.departureTime).getSeconds).abs.toDouble
  }
}
