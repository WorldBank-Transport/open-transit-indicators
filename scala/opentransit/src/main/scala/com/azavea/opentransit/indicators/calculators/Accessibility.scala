package com.azavea.opentransit.indicators.calculators

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.database.DemographicsTable
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.parameters._

import geotrellis.vector._

import com.github.nscala_time.time.Imports._
import org.joda.time._

// Calculates population within buffer of stops
abstract class Accessibility(params: StopBuffers with Demographics)
    extends Indicator with AggregatesBySystem {

  val name: String

  val columnName: String

  def calculation(period: SamplePeriod) = {
    def calculate(transitSytem: TransitSystem) = {
      val stopBuffer = params.bufferForPeriod(period)
      val systemResult = params.populationMetricForBuffer(stopBuffer, columnName)
      AggregatedResults.systemOnly(systemResult)
    }
    perSystemCalculation(calculate)
  }
}

// Used for total population metric
class AllAccessibility(params: StopBuffers with Demographics) extends Accessibility(params) {
  val name = "system_access"
  val columnName = "populationMetric1"
}

// Used for low income accessibility metric
class LowIncomeAccessibility(params: StopBuffers with Demographics) extends Accessibility(params) {
  val name = "system_access_low"
  val columnName = "populationMetric2"
}
