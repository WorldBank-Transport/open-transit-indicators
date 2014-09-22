package com.azavea.opentransit.indicators

object Indicators {
  // Add new indicators here!
  val indicators = List(
    NumStops
  )
}

trait Indicator { self: AggregatesBy =>
  val name: String
  val calculation: IndicatorCalculation

  def register(): Unit = ???
    // IndicatorRegistry.register(this, 
    // aggregates.map(IndicatorRegistry.register(this, name, calculation, _))
}
