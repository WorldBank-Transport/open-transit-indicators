package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._

class NumRoutesSpec extends FlatSpec with Matchers with IndicatorSpec {
  it should "calculate num_routes by mode for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = NumRoutes(system)
    byRouteType(Rail) should be (13)
  }

  it should "calculate overall num_routes by mode for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(NumRoutes)
    byRouteType(Rail) should be (12.45833 +- 1e-5)
  }

  // TODO: the results to the following two tests after the refactor look off
  /*
  it should "calculate num_routes by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = NumRoutes(system)

    getResultByRouteId(byRoute, "AIR") should be (1)
    getResultByRouteId(byRoute, "CHE") should be (1)
    getResultByRouteId(byRoute, "CHW") should be (1)
    getResultByRouteId(byRoute, "CYN") should be (1)
    getResultByRouteId(byRoute, "FOX") should be (1)
    getResultByRouteId(byRoute, "LAN") should be (1)
    getResultByRouteId(byRoute, "MED") should be (1)
    getResultByRouteId(byRoute, "NOR") should be (1)
    getResultByRouteId(byRoute, "PAO") should be (1)
    getResultByRouteId(byRoute, "TRE") should be (1)
    getResultByRouteId(byRoute, "WAR") should be (1)
    getResultByRouteId(byRoute, "WIL") should be (1)
    getResultByRouteId(byRoute, "WTR") should be (1)
  }

  it should "calculate overall num_routes by route for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(NumRoutes)

    getResultByRouteId(byRoute, "AIR") should be (1.0000 +- 1e-4)
    getResultByRouteId(byRoute, "CHE") should be (1.0000 +- 1e-4)
    getResultByRouteId(byRoute, "CHW") should be (1.0000 +- 1e-4)
    getResultByRouteId(byRoute, "CYN") should be (0.4017 +- 1e-4)
    getResultByRouteId(byRoute, "FOX") should be (1.0000 +- 1e-4)
    getResultByRouteId(byRoute, "GLN") should be (0.0000 +- 1e-4)
    getResultByRouteId(byRoute, "LAN") should be (1.0000 +- 1e-4)
    getResultByRouteId(byRoute, "MED") should be (1.0000 +- 1e-4)
    getResultByRouteId(byRoute, "NOR") should be (1.0000 +- 1e-4)
    getResultByRouteId(byRoute, "PAO") should be (1.0000 +- 1e-4)
    getResultByRouteId(byRoute, "TRE") should be (1.0000 +- 1e-4)
    getResultByRouteId(byRoute, "WAR") should be (1.0000 +- 1e-4)
    getResultByRouteId(byRoute, "WIL") should be (1.0000 +- 1e-4)
    getResultByRouteId(byRoute, "WTR") should be (1.0000 +- 1e-4)
  }
   */

  it should "calculate num_routes by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = NumRoutes(system)

    bySystem.get should be (13)
  }

  it should "calculate overall num_routes by system for SEPTA" in {
    val AggregatedResults(byRoute, byRouteType, bySystem) = septaOverall(NumRoutes)

    bySystem.get should be (12.45833 +- 1e-5)
  }
}
