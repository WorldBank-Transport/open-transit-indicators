package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

object Length extends Indicator
                 with AggregatesByAll {
  type Intermediate = Double

  val name = "length"

  val calculation = 
    new PerRouteIndicatorCalculation[Double] {
      def map(trips: Seq[Trip]): Double =
        trips.foldLeft(0.0) { (maxLength, trip) =>
          trip.tripShape match {
            case Some(shape) =>
              val tripLength = shape.line.length / 1000
              math.max(maxLength, tripLength)
            case None =>
              maxLength
          }
        }

      def reduce(routeLengths: Seq[Double]): Double =
        routeLengths.foldLeft(0.0)(_ + _)
    }
}
