package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import org.scalatest._


class AdHocLengthSpec extends AdHocSystemIndicatorSpec {
  it should "Accurately calculate the length of routes" in {
    val calculation = Length.calculation(allStopsPeriod)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(systemWithAllStops)
    implicit val routeMap = byRoute

    // Only subway tripshapes exist in the AdHoc system.
    routeById("EastRail") should be (0.0)
    routeById("EastBus") should be (0.0)
    routeById("NorthSouth") should be (0.02) // Kilometers
    routeById("WestRail") should be (0.0)
    routeById("EastWest") should be (0.02) // Kilometers
    routeById("WestBus") should be (0.0)
  }
}


// TODO: fix these tests. After the recent geometry changes, the results are all off.
class LengthSpec extends FlatSpec with Matchers with IndicatorSpec {
  it should "calculate length by route for SEPTA" in {
    val calculation = Length.calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    implicit val routeMap = byRoute

    // NOTE: These are results for SEPTA regional rail, but the trip shapes
    // for SEPTA are completely messed up, so these shouldn't be taken as
    // representing actual values for SEPTA, but rather indicators of whether
    // the indicator calculations remain consistent.
    getResultByRouteId(byRoute, "AIR") should be ( 21.86907 +- 1e-5)
    getResultByRouteId(byRoute, "CHE") should be ( 22.47520 +- 1e-5)
    getResultByRouteId(byRoute, "CHW") should be ( 17.77621 +- 1e-5)
    getResultByRouteId(byRoute, "CYN") should be ( 10.08848 +- 1e-5)
    getResultByRouteId(byRoute, "FOX") should be ( 14.58365 +- 1e-5)
    getResultByRouteId(byRoute, "LAN") should be ( 59.69138 +- 1e-5)
    getResultByRouteId(byRoute, "MED") should be ( 58.33978 +- 1e-5)
    getResultByRouteId(byRoute, "NOR") should be ( 34.50016 +- 1e-5)
    getResultByRouteId(byRoute, "PAO") should be ( 61.05067 +- 1e-5)
    getResultByRouteId(byRoute, "TRE") should be (117.48124 +- 1e-5)
    getResultByRouteId(byRoute, "WAR") should be ( 45.30601 +- 1e-5)
    getResultByRouteId(byRoute, "WIL") should be (130.04707 +- 1e-5)
    getResultByRouteId(byRoute, "WTR") should be ( 57.57047 +- 1e-5)

  }

  it should "calculate overall length by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(Length)
    implicit val routeMap = byRoute

    getResultByRouteId(byRoute, "AIR") should be ( 20.95786 +- 1e-5)
    getResultByRouteId(byRoute, "CHE") should be ( 19.73522 +- 1e-5)
    getResultByRouteId(byRoute, "CHW") should be ( 19.07085 +- 1e-5)
    getResultByRouteId(byRoute, "CYN") should be ( 9.66813 +- 1e-5)
    getResultByRouteId(byRoute, "FOX") should be ( 13.97600 +- 1e-5)
    getResultByRouteId(byRoute, "LAN") should be ( 54.42701 +- 1e-5)
    getResultByRouteId(byRoute, "MED") should be ( 48.70017 +- 1e-5)
    getResultByRouteId(byRoute, "NOR") should be ( 33.06265 +- 1e-5)
    getResultByRouteId(byRoute, "PAO") should be ( 56.50100 +- 1e-5)
    getResultByRouteId(byRoute, "TRE") should be (112.13954 +- 1e-5)
    getResultByRouteId(byRoute, "WAR") should be ( 43.41826 +- 1e-5)
    getResultByRouteId(byRoute, "WIL") should be (124.62844 +- 1e-5)
    getResultByRouteId(byRoute, "WTR") should be ( 52.07998 +- 1e-5)
  }

  it should "calculate length by mode for SEPTA" in {
    val calculation = Length.calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)

    byRouteType(Rail) should be (650.77942 +- 1e-5)
  }

  it should "calculate overall length by mode for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(Length)

    byRouteType(Rail) should be (608.36514 +- 1e-5)
  }

  it should "calculate length by system for SEPTA" in {
    val calculation = Length.calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.get should be (650.77942 +- 1e-5)
  }

  it should "calculate overall length by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(Length)
    bySystem.get should be (608.36514 +- 1e-5)
  }
}
