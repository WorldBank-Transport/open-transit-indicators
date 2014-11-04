package com.azavea.opentransit.indicators.calculators

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._

import com.github.nscala_time.time.Imports._
import org.joda.time._

object TimeTraveledStops extends Indicator
                            with AggregatesByAll {
  type Intermediate = Seq[Int]

  val name = "time_traveled_stops"

  def calculation(period: SamplePeriod): IndicatorCalculation = {
    def map(trip: Trip): Seq[Int] =
      trip.schedule match {
        case Nil => Seq[Int]()
        case schedule => {
          schedule
          .zip(schedule.tail)
          .map { case (stop1, stop2) =>
            Seconds.secondsBetween(stop1.departureTime, stop2.arrivalTime).getSeconds
              }
        }
      }

    def reduce(durations: Seq[Seq[Int]]): Double = {
      val (sum, count) =
        durations
      .flatten
      .foldLeft((0.0, 0)) { case ((sum, count), minutes) =>
        (sum + minutes, count + 1)
                      }
      if (count > 0) (sum / count) / 60 else 0.0 // div60 for minutes
    }

    perTripCalculation(map, reduce)

  }
}
