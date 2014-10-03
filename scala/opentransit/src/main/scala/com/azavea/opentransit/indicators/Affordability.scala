
package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

class Affordability(params: IndicatorCalculationParams)
    extends Indicator
       with AggregatesBySystem {
  type Intermediate = Double

  val name = "affordability"
  
  /* This indicatior is a simple calculation from the parameters.
   * We fit it into the map reduce pattern by having map do nothing,
   * and having reduce just return the result.
   */
  val calculation =
    new PerRouteIndicatorCalculation[Double] {
      def map(trash: Seq[Trip]): Double = 0.0

      /* Returns expected cost of using the system 
       * as percentage of poverty line
       *
       * 42 is "average amount of trips per week"
       */
      def reduce(trash: Seq[Double]): Double =
        (42 * params.averageFare) / (params.povertyLine / 12)*100
    }
}
