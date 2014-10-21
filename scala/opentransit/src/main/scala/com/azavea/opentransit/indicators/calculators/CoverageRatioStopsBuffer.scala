package com.azavea.opentransit.indicators.calculators

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.parameters._

import geotrellis.vector._

import com.github.nscala_time.time.Imports._
import org.joda.time._

// Areal Coverage Ratio of Transit Stops (user-configurable buffer)
class CoverageRatioStopsBuffer(params: Boundaries with StopBuffers)
    extends Indicator with AggregatesBySystem {
  type Intermediate = Seq[Stop]

  val name = "coverage_ratio_stops_buffer"

  def calculation(period: SamplePeriod) = {
    def calculate(transitSytem: TransitSystem) = {
      val cityBoundary = params.cityBoundary
      val coverage = params.bufferForPeriod(period)
      val systemResult =
        ((cityBoundary & coverage) match {
                case MultiPolygonResult(mp) => mp.area
                case PolygonResult(p) => p.area
                case _ => 0.0
        }) / cityBoundary.area
      AggregatedResults.systemOnly(systemResult * 100)
    }

    perSystemCalculation(calculate)
  }
}
