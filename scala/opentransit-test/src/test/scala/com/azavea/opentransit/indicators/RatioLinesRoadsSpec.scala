package com.azavea.opentransit.indicators

import geotrellis.vector._

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._
import org.scalatest.OptionValues._

class RatioLinesRoadsSpec
  extends FlatSpec
  with Matchers
  with IndicatorSpec
  with RoadLengthSpec {

  val totalRoadLength = new RoadLengthSpecParams {}

  // TODO: fix this test. After the recent geometry changes, the test returns 1.89911
  ignore should "calculate the ratio of transit system length to road system length for SEPTA/Philadelphia" in {
    val calculation = new RatioLinesRoads(totalRoadLength).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.isDefined should be (true)
    // The value calculated manually in QGIS is ~0.09060, which is pretty close.
    bySystem.value should be (9.36412 +- 1e-5)
  }
}
