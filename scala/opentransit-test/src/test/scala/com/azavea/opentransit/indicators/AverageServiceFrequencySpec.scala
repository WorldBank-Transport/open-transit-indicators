package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import org.scalatest._


class AdHocAverageServiceFrequencySpec extends AdHocSystemIndicatorSpec {
  it should "Calculate the average service frequency of our ad-hoc routes" in {
    val calculation = AverageServiceFrequency.calculation(sundayPeriod)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(systemSunday)
    implicit val routeMap = byRoute // Use this implicit to DRY out your tests

    routeById("EastWest") should be (0.5)
  }
}
