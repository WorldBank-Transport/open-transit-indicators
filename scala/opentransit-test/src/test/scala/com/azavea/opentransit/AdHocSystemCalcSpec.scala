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
  val sundayPeriod = SamplePeriod(1, "sundaystops",
    new LocalDateTime(2014, 2, 2, 5, 0),
    new LocalDateTime(2014, 2, 2, 23, 59))
  val systemWithAllStops =
    systemBuilder.systemBetween(allStopsPeriod.start, allStopsPeriod.end) // Weekday
  val systemWithoutSomeStops =
    systemBuilder.systemBetween(missingStopsPeriod.start, missingStopsPeriod.end) // Weekday
  val systemSunday =
    systemBuilder.systemBetween(sundayPeriod.start, sundayPeriod.end) // Sunday

  def routeById(routeId: String)(implicit routeMap: Map[Route, Double]): Double = {
    val routeIdMap = routeMap.map{case (k, v) => (k.id -> v)}.toMap
    routeIdMap(routeId)
  }

}

// For an example of a test utilizing the AdHoc System, see indicators/TimeTraveledStopsSpec.scala
