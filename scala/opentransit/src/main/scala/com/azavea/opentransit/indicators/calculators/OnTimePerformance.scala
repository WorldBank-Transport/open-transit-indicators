package com.azavea.opentransit.indicators.calculators

import org.joda.time.Seconds

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.parameters._

/**
* This indicator calculates the average deviation between
* arrival times predicted and actually observed (in minutes)
**/
class OnTimePerformance(params: ObservedStopTimes)
    extends Indicator
      with AggregatesByAll {
  type Intermediate = Seq[Double]

  val name = "on_time_perf"

  def calculation(period: SamplePeriod) = {
    val zippedStopsByTrip: Map[String, Seq[(ScheduledStop, ScheduledStop)]] =
      params.observedStopsByTrip(period)

    def map(trip: Trip): Seq[Double] =
      for {
        (sched, obs) <- zippedStopsByTrip(trip.id)
        deviation = Seconds.secondsBetween(sched.arrivalTime, obs.arrivalTime).getSeconds
      } yield deviation.abs.toDouble

    def reduce(timeDeltas: Seq[Seq[Double]]): Double = {
      val (total, count) =
        timeDeltas.flatten.foldLeft((0.0, 0)) { case ((total, count), diff) =>
          (total + diff, count + 1)
        }
      if (count > 0) (total /count) / 60 else 0.0 // div60 for minutes
    }
    perTripCalculation(map, reduce)
  }
}

