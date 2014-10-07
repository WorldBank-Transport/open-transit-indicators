package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

// This indicator calculates the average number of stops
// for a unit of transit line length
object StopsToLength extends Indicator
                   with AggregatesByAll {
  type Intermediate = Double

  val name = "stops_route_length"

  def calculation(period: SamplePeriod): IndicatorCalculation = {
    def map(trip: Trip): Double = {
      val stops: Seq[Stop] = trip.schedule.map(_.stop)
      trip.tripShape match {
        case Some(shape) =>
          stops.size / (shape.line.length / 1000)
        case None =>
          0.0
      }
    }

    def reduce(stopRates: Seq[Double]): Double = {
      val (total, count) =
        stopRates
      .foldLeft((0.0, 0)) { case ((headVal, count), tailVal) =>
        (headVal + tailVal, count + 1)
                         }
      if (count > 0) total / count else 0.0
    }
    perTripCalculation(map, reduce)
  }
}
