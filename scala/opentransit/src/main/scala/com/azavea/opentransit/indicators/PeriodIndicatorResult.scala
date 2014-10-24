package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import geotrellis.vector._

import spray.json._

object PeriodIndicatorResult {
  def apply(indicatorId: String, period: SamplePeriod, value: Double): PeriodIndicatorResult =
    new PeriodIndicatorResult(indicatorId, period, value)

  def createContainerGenerators(name: String, period: SamplePeriod, results: AggregatedResults, geometries: SystemGeometries): Seq[ContainerGenerator] = {
    val AggregatedResults(byRoute, byRouteType, bySystem) = results

    val containersByRoute: Iterable[ContainerGenerator] =
      byRoute.map { case (route, value) =>
        PeriodIndicatorResult(name, period, value)
          .forRoute(route, geometries.byRouteGeoJson(route))
      }


    val containersByRouteType: Iterable[ContainerGenerator] =
      byRouteType.map { case (routeType, value) =>
        PeriodIndicatorResult(name, period, value).forRouteType(routeType, geometries.byRouteTypeGeoJson(routeType))
      }

    val containerForSystem: Iterable[ContainerGenerator] =
      bySystem match {
        case Some(v) =>
          Seq(PeriodIndicatorResult(name, period, v).forSystem(geometries.bySystemGeoJson))
        case None =>
          Seq()
      }
    Seq(containersByRoute, containersByRouteType, containerForSystem).flatten
  }
}

class PeriodIndicatorResult(indicatorId: String, period: SamplePeriod, value: Double) {
  def forRoute(route: Route, geoJson: JsValue) =
    new ContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          indicatorId,
          period.periodType,
          RouteAggregate,
          value,
          geoJson,
          version,
          routeId = route.id
        )
    }

  def forRouteType(routeType: RouteType, geoJson: JsValue) =
    new ContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          indicatorId,
          period.periodType,
          RouteTypeAggregate,
          value,
          geoJson,
          version,
          routeType = Some(routeType)
        )
    }

  def forSystem(geoJson: JsValue) =
    new ContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          indicatorId,
          period.periodType,
          SystemAggregate,
          value,
          geoJson,
          version = version
        )
    }
}
