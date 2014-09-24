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
  `type`: String,
  sample_period: String,
  aggregation: String = "",
  route_id: String = "",
  route_type: Int = 0,
  city_bounded: Boolean = false,
  version: String = "",
  value: Double = 0,
  the_geom: String = ""
)

trait IndicatorContainerGenerator {
  def toContainer(version: String): IndicatorResultContainer 
}

class PeriodIndicatorResult(indicatorId: String, period: SamplePeriod, value: Double) {
  def forRoute(route: Route, line: Line) =
    new IndicatorContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          `type`= indicatorId,
          sample_period = period.`type`,
          aggregation=IndicatorResultContainer.ROUTE_KEY,
          route_id = route.id,
          value = value,
          version = version,
          the_geom = line.toGeoJson
        )
    }

  def forRouteType(routeType: RouteType, ml: MultiLine) =
    new IndicatorContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          `type`= indicatorId,
          sample_period = period.`type`,
          aggregation=IndicatorResultContainer.ROUTE_KEY,
          route_type = routeType.id,
          value = value,
          version = version,
          the_geom = ml.toGeoJson
        )
    }

  def forSystem(ml: MultiLine) = 
    new IndicatorContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          `type` = indicatorId,
          sample_period = period.`type`,
          aggregation = IndicatorResultContainer.SYSTEM_KEY,
          version = version,
          value = value,
          the_geom = ml.toGeoJson
        )
    }
}

class OverallIndicatorResult(indicatorId: String, value: Double) {
  def forRoute(route: Route, line: Line) =
    new IndicatorContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          `type`= indicatorId,
          sample_period = IndicatorResultContainer.OVERALL_KEY,
          aggregation=IndicatorResultContainer.ROUTE_KEY,
          route_id = route.id,
          value = value,
          version = version,
          the_geom = line.toGeoJson
        )
    }

  def forRouteType(routeType: RouteType, ml: MultiLine) =
    new IndicatorContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          `type`= indicatorId,
          sample_period = IndicatorResultContainer.OVERALL_KEY,
          aggregation=IndicatorResultContainer.ROUTE_KEY,
          route_type = routeType.id,
          value = value,
          version = version,
          the_geom = ml.toGeoJson
        )
    }

  def forSystem(ml: MultiLine) = 
    new IndicatorContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          `type` = indicatorId,
          sample_period = IndicatorResultContainer.OVERALL_KEY,
          aggregation = IndicatorResultContainer.SYSTEM_KEY,
          version = version,
          value = value,
          the_geom = ml.toGeoJson
        )
    }
}
