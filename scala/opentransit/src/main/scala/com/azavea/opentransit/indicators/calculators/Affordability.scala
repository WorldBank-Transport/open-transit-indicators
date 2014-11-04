package com.azavea.opentransit.indicators.calculators

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.parameters._

class Affordability(params: StaticParams)
    extends Indicator
       with AggregatesBySystem {
  type Intermediate = Double

  val name = "affordability"

  /* This indicatior is a simple calculation from the parameters.
   * We fit it into the map reduce pattern by having map do nothing,
   * and having reduce just return the result.
   */
  def calculation(period: SamplePeriod) = {
    def calculate(transitSystem: TransitSystem) = {
      val avgFare = params.settings.averageFare
      val povertyLine = params.settings.povertyLine
      val avgTripsPerWeek = 42
      val monthsPerYear = 12
      val systemResult = (avgTripsPerWeek * avgFare) / (povertyLine / monthsPerYear)*100
      AggregatedResults.systemOnly(systemResult)
    }
    perSystemCalculation(calculate)
  }
}
