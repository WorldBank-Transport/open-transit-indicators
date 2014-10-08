package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit.io.GtfsIngest

import com.azavea.opentransit.testkit._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._

import scala.slick.jdbc.JdbcBackend.Session
import scala.util.{Try, Success, Failure}


class TravelTimePerformanceSpec extends FlatSpec with Matchers with IndicatorSpec {
  it should "calculate travel time performance by mode for SEPTA" in {
    val calculation = TravelTimePerformance.calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    byRouteType(Rail) should be (13)
  }

  it should "calculate num_routes by route for SEPTA" in {
    val calculation = NumRoutes.calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)

    getResultByRouteId(byRoute, "AIR") should be (15 plusorMinus 5)
    getResultByRouteId(byRoute, "CHE") should be (15 plusOrMinus 5)
    getResultByRouteId(byRoute, "CHW") should be (15 plusOrMinus 5)
    getResultByRouteId(byRoute, "CYN") should be (15 plusOrMinus 5)
    getResultByRouteId(byRoute, "FOX") should be (15 plusOrMinus 5)
    getResultByRouteId(byRoute, "LAN") should be (15 plusOrMinus 5)
  }
}
// vim: set ts=2 sw=2 et sts=2:
