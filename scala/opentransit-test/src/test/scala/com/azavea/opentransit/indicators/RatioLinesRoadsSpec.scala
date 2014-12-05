package com.azavea.opentransit.indicators

import geotrellis.vector._

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._
import org.scalatest.OptionValues._

class RatioLinesRoadsSpec
    extends FlatSpec
    with Matchers
    with IndicatorSpec
    with RoadLengthSpec {

  val totalRoadLength = new RoadLengthSpecParams {}

  /**This test is fine - the OSM data is artificially tiny so that the SQL we commit for testing
   *  doesn't fill the repo.
   * In postgis, I get ~248.9km for the road length.
   * The new LineDissolve from geotrellis.vector for SEPTA rail is ~472.8km
   */
  it should "calculate the ratio of transit system length to road system length for SEPTA/Philadelphia" in {
    val calculation = new RatioLinesRoads(totalRoadLength).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.isDefined should be (true)
    val roadLength = 248.9
    val transitLength = 472.8
    bySystem.value should be (transitLength/roadLength +- 1e-2)
  }
}
