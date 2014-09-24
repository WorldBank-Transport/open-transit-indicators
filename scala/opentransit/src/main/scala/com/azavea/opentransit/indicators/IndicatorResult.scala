package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import geotrellis.vector._
import geotrellis.vector.json._

object IndicatorResultContainer {
  final val OVERALL_KEY = "alltime"
  final val SYSTEM_KEY = "system"
  final val MODE_KEY = "mode"
  final val ROUTE_KEY = "route"
}

/** This class is a container which will
    ship the indicator result to the Django service
    that saves off indicators. */
case class IndicatorResultContainer(
  indicatorId: String,
  samplePeriodType: String,
  aggregation: Aggregate,
  value: Double,
  geom: Geometry,
  version: String,
  routeId: String = "",
  routeType: Option[RouteType] = None,
  cityBounded: Boolean = false
)

trait ContainerGenerator {
  def toContainer(version: String): IndicatorResultContainer 
}

object PeriodIndicatorResult { 
  def apply(indicatorId: String, period: SamplePeriod, value: Double): PeriodIndicatorResult =
    new PeriodIndicatorResult(indicatorId, period, value)
}

class PeriodIndicatorResult(indicatorId: String, period: SamplePeriod, value: Double) {
  def forRoute(route: Route, ml: MultiLine) =
    new ContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          indicatorId,
          period.periodType,
          RouteAggregate,
          value,
          ml,
          version,
          routeId = route.id
        )
    }

  def forRouteType(routeType: RouteType, ml: MultiLine) =
    new ContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          indicatorId,
          period.periodType,
          RouteTypeAggregate,
          value,
          ml,
          version,
          routeType = Some(routeType)
        )
    }

  def forSystem(ml: MultiLine) = 
    new ContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          indicatorId,
          period.periodType,
          SystemAggregate,
          value,
          ml,
          version = version
        )
    }
}

object OverallIndicatorResult { 
  def apply(indicatorId: String, value: Double): OverallIndicatorResult =
    new OverallIndicatorResult(indicatorId, value)
}

class OverallIndicatorResult(indicatorId: String, value: Double) {
  def forRoute(route: Route, ml: MultiLine) =
    new ContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          indicatorId,
          IndicatorResultContainer.OVERALL_KEY,
          RouteAggregate,
          value,
          ml,
          version,
          routeId = route.id
        )
    }

  def forRouteType(routeType: RouteType, ml: MultiLine) =
    new ContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          indicatorId,
          IndicatorResultContainer.OVERALL_KEY,
          RouteTypeAggregate,
          value,
          ml,
          version,
          routeType = Some(routeType)
        )
    }

  def forSystem(ml: MultiLine) = 
    new ContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          indicatorId,
          IndicatorResultContainer.OVERALL_KEY,
          SystemAggregate,
          value,
          ml,
          version
        )
    }
}
