package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import org.scalatest._


class AdHocInterstopDistanceSpec extends AdHocSystemIndicatorSpec {
  it should "Calculate the average distance between stops in the ad-hoc system" in {
    val calculation = InterstopDistance.calculation(allStopsPeriod)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(systemWithAllStops)
    implicit val routeMap = byRoute // Use this implicit to DRY out your tests

    // Tests have +- 1e-2 because of a limitation of the current
    // implementation of InterstopDistance. (Indicator slightly
    // miscalculates trips missing distanceTraveled.)
    routeById("EastRail") should be (0.0)
    routeById("EastBus") should be (0.0)
    routeById("NorthSouth") should be (0.01 +- 1e-2)
    routeById("WestRail") should be (0.0)
    routeById("EastWest") should be (0.01 +- 1e-2)
    routeById("WestBus") should be (0.0)
  }
}

// these values are very close to the slightly less accurate byRoute results which were removed
class InterstopDistanceSpec extends FlatSpec with Matchers with IndicatorSpec {
  it should "calcuate overall distance_between_stops by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(InterstopDistance)

    byRouteType(Rail) should be (3.51069 +- 1e-5)
  }

  it should "calcuate distance_between_stops by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(InterstopDistance)

    getResultByRouteId(byRoute, "AIR") should be (2.46497 +- 1e-5)
    getResultByRouteId(byRoute, "CHE") should be (1.28437 +- 1e-5)
  }

  it should "calcuate overall distance_between_stops by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(InterstopDistance)

    bySystem.get should be (3.51069 +- 1e-5)
  }
}

