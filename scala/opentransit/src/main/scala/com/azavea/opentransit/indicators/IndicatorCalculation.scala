package com.azavea.opentransit.indicators

import com.azavea.gtfs._

trait IndicatorRegister {
  def register(registerFunction: IndicatorCalculator => Unit, aggregatesBy: Aggregate => Boolean, name: String): Unit
}

/** Indicator that calculates intermeidate values of type T */
trait IndicatorCalculation[T] extends IndicatorRegister {
  def reduce(results: Seq[T]): Double

  def register(registerFunction: IndicatorCalculator => Unit, aggregatesBy: Aggregate => Boolean, name: String): Unit =
    registerFunction(new IndicatorCalculator {
      def apply(systemsByPeriod: Map[SamplePeriod, TransitSystem])(sink: Seq[ContainerGenerator] => Unit): Unit =
        CalculateIndicator.apply(IndicatorCalculation.this, systemsByPeriod, aggregatesBy, name)(sink)
    })

}

trait PerRouteIndicatorCalculation[T] extends IndicatorCalculation[T] {
  def map(trips: Seq[Trip]): T
}

trait PerTripIndicatorCalculation[T] extends IndicatorCalculation[T] {
  def map(trip: Trip): T
}

