package com.azavea.opentransit.indicators.calculators

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.parameters._

import geotrellis.vector._

import com.github.nscala_time.time.Imports._
import org.joda.time._

// Areal Coverage Ratio of Transit Stops (user-configurable buffer)
class RoadLengthRatio(roadLength: RoadLength)
    extends Indicator
       with AggregatesBySystem {
  type Intermediate = Double

  val name = "lines_roads"

  val totalRoadLength = roadLength.totalRoadLength

  def calculation(period: SamplePeriod) =
    new PerRouteIndicatorCalculation[Double] {
      def apply(system: TransitSystem) =
        apply(system, aggregatesBy)

      def map(trips: Seq[Trip]): Double =
        trips
          .map { trip =>
            trip.tripShape.map { ts => ts.line.geom.length }
           }
          .flatten
          .foldLeft(0.0)(math.max(_, _))

      def reduce(stops: Seq[Double]): Double =
        stops.foldLeft(0.0)(_ + _) / totalRoadLength
    }
}
