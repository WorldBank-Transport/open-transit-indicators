package com.azavea.opentransit.indicators

import scala.util.{Try, Success, Failure}
import com.azavea.gtfs._
import geotrellis.vector._

// TODO: Move this out of indicators

class SystemGeometries(geomsByRoute: Map[Route, MultiLine], geomsByRouteType: Map[RouteType, MultiLine], geomForSystem: MultiLine) {
  def toTuple = (geomsByRoute, geomsByRouteType, geomForSystem)

  def byRoute(route: Route): MultiLine =
    geomsByRoute.getOrElse(route, MultiLine.EMPTY)

  def byRouteType(routeType: RouteType): MultiLine =
    geomsByRouteType.getOrElse(routeType, MultiLine.EMPTY)

  def bySystem = geomForSystem
}

object SystemGeometries {
  // We always want a MultiLine from a MultiLine.union() call
  implicit def multiLineResultToMultiLine(result: MultiLineMultiLineUnionResult): MultiLine =
    result match {
      case MultiLineResult(ml) => ml
      case LineResult(l) => MultiLine(l)
      case NoResult => MultiLine()
    }

  // TODO: *IMPORTANT* merge in fix for geotrellis' wrapper of JTS so that geometries can be used
  def apply(geomsByRoute: Map[Route, MultiLine], geomsByRouteType: Map[RouteType, MultiLine], geomForSystem: MultiLine): SystemGeometries =
    new SystemGeometries(geomsByRoute, geomsByRouteType, geomForSystem)
  def apply(transitSystem: TransitSystem): SystemGeometries = {
    val byRoute: Map[Route, MultiLine] =
      transitSystem.routes
        .map { route =>
          val lines = List[Line]() // THIS LINE IS *ONLY* ACCEPTABLE AS A PLACEHOLDER UNTIL GEOMETRY CALCULATION THROUGH JTS IS FIXED
           /* route.trips
              .map { trip => trip.tripShape.map(_.line.geom) }
              .flatten*/
          val multiLine: MultiLine = MultiLine(lines).union
          (route, multiLine)
        }.toMap

    def byRouteType: Map[RouteType, MultiLine] =
      byRoute
        .groupBy { case (route, multiLine) => route.routeType }
        .map { case(routeType, seq) =>
          val lines = seq.values.map(_.lines).flatten.toSeq
          (routeType, MultiLine(lines).union: MultiLine)
        }.toMap

    def bySystem: MultiLine =
      MultiLine(byRouteType.values.map(_.lines).flatten.toSeq).union

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
          val lines = multiLines.map(_.lines).flatten
          (route, MultiLine(lines.toSeq).union: MultiLine)
         }

      val mergedRouteTypeGeom =
        byRouteTypes
          .combineMaps
          .map { case(routeType, multiLines) =>
            val lines = multiLines.map(_.lines).flatten
            (routeType, MultiLine(lines.toSeq).union: MultiLine)
           }

      val mergedSystemGeom: MultiLine =
        MultiLine(
          bySystems
            .map(_.lines)
            .flatten
        ).union

      SystemGeometries(mergedRouteGeom, mergedRouteTypeGeom, mergedSystemGeom)
  }
}
