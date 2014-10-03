package com.azavea.opentransit.indicators

import com.azavea.gtfs.TransitSystem
import scala.collection.mutable

trait TransitSystemCalculation {
  def apply(transitSystem: TransitSystem): AggregatedResults
}

object Indicators {
  // These are indicators that don't need the request info.
  private val staticIndicators: List[Indicator] =
    List(
      AverageServiceFrequency,
      DistanceStops,
      Length,
      NumRoutes,
      NumStops,
      TimeTraveledStops,
      InterstopDistance,
      StopsToLength
    )

  // These are indicators that need to know things about the request
  private def paramIndicators(params: IndicatorCalculationParams): List[Indicator] =
    List(
      new CoverageRatioStopsBuffer(params),
      new TransitNetworkDensity(params)
    )

  def list(params: IndicatorCalculationParams): List[Indicator] =
    staticIndicators ++ paramIndicators(params)
}

trait Indicator extends TransitSystemCalculation { self: AggregatesBy =>
  val name: String
  def calculation(period: SamplePeriod): IndicatorCalculation

  def aggregatesBy(aggregate: Aggregate) =
    self.aggregates.contains(aggregate)
}
