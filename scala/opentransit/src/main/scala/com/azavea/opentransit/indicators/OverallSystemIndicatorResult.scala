package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import geotrellis.vector._

import spray.json._

object OverallIndicatorResult {
  def apply(indicatorId: String, value: Double): OverallIndicatorResult =
    new OverallIndicatorResult(indicatorId, value)

  def createContainerGenerators(name: String, results: AggregatedResults, geometries: SystemGeometries): Seq[ContainerGenerator] = {
    val AggregatedResults(byRoute, byRouteType, bySystem) = results

    val containersByRoute: Iterable[ContainerGenerator] =
      byRoute.map { case (route, value) =>
        OverallIndicatorResult(name, value).forRoute(route, geometries.byRouteGeoJson(route))
      }

    val containersByRouteType: Iterable[ContainerGenerator] =
      byRouteType.map { case (routeType, value) =>
        OverallIndicatorResult(name, value).forRouteType(routeType, geometries.byRouteTypeGeoJson(routeType))
      }

    val containerForSystem: Seq[ContainerGenerator] =
      bySystem match {
        case Some(v) =>
          Seq(OverallIndicatorResult(name, v).forSystem(geometries.bySystemGeoJson))
        case None =>
          Seq()
      }

    Seq(containersByRoute, containersByRouteType, containerForSystem).flatten
  }
}

class OverallIndicatorResult(indicatorId: String, value: Double) {
  def forRoute(route: Route, geoJson: JsValue) =
    new ContainerGenerator {
      def toContainer(version: String): IndicatorResultContainer =
        IndicatorResultContainer(
          indicatorId,
          IndicatorResultContainer.OVERALL_KEY,
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
          IndicatorResultContainer.OVERALL_KEY,
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
          IndicatorResultContainer.OVERALL_KEY,
          SystemAggregate,
          value,
          geoJson,
          version
        )
    }
}
