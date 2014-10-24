package com.azavea.opentransit.indicators

import scala.util.{Try, Success, Failure}
import com.azavea.gtfs._
import geotrellis.vector._
import geotrellis.vector.json._
import spray.json._

// TODO: Move this out of indicators

class SystemGeometries(geomsByRoute: Map[Route, MultiLine], geomsByRouteType: Map[RouteType, MultiLine], geomForSystem: MultiLine) {
  def toTuple = (geomsByRoute, geomsByRouteType, geomForSystem)

  def byRoute(route: Route): MultiLine =
    geomsByRoute.getOrElse(route, MultiLine.EMPTY)

  def byRouteType(routeType: RouteType): MultiLine =
    geomsByRouteType.getOrElse(routeType, MultiLine.EMPTY)

  def bySystem = geomForSystem

  // Memoize the Json serialization so it only happens once per instance.
  lazy val byRouteGeoJson: Map[Route, JsValue] = 
    Timer.timedTask("Created byRoute GeoJson") {
      geomsByRoute.map { case (route, ml) => (route, ml.toJson) }.toMap
    }

  lazy val byRouteTypeGeoJson: Map[RouteType, JsValue] = 
    Timer.timedTask("Created byRouteType GeoJson") {
      geomsByRouteType.map { case (routeType, ml) => (routeType, ml.toJson) }.toMap
    }

  lazy val bySystemGeoJson: JsValue =
    Timer.timedTask("Created bySystem GeoJson") {
      geomForSystem.toJson
    }
}

object SystemGeometries {
  def apply(geomsByRoute: Map[Route, MultiLine], geomsByRouteType: Map[RouteType, MultiLine], geomForSystem: MultiLine): SystemGeometries =
    new SystemGeometries(geomsByRoute, geomsByRouteType, geomForSystem)

  def apply(transitSystem: TransitSystem): SystemGeometries = {
    val byRoute: Map[Route, MultiLine] =
      transitSystem.routes
        .map { route =>
          val lines = 
            route.trips
              .map { trip => trip.tripShape.map(_.line.geom) }
              .flatten
              .dissolve

          (route, MultiLine(lines))
        }.toMap

    def byRouteType: Map[RouteType, MultiLine] =
      byRoute
        .groupBy { case (route, multiLine) => route.routeType }
        .map { case(routeType, seq) =>
          val lines = seq.values.map(_.lines).flatten.dissolve
          (routeType, MultiLine(lines))
        }.toMap

    def bySystem: MultiLine =
      MultiLine(byRouteType.values.map(_.lines).flatten.dissolve)

    SystemGeometries(byRoute, byRouteType, bySystem)
  }

  def merge(geometries: Seq[SystemGeometries]): SystemGeometries = {
    val (byRoutes, byRouteTypes, bySystems) =
      geometries
        .map(_.toTuple)
        .transposeTuples

    val mergedRouteGeom =
      byRoutes
        .combineMaps
        .map { case(route, multiLines) =>
          val lines = multiLines.map(_.lines).flatten.dissolve
          (route, MultiLine(lines))
         }

      val mergedRouteTypeGeom =
        byRouteTypes
          .combineMaps
          .map { case(routeType, multiLines) =>
            val lines = multiLines.map(_.lines).flatten.dissolve
            (routeType, MultiLine(lines))
           }

      val mergedSystemGeom: MultiLine =
        MultiLine(
          bySystems
            .map(_.lines)
            .flatten
            .dissolve
        )

      SystemGeometries(mergedRouteGeom, mergedRouteTypeGeom, mergedSystemGeom)
  }
}
