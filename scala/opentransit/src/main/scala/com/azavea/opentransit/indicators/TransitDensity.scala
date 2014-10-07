package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

// This indicator is a measure of the distance of track for a
// given unit of area
class TransitNetworkDensity(params:  Boundaries)
    extends Indicator
      with AggregatesByAll {
  type Intermediate = Double

  val name = "line_network_density"

  def calculation(period: SamplePeriod) =
    new PerRouteIndicatorCalculation[Double] {
      def apply(system: TransitSystem) =
        apply(system, aggregatesBy)

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

      def reduce(routeLengths: Seq[Double]): Double = {
        val area: Double = params.regionBoundary.area
        if (area > 0) routeLengths.foldLeft(0.0)(_ + _) / area else 0
      }
    }
}
