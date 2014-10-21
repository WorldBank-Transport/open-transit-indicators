package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._


class AdHocWeeklyServiceHoursSpec extends AdHocSystemIndicatorSpec {
  it should "Accurately calculate the number of weekly service hours for routes" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = WeeklyServiceHours.runTest(List(allStopsPeriod), systemBuilder)
    implicit val routeMap = byRoute

    routeById("EastRail") should be (93.5 +- 1e-5)
    routeById("EastBus") should be (88.2 +- 1e-5)
    routeById("NorthSouth") should be (127.28333 +- 1e-5)
    routeById("WestRail") should be (112.5 +- 1e-5)
    routeById("EastWest") should be (127.283333 +- 1e-5)
    routeById("WestBus") should be (112.2 +- 1e-5)
  }
}


class WeeklyServiceHoursSpec extends FlatSpec with Matchers with IndicatorSpec {

  it should "calculate hours_service by mode for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = WeeklyServiceHours.runTest(periods, systemBuilder)
    byRouteType(Rail) should be (139.05)
  }

  it should "calculate hours_service by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = WeeklyServiceHours.runTest(periods, systemBuilder)

    getResultByRouteId(byRoute, "AIR") should be (137.55 +- 1e-5)
    getResultByRouteId(byRoute, "CHE") should be (126.8 +- 1e-5)
    getResultByRouteId(byRoute, "CHW") should be (124.9 +- 1e-5)
    getResultByRouteId(byRoute, "CYN") should be (83.01 +- 1e-5)
    getResultByRouteId(byRoute, "FOX") should be (124.35 +- 1e-5)
    getResultByRouteId(byRoute, "LAN") should be (129.83333 +- 1e-5)
    getResultByRouteId(byRoute, "MED") should be (128.466667 +- 1e-5)
    getResultByRouteId(byRoute, "NOR") should be (126.98333 +- 1e-5)
    getResultByRouteId(byRoute, "PAO") should be (132.05 +- 1e-5)
    getResultByRouteId(byRoute, "TRE") should be (136.65 +- 1e-5)
    getResultByRouteId(byRoute, "WAR") should be (139.05 +- 1e-5)
    getResultByRouteId(byRoute, "WIL") should be (133.133333 +- 1e-5)
    getResultByRouteId(byRoute, "WTR") should be (130.5 +- 1e-5)
  }

  it should "calculate hours_service by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = WeeklyServiceHours.runTest(periods, systemBuilder)

    bySystem.get should be (139.05 +- 1e-5)
  }

  it should "calculate overall hours_service by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = WeeklyServiceHours.runTest(periods, systemBuilder)
    bySystem.get should be (139.05 +- 1e-5)
  }
}
