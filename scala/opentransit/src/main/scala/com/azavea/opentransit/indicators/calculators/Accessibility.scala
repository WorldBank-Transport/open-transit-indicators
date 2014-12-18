package com.azavea.opentransit.indicators.calculators

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.database.DemographicsTable
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.parameters._

import geotrellis.vector._
import geotrellis.slick._

import com.github.nscala_time.time.Imports._
import org.joda.time._

// Calculates population within buffer of stops
abstract class Accessibility(params: StopBuffers with Demographics with Boundaries)
    extends Indicator with AggregatesBySystem {

  val name: String

  val columnName: String

  def calculation(period: SamplePeriod) = {
    def calculate(transitSytem: TransitSystem) = {
      val stopBuffer = params.bufferForPeriod(period)
      val popServed = params.populationMetricForBuffer(stopBuffer, columnName)
      val totalPop = params.populationMetricForBuffer(Projected(params.cityBoundary, stopBuffer.srid), columnName)
      AggregatedResults.systemOnly(if (totalPop > 0) (popServed/totalPop * 100) else 100) // Prevent infinity result
     }
    perSystemCalculation(calculate)
  }
}

// Used for total population metric
class AllAccessibility(params: StopBuffers with Demographics with Boundaries)
    extends Accessibility(params) {
  val name = "system_access"
  val columnName = "populationMetric1"
}

// Used for low income accessibility metric
class LowIncomeAccessibility(params: StopBuffers with Demographics with Boundaries)
    extends Accessibility(params) {
  val name = "system_access_low"
  val columnName = "populationMetric2"
}
