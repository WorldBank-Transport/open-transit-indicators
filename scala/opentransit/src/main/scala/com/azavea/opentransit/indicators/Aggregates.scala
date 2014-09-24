package com.azavea.opentransit.indicators

abstract sealed class Aggregate
case object RouteAggregate extends Aggregate        // Indicator should contribute results aggregated by Route
case object RouteTypeAggregate extends Aggregate    // Indicator should contribute results aggregated by RouteType
case object SystemAggregate extends Aggregate       // Indicator should contribute results aggregated by System

/** Stackable trait that describes what sort of Aggregation (byRoute, byRouteType, bySystem)
  * should be calculated for an indicator.
  */
sealed trait AggregatesBy {
  def aggregates: List[Aggregate] = List[Aggregate]()
}

trait AggregatesByRoute extends AggregatesBy {
  abstract override def aggregates = RouteAggregate :: super.aggregates
}

trait AggregatesByMode extends AggregatesBy {
  abstract override def aggregates = RouteTypeAggregate :: super.aggregates
}

trait AggregatesBySystem extends AggregatesBy {
  abstract override def aggregates = SystemAggregate :: super.aggregates
}

trait AggregatesByAll extends AggregatesByRoute with AggregatesByMode with AggregatesBySystem
