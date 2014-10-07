
package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

class Affordability(params: IndicatorParams)
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
      val averageFare = params.settings.averageFare
      val povertyLine = params.settings.povertyLine
      val avgTripsPerWeek = 42
      val monthsPerYear = 12
      val systemResult = (avgTripsPerWeek * averageFare) / (povertyLine / montsPerYear)*100
      AggregatedResults.systemOnly(systemResult)
    }
    perSystemCalculation(calculate)
  }
}
