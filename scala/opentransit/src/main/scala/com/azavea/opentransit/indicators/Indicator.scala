package com.azavea.opentransit.indicators

object Indicators {
  // Add new indicators here!
  val list = List(
    NumStops
  )
}

trait Indicator { self: AggregatesBy =>
  val name: String
  val calculation: IndicatorCalculation
}
