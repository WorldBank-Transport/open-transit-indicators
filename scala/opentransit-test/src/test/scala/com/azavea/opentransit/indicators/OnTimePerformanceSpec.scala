package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._
import org.scalatest.OptionValues._

class OnTimePerformanceSpec
    extends FlatSpec
    with Matchers
    with IndicatorSpec
    with ObservedStopTimeSpec {


  val observedMapping = new ObservedStopTimeSpecParams {}

  // Noise with a mean value of 7.5 minutes was introduced to create this simulated observational data
  it should "calculate on time performance for SEPTA" in {
    val calculation = new OnTimePerformance(observedMapping).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.get should be (7.5 +- 1e-1)
  }

}
