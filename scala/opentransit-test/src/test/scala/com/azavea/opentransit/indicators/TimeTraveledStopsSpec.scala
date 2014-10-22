package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import org.scalatest._


class AdHocTimeTraveledStopsSpec extends AdHocSystemIndicatorSpec {
  it should "Calculate the time traveled between stops in the ad-hoc system" in {
    val calculation = TimeTraveledStops.calculation(allStopsPeriod)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(systemWithAllStops)
    implicit val routeMap = byRoute // Use this implicit to DRY out your tests

    routeById("EastRail") should be (45.0)
    routeById("EastBus") should be (20.0)
    routeById("NorthSouth") should be (5.0)
    routeById("WestRail") should be (45.0)
    routeById("EastWest") should be (5.0)
    routeById("WestBus") should be (20.0)
  }
}


class TimeTraveledStopsSpec extends FlatSpec with Matchers with IndicatorSpec {
  it should "calculate time_traveled_stops by mode for SEPTA" in {
    val calculation = TimeTraveledStops.calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    byRouteType(Rail) should be (3.78026 +- 1e-1)
  }

  it should "calculate overall time_traveled_stops by mode for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(TimeTraveledStops)
    byRouteType(Rail) should be (3.46785 +- 1e-1)
  }

  it should "calculate time_traveled_stops by route for SEPTA" in {
    val calculation = TimeTraveledStops.calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)

    getResultByRouteId(byRoute, "AIR") should be (3.95000 +- 1e-1)
    getResultByRouteId(byRoute, "CHE") should be (2.96923 +- 1e-1)
    getResultByRouteId(byRoute, "CHW") should be (3.17777 +- 1e-1)
    getResultByRouteId(byRoute, "CYN") should be (4.52631 +- 1e-1)
    getResultByRouteId(byRoute, "FOX") should be (4.25531 +- 1e-1)
    getResultByRouteId(byRoute, "LAN") should be (3.82812 +- 1e-1)
    getResultByRouteId(byRoute, "MED") should be (3.18400 +- 1e-1)
    getResultByRouteId(byRoute, "NOR") should be (3.61290 +- 1e-1)
    getResultByRouteId(byRoute, "PAO") should be (3.56470 +- 1e-1)
    getResultByRouteId(byRoute, "TRE") should be (5.10370 +- 1e-1)
    getResultByRouteId(byRoute, "WAR") should be (3.95614 +- 1e-1)
    getResultByRouteId(byRoute, "WIL") should be (3.87821 +- 1e-1)
    getResultByRouteId(byRoute, "WTR") should be (3.67724 +- 1e-1)
  }

  it should "calculate overall time_traveled_stops by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(TimeTraveledStops)

    getResultByRouteId(byRoute, "AIR") should be (3.70315 +- 1e-1)
    getResultByRouteId(byRoute, "CHE") should be (2.81119 +- 1e-1)
    getResultByRouteId(byRoute, "CHW") should be (3.07009 +- 1e-1)
    getResultByRouteId(byRoute, "CYN") should be (4.59710 +- 1e-1)
    getResultByRouteId(byRoute, "FOX") should be (3.93347 +- 1e-1)
    findRouteById(byRoute.keys, "GLN") should be (None)
    getResultByRouteId(byRoute, "LAN") should be (3.52410 +- 1e-1)
    getResultByRouteId(byRoute, "MED") should be (2.91718 +- 1e-1)
    getResultByRouteId(byRoute, "NOR") should be (3.49210 +- 1e-1)
    getResultByRouteId(byRoute, "PAO") should be (3.18547 +- 1e-1)
    getResultByRouteId(byRoute, "TRE") should be (4.79009 +- 1e-1)
    getResultByRouteId(byRoute, "WAR") should be (3.59415 +- 1e-1)
    getResultByRouteId(byRoute, "WIL") should be (3.44249 +- 1e-1)
    getResultByRouteId(byRoute, "WTR") should be (3.45668 +- 1e-1)
  }

  it should "calculate time_traveled_stops by system for SEPTA" in {
    val calculation = TimeTraveledStops.calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.get should be (3.78026 +- 1e-1)
  }

  it should "calculate overall time_traveled_stops by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(TimeTraveledStops)
    bySystem.get should be (3.46785 +- 1e-1)
  }
}
