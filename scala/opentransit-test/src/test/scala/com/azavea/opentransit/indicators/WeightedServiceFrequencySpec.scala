package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators.calculators._

import org.scalatest._


class WeightedServiceFrequencySpec
    extends FlatSpec
    with Matchers
    with IndicatorSpec
    with DemographicsSpec
    with StopBuffersSpec {

  val demographicsAndBuffers = new StopBuffersSpecParams with DemographicsSpecParams {}

  /**The result 0.29 is in the ballpark of 0.22 (which is the result
   * of the unweighted Avg Svc Freq indicator)
   */
  it should "calculate service frequency weighted by population served for SEPTA" in {
    val calculation = new AllWeightedServiceFrequency(demographicsAndBuffers).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.isDefined should be (true)
    bySystem.get should be (17.87 +- 1e-2) // minutes
  }

  it should "calculate service frequency weighted by low income population served for SEPTA" in {
    val calculation = new LowIncomeWeightedServiceFrequency(demographicsAndBuffers).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.isDefined should be (true)
    bySystem.get should be (18.61 +- 1e-2) // minutes
  }
}

