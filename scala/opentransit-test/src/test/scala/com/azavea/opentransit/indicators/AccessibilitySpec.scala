package com.azavea.opentransit.indicators

import geotrellis.vector._

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._
import org.scalatest.OptionValues._

class AccessibilitySpec
  extends FlatSpec with Matchers with IndicatorSpec with DemographicsSpec with StopBuffersSpec {

  val demographicsAndBuffers = new StopBuffersSpecParams with DemographicsSpecParams {}

  it should "calculate overall system accessibility by system for SEPTA" in {
    val calculation = new AllAccessibility(demographicsAndBuffers).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.isDefined should be (true)
    bySystem.value should be (123554.2431922 +- 1e-5)
  }

  it should "calculate system accessibility by system for SEPTA (populationMetric2)" in {
    val calculation = new LowIncomeAccessibility(demographicsAndBuffers).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.isDefined should be (true)
    bySystem.value should be (58442.77714347 +- 1e-5)
  }

}
