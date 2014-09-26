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
      TimeTraveledStops
    )

  // These are indicators that need to know things about the request
  private def paramIndicators(params: IndicatorCalculationParams): List[Indicator] = 
    List(
      new CoverageRatioStopsBuffer(params)
    )

  def list(params: IndicatorCalculationParams): List[Indicator] =
    staticIndicators ++ paramIndicators(params)
}

trait Indicator extends TransitSystemCalculation { self: AggregatesBy =>
  type Intermediate

  val name: String
  val calculation: IndicatorCalculation[Intermediate]

  def aggregatesBy(aggregate: Aggregate) =
    self.aggregates.contains(aggregate)

  def apply(transitSystem: TransitSystem): AggregatedResults =
    calculation(transitSystem, aggregatesBy _)
}
