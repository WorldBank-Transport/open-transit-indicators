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
          .forRoute(route, geometries.byRoute(route))
      }


    val containersByRouteType: Iterable[ContainerGenerator] =
      byRouteType.map { case (routeType, value) =>
        PeriodIndicatorResult(name, period, value).forRouteType(routeType, geometries.byRouteType(routeType))
      }

    val containerForSystem: Iterable[ContainerGenerator] =
      bySystem match {
        case Some(v) =>
          Seq(PeriodIndicatorResult(name, period, v).forSystem(geometries.bySystem))
        case None =>
          Seq()
      }
    Seq(containersByRoute, containersByRouteType, containerForSystem).flatten
  }
}

class PeriodIndicatorResult(indicatorId: String, period: SamplePeriod, value: Double) {
  def forRoute(route: Route, geometry: Geometry) =
    new ContainerGenerator {
      def toContainer(calculationJob: Int): IndicatorResultContainer =
        IndicatorResultContainer(
          indicatorId,
          period,
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
          period,
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
          period,
          SystemAggregate,
          value,
          geometry,
          calculationJob = calculationJob
        )
    }
}
