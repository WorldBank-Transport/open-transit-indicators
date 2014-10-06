package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

import geotrellis.vector._

import com.github.nscala_time.time.Imports._
import org.joda.time._

// Areal Coverage Ratio of Transit Stops (user-configurable buffer)
class CoverageRatioStopsBuffer(params: Boundaries with StopBuffers)
    extends Indicator
       with AggregatesBySystem {
  type Intermediate = Seq[Stop]

  val name = "coverage_ratio_stops_buffer"

  def calculation(period: SamplePeriod) =
    new IndicatorCalculation {
      def apply(transitSytem: TransitSystem) = {
        val cityBoudary = params.cityBoundary.area
        val systemResult =
          params.bufferForPeriod(period).area / params.cityBoundary.area
        AggregatedResult.systemOnly(systemResult)
      }
    }
}
