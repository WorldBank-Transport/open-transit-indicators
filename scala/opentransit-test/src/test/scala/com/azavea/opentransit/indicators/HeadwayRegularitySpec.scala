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

  // The observed data was created by randomly increasing the arrival and departure times
  // for each stop by up to 15 minutes (mean of 7.5 minutes). This would not be expected to
  // change the average headway (time between arrivals), since all arrivals are shifted in
  // the same direction by the same amount on average. The actual value is roughly in line with
  // these expectations.
  it should "calculate the regularity of headway for SEPTA" in {
    val calculation = new HeadwayRegularity(observedMapping).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.get should be (0.6867 +- 1e-1)
  }

}
