package com.azavea.opentransit.indicators

import com.azavea.gtfs.TransitSystem
import scala.collection.mutable

trait TransitSystemCalculation {
  def apply(transitSystem: TransitSystem): AggregatedResults
}

object Indicators {
  // Add new indicators here!
  val list: List[Indicator] = List(
    AverageServiceFrequency,
    DistanceStops,
    Length,
    NumRoutes,
    NumStops,
    TimeTraveledStops
  )
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
