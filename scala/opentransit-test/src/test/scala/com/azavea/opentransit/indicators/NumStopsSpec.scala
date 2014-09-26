package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._

class NumStopsSpec extends FlatSpec with Matchers with IndicatorSpec {

  it should "calculate num_stops by mode for SEPTA" in {
    println(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = NumStops(system)
    byRouteType(Rail) should be (154)
  }

  it should "calculate overall num_stops by mode for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) =
      septaOverall(NumStops)

    byRouteType(Rail) should be (150.72149 +- 1e-5)
  }

  it should "calculate num_stops by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = NumStops(system)

    getResultByRouteId(byRoute, "AIR") should be (10)
    getResultByRouteId(byRoute, "CHE") should be (15)
    getResultByRouteId(byRoute, "CHW") should be (14)
    getResultByRouteId(byRoute, "CYN") should be (5)
    getResultByRouteId(byRoute, "FOX") should be (10)
    findRouteById(byRoute.keys, "GLN") should be (None)
    getResultByRouteId(byRoute, "LAN") should be (27)
    getResultByRouteId(byRoute, "MED") should be (19)
    getResultByRouteId(byRoute, "NOR") should be (17)
    getResultByRouteId(byRoute, "PAO") should be (26)
    getResultByRouteId(byRoute, "TRE") should be (15)
    getResultByRouteId(byRoute, "WAR") should be (17)
    getResultByRouteId(byRoute, "WIL") should be (22)
    getResultByRouteId(byRoute, "WTR") should be (23)
  }

  it should "calculate overall num_stops by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(NumStops)

    getResultByRouteId(byRoute, "AIR") should be ( 9.99988 +- 1e-5)
    getResultByRouteId(byRoute, "CHE") should be (14.68733 +- 1e-5)
    getResultByRouteId(byRoute, "CHW") should be (13.99983 +- 1e-5)
    getResultByRouteId(byRoute, "CYN") should be ( 2.00892 +- 1e-5)
    getResultByRouteId(byRoute, "FOX") should be ( 9.71417 +- 1e-5)
    getResultByRouteId(byRoute, "GLN") should be ( 0.00000 +- 1e-5)
    getResultByRouteId(byRoute, "LAN") should be (24.68423 +- 1e-5)
    getResultByRouteId(byRoute, "MED") should be (18.99978 +- 1e-5)
    getResultByRouteId(byRoute, "NOR") should be (16.99980 +- 1e-5)
    getResultByRouteId(byRoute, "PAO") should be (25.99969 +- 1e-5)
    getResultByRouteId(byRoute, "TRE") should be (14.77662 +- 1e-5)
    getResultByRouteId(byRoute, "WAR") should be (16.99980 +- 1e-5)
    getResultByRouteId(byRoute, "WIL") should be (20.07121 +- 1e-5)
    getResultByRouteId(byRoute, "WTR") should be (22.38963 +- 1e-5)
  }

  it should "calculate num_stops by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = NumStops(system)
    bySystem.get should be (154)
  }

  it should "calculate overall num_stops by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(NumStops)
    bySystem.get should be (150.72149 +- 1e-5)
  }
}
