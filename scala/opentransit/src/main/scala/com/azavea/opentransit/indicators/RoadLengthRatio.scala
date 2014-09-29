package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

import geotrellis.vector._

import com.github.nscala_time.time.Imports._
import org.joda.time._

// Areal Coverage Ratio of Transit Stops (user-configurable buffer)
class RoadLengthRatio(params: IndicatorCalculationParams) 
    extends Indicator
       with AggregatesBySystem {
  type Intermediate = Double

  val name = "lines_roads"

  val totalRoadLength =  params.totalRoadLength

  val calculation =
    new PerRouteIndicatorCalculation[Double] {
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
