package com.azavea.opentransit.indicators

import com.azavea.opentransit.indicators.calculators._

import org.scalatest._

// Remember: this thing is hella broken right now.
// TODO: Fix it and its indicator once JTS bugs are resolved
/*
class RoadLengthRatioSpec extends FlatSpec with Matchers with IndicatorParamSpec {
  it should "calculate time_traveled_stops by route for SEPTA" in {
    val calculation =
      new RoadLengthRatio(params: RoadLength).calculation(firstPeriod)
    val AggregatedResults(byRoute, byRouteType, bySystem) =
      calculation(firstSystem)
    implicit val routeMap = byRoute

    bySystem.get should be (2636.3575 +- 1e4) // THIS TEST IS BROKEN
  }
}
*/
