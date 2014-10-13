package com.azavea.opentransit.indicators.calculators

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._

object Length extends Indicator
                 with AggregatesByAll {
  type Intermediate = Double

  val name = "length"

  // NOTE: This calculation is not correct in all circumstances; that is, the trip
  // with the maximum number of stops may not contain every stop on the route, and
  // might therefore be shorter than the actual length of the route. However, this
  // has been determined to be close enough for now.
  // TODO: Fix this if there is time or an edge case in which it makes a significant
  // difference.
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
