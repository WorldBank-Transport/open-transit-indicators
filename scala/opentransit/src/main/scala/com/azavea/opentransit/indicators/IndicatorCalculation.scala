package com.azavea.opentransit.indicators

import com.azavea.gtfs._

trait IndicatorCalculation {
  type Intermediate 

  def reduce(results: Seq[Intermediate]): Double
}

trait PerRouteIndicatorCalculation extends IndicatorCalculation {
  def map(trips: Seq[Trip]): Intermediate
}

trait PerTripIndicatorCalculation extends IndicatorCalculation {
  def map(trip: Trip): Intermediate
}

