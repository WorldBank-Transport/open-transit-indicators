package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import geotrellis.vector._
import geotrellis.slick._

import spray.json._

object OverallIndicatorResult {
  def apply(indicatorId: String, value: Double): OverallIndicatorResult =
    new OverallIndicatorResult(indicatorId, value)

  def createContainerGenerators(name: String, results: AggregatedResults, geometries: SystemGeometries): Seq[ContainerGenerator] = {
    val AggregatedResults(byRoute, byRouteType, bySystem) = results

    val containersByRoute: Iterable[ContainerGenerator] =
      byRoute.map { case (route, value) =>
        OverallIndicatorResult(name, value).forRoute(route, geometries.byRoute(route))
      }

    val containersByRouteType: Iterable[ContainerGenerator] =
      byRouteType.map { case (routeType, value) =>
        OverallIndicatorResult(name, value).forRouteType(routeType, geometries.byRouteType(routeType))
      }

    val containerForSystem: Seq[ContainerGenerator] =
      bySystem match {
        case Some(v) =>
          Seq(OverallIndicatorResult(name, v).forSystem(geometries.bySystem))
        case None =>
          Seq()
      }

    Seq(containersByRoute, containersByRouteType, containerForSystem).flatten
  }
}

class OverallIndicatorResult(indicatorId: String, value: Double) {
  def forRoute(route: Route, geometry: Geometry) =
    new ContainerGenerator {
      def toContainer(calculationJob: Int): IndicatorResultContainer =
        IndicatorResultContainer(
          indicatorId,
          IndicatorResultContainer.OVERALL_KEY,
          RouteAggregate,
          value,
          geometry,
          calculationJob,
          routeId = route.id
        )
    }

  def forRouteType(routeType: RouteType, geometry: Geometry) =
    new ContainerGenerator {
      def toContainer(calculationJob: Int): IndicatorResultContainer =
        IndicatorResultContainer(
          indicatorId,
          IndicatorResultContainer.OVERALL_KEY,
          RouteTypeAggregate,
          value,
          geometry,
          calculationJob,
          routeType = Some(routeType)
        )
    }

  def forSystem(geometry: Geometry) =
    new ContainerGenerator {
      def toContainer(calculationJob: Int): IndicatorResultContainer =
        IndicatorResultContainer(
          indicatorId,
          IndicatorResultContainer.OVERALL_KEY,
          SystemAggregate,
          value,
          geometry,
          calculationJob
        )
    }
}
