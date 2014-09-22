package com.azavea.opentransit.indicators

abstract sealed class Aggregate
case object RouteAggregate
case object ModeAggregate
case object SystemAggregate

sealed trait AggregatesBy {
  def aggregates: List[Aggregate] = List[Aggregate]()
}

trait AggregatesByRoute extends Indicator {
  abstract override aggregates = RouteAggregate :: super.aggregates
}

trait AggregatesByMode extends Indicator {
  abstract override aggregates = ModeAggregate :: super.aggregates
}

trait AggregatesBySystem extends Indicator {
  abstract override aggregates = SystemAggregate :: super.aggregates
}

trait AggregatesByAll extends AggregatesByRoute with AggregatesByMode with AggregatesBySystem
