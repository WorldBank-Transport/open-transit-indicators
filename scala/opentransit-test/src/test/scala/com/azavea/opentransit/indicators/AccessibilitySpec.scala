package com.azavea.opentransit.indicators

import geotrellis.vector._

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._
import org.scalatest.OptionValues._

class AccessibilitySpec
    extends FlatSpec
    with Matchers
    with IndicatorSpec
    with DemographicsSpec
    with StopBuffersSpec
    with BoundariesSpec {

  val demographicsAndBuffersAndBoundaries = new StopBuffersSpecParams
      with DemographicsSpecParams
      with BoundariesSpecParams {}

  it should "calculate overall system accessibility by system for SEPTA" in {
    val calculation = new AllAccessibility(demographicsAndBuffersAndBoundaries).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.isDefined should be (true)
    bySystem.value should be (10.4519370 +- 1e-5)
  }

  it should "calculate system accessibility by system for SEPTA (populationMetric2)" in {
    val calculation = new LowIncomeAccessibility(demographicsAndBuffersAndBoundaries).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.isDefined should be (true)
    bySystem.value should be (11.7641318 +- 1e-5)
  }

}
