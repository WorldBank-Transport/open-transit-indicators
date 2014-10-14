package com.azavea.opentransit.indicators.calculators

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.parameters._

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
              val tripLength = shape.line.length / 1000 // div by 1000 for kms
              math.max(maxLength, tripLength)
            case None =>
              maxLength
          }
        }

      def reduce(routeLengths: Seq[Double]): Double = {
        val area: Double = params.regionBoundary.area / 1000 // div by 1000 for km
        println(routeLengths)
        println(area)
        if (area > 0) routeLengths.foldLeft(0.0)(_ + _) / area else 0
      }
    }
}
