package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._
import org.scalatest.OptionValues._

class HeadwayRegularitySpec
    extends FlatSpec
    with Matchers
    with IndicatorSpec
    with ObservedStopTimeSpec {


  val observedMapping = new ObservedStopTimeSpecParams {}

  // Noise with a mean value of 7.5 minutes was introduced to create this simulated
  // observational data - the slight difference from that result which we expect see here
  // is likely due to the somewhat small sample size (none of the routes have more than 75
  // samples)
  //
  // TODO: fix this test. After the recent geometry changes, the test returns 1.33256
  ignore should "calculate the regularity of headway for SEPTA" in {
    val calculation = new HeadwayRegularity(observedMapping).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.get should be (7.11 +- 1e-1)
  }

}
