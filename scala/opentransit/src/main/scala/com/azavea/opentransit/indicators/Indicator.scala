package com.azavea.opentransit.indicators

import com.azavea.gtfs.TransitSystem
import scala.collection.mutable

trait IndicatorCalculator {
  def apply(systemsByPeriod: Map[SamplePeriod, TransitSystem])(sink: Seq[ContainerGenerator] => Unit): Unit
}

object Indicators {
  private val _calculations = mutable.ListBuffer[IndicatorCalculator]()
  def calculations = _calculations.toList

  // Add new indicators here!
  val list = List(
    AverageServiceFrequency,
    NumStops
  )

  for(indicator <- list) { 
    indicator.calculation.register(this.registerCalculation, indicator.aggregatesBy _, indicator.name) 
  }

  def registerCalculation(calculation: IndicatorCalculator): Unit =
    _calculations += calculation
}

trait Indicator { self: AggregatesBy =>
  val name: String
  val calculation: IndicatorRegister

  def aggregatesBy(aggregate: Aggregate) =
    self.aggregates.contains(aggregate)
}
