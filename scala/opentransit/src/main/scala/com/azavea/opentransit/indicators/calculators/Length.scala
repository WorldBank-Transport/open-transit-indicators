package com.azavea.opentransit.indicators.calculators

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._

object Length extends Indicator
                 with AggregatesByAll {
  type Intermediate = Double

  val name = "length"

  // TODO: Rob pointed out during the refactor walkthrough that this calculation
  // is not correct and needs tweaking (particularly eliminating the max logic).
  def calculation(period: SamplePeriod) = {
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

    perRouteCalculation(map, reduce)
  }
}
