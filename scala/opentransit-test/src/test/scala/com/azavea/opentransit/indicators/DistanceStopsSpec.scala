package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import org.scalatest._


class AdHocDistanceStopsSpec extends AdHocSystemIndicatorSpec {
  it should "Calculate the distance between subway stops for the AdHoc system" in {
    val calculation = DistanceStops.calculation(allStopsPeriod)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(systemWithAllStops)
    implicit val routeMap = byRoute

    byRouteType(Subway) should be (0.01) // Kilometers
  }
}


class DistanceStopsSpec extends FlatSpec with Matchers with IndicatorSpec {
  it should "calcuate overall distance_between_stops by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(DistanceStops)

    byRouteType(Rail) should be (2.31436 +- 1e-5)
  }

  it should "calcuate distance_between_stops by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(DistanceStops)

    getResultByRouteId(byRoute, "AIR") should be (1.90437 +- 1e-5)
    getResultByRouteId(byRoute, "CHE") should be (1.00636 +- 1e-5)
    getResultByRouteId(byRoute, "CHW") should be (1.32695 +- 1e-5)
    getResultByRouteId(byRoute, "CYN") should be (2.41703 +- 1e-5)
    getResultByRouteId(byRoute, "FOX") should be (1.55288 +- 1e-5)
    findRouteById(byRoute.keys, "GLN") should be (None)
    getResultByRouteId(byRoute, "LAN") should be (2.08694 +- 1e-5)
    getResultByRouteId(byRoute, "MED") should be (1.59278 +- 1e-5)
    getResultByRouteId(byRoute, "NOR") should be (1.72959 +- 1e-5)
    getResultByRouteId(byRoute, "PAO") should be (1.80415 +- 1e-5)
    getResultByRouteId(byRoute, "TRE") should be (5.39617 +- 1e-5)
    getResultByRouteId(byRoute, "WAR") should be (1.75354 +- 1e-5)
    getResultByRouteId(byRoute, "WIL") should be (5.30987 +- 1e-5)
    getResultByRouteId(byRoute, "WTR") should be (2.20600 +- 1e-5)
  }

  it should "calcuate overall distance_between_stops by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(DistanceStops)

    bySystem.get should be (2.31436 +- 1e-5)
  }
}
