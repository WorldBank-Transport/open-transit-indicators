package com.azavea.opentransit.indicators.calculators

import org.joda.time.Seconds

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.parameters._

/**
* This indicator calculates the average distance between
* arrival times predicted and actually observed
**/
class TravelTimePerformance(params: ObservedStopTimes)
    extends Indicator
      with AggregatesByAll {
  type Intermediate = Seq[Double]

  val name = "on_time_perf"

  def calculation(period: SamplePeriod) = {
      def map(trip: Trip): Seq[Double] = {
        val obsTrip = params.observedForTrip(period, trip.id)
        val schedStopIds = trip.schedule map (_.stop.id)
        for {
          id <- schedStopIds
          schedStop <- trip.schedule if schedStop.stop.id == id
          schedStopTime = schedStop.arrivalTime
          obsStop <- obsTrip.schedule if obsStop.stop.id == id
          obsStopTime = obsStop.arrivalTime
        } yield Seconds.secondsBetween(schedStopTime, obsStopTime)
          .getSeconds
          .toDouble
      }

      def reduce(timeDeltas: Seq[Seq[Double]]): Double = {
        val (total, count) =
          timeDeltas.flatten.foldLeft((0.0, 0)) { case ((total, count), diff) =>
            (total + diff, count + 1)
          }
        if (count > 0) (total / 60 / 60) / count else 0.0
      }
      perTripCalculation(map, reduce)
    }
  }
