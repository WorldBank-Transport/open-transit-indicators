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

  /* 8.70 is significantly lower than the average frequency of SEPTA regional rail,
   * but remember that all the lines run through center city and center city
   * is more densely populated than the suburban stops the rail system serves.
   */
  it should "calculate service frequency weighted by population served for SEPTA" in {
    val calculation = new AllWeightedServiceFrequency(demographicsAndBuffers).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.isDefined should be (true)
    bySystem.get should be (8.70 +- 1e-2) // minutes
  }

  it should "calculate service frequency weighted by low income population served for SEPTA" in {
    val calculation = new LowIncomeWeightedServiceFrequency(demographicsAndBuffers).calculation(period)
    val AggregatedResults(byRoute, byRouteType, bySystem) = calculation(system)
    bySystem.isDefined should be (true)
    bySystem.get should be (9.01 +- 1e-2) // minutes
  }
}

