package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import org.scalatest._
import org.scalatest.OptionValues._

class DwellTimeSpec extends FlatSpec with Matchers with IndicatorSpec with ObservedStopTimeSpec {
  val observedMapping = new ObservedStopTimeSpecParams {}
  it should "calculate dwell time performance for SEPTA" in {
    val calculation = new DwellTimePerformance(observedMapping).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    // The arrival / departure times were randomly nudged by up to 15 minutes in either direction,
    // which results in the average dwell time deviation being 5 minutes. I haven't quite wrapped
    // my head around why this should be the case mathematically, but independently calculating
    // the dwell time in Python against the source CSV confirms that this is the correct value.
    bySystem.get should be (5.0 +- 1e-1)
  }
}
