package com.azavea.opentransit.indicators

import geotrellis.vector._

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._
import org.scalatest.OptionValues._

class CoverageRatioStopsBufferSpec
  extends FlatSpec with Matchers with IndicatorSpec with StopBuffersSpec with BoundariesSpec {

  val boundariesAndBuffers = new StopBuffersSpecParams with BoundariesSpecParams {}

  it should "calculate coverage_ratio_stops_buffer by system for SEPTA" in {
    val calculation = new CoverageRatioStopsBuffer(boundariesAndBuffers).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.isDefined should be (true)
    // The value calculated manually in QGIS is ~0.09060, which is pretty close.
    bySystem.value should be (0.09364 +- 1e-5)
  }
}
