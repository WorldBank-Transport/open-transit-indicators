package com.azavea.opentransit.indicators

import geotrellis.vector._

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._
import org.scalatest.OptionValues._

class TransitNetworkDensitySpec
    extends FlatSpec
    with Matchers
    with IndicatorSpec
    with StopBuffersSpec
    with BoundariesSpec {

  val boundaries = new BoundariesSpecParams {}

  it should "calculate transit network density by system for SEPTA" in {
    val calculation = new TransitNetworkDensity(boundaries).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.isDefined should be (true)
    bySystem.value should be (0.0017 +- 1e-4)
  }
}
