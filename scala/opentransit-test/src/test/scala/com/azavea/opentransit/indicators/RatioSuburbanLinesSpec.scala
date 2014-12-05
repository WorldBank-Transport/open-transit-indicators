package com.azavea.opentransit.indicators


import com.azavea.gtfs._

import com.azavea.opentransit.indicators.calculators._

import com.azavea.opentransit.testkit._

import org.scalatest._


class RatioSuburbanLinesSpec
    extends FlatSpec
    with Matchers
    with IndicatorSpec
    with BoundariesSpec {

  val params = new BoundariesSpecParams {}

  it should "Calculate the ratio" in {
    val calculator = new RatioSuburbanLines(params)
    val calculation = calculator.calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)

    bySystem.get should be (0.84615 +- 1e-5)  // 11 suburban routes / 13 routes total
  }
}
