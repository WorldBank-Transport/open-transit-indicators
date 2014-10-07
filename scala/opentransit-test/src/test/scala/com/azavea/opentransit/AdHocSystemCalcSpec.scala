package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit.io.GtfsIngest

import com.azavea.opentransit.testkit._

import com.github.nscala_time.time.Imports._

import org.scalatest._

/**
* Inherit from this trait if you're interested in using ad hoc GTFS data;
* it will be easier to reason about
*/
trait AdHocSystemIndicatorSpec extends FlatSpec with Matchers {
  val systemBuilder = TransitSystemBuilder(TestGtfsRecords())
  val allStopsPeriod = SamplePeriod(1, "allstops",
    new LocalDateTime(2014, 2, 3, 5, 0),
    new LocalDateTime(2014, 2, 3, 18, 0))
  val missingStopsPeriod = SamplePeriod(1, "missingstops",
    new LocalDateTime(2014, 2, 3, 6, 0),
    new LocalDateTime(2014, 2, 3, 18, 0))
  val systemWithAllStops =
    systemBuilder.systemBetween(allStopsPeriod.start, allStopsPeriod.end) // Weekday
  val systemWithoutSomeStops =
    systemBuilder.systemBetween(missingStopsPeriod.start, missingStopsPeriod.end) // Weekday

  def routeById(routeId: String)(implicit routeMap: Map[Route, Double]): Double = {
    val routeIdMap = routeMap.map{case (k, v) => (k.id -> v)}.toMap
    routeIdMap(routeId)
  }

}


class AdHocDemoSpec extends AdHocSystemIndicatorSpec {

  it should "Calculate the length of our ad hoc routes" in {
    val calculation = TimeTraveledStops.calculation(allStopsPeriod)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(systemWithAllStops)
    implicit val routeMap = byRoute // Use this implicit to DRY out your tests

    routeById("EastRail") should be (50.0)
    routeById("EastBus") should be (22.0)
    routeById("NorthSouth") should be (6.0)
    routeById("WestRail") should be (50.0)
    routeById("EastWest") should be (6.0)
    routeById("WestBus") should be (22.0)
  }
}
