package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

object InterstopDistance extends Indicator
                   with AggregatesByAll {
  type Intermediate = Double

  val name = "distance_stops"

  val calculation =
    new PerTripIndicatorCalculation[Double] {
      def map(trip: Trip): (Double) = {
        val stops: Seq[Stop] = trip.schedule.map(_.stop)
        trip.tripShape match {
          case Some(shape) =>
            if (stops.size > 0) ((shape.line.length / 1000) / stops.size) else 0.0
          case None =>
            0.0
        }
      }

      def reduce(stopRates: Seq[Double]): Double = {
        val (total, count) =
          stopRates
            .reduce { case ((headVal, count), tailVal) =>
              ((headVal + tailVal), count + 1)
            }
        if (count > 0) total / count else 0.0
      }
    }

}
