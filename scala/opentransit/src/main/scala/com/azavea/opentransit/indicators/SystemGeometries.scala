package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import geotrellis.vector._

// TODO: Move this out of indicators

case class SystemGeometries(byRoute: Map[Route, MultiLine], byRouteType: Map[RouteType, MultiLine], bySystem: MultiLine) {
  def toTuple = (byRoute, byRouteType, bySystem)
}

object SystemGeometries {
  // We always want a MultiLine from a MultiLine.union() call
  implicit def multiLineResultToMultiLine(result: MultiLineMultiLineUnionResult): MultiLine =
    result match {
      case MultiLineResult(ml) => ml
      case LineResult(l) => MultiLine(l)
      case NoResult => MultiLine()
    }

  def apply(transitSystem: TransitSystem): SystemGeometries = {
    val byRoute: Map[Route, MultiLine] =
      transitSystem.routes
        .map { route =>
          val lines =
            route.trips
              .map { trip => trip.tripShape.map(_.line.geom) }
              .flatten
          val multiLine: MultiLine = MultiLine(lines).union
          (route, multiLine)
         }
        .toMap

    val byRouteType: Map[RouteType, MultiLine] =
      byRoute
        .groupBy { case (route, multiLine) => route.routeType }
        .mapValues { seq: Map[Route, MultiLine] =>
          val lines = seq.values.map(_.lines).flatten.toSeq
          MultiLine(lines).union: MultiLine
         }

    val bySystem: MultiLine =
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
        .mapValues { multiLines =>
          val lines = multiLines.map(_.lines).flatten
          MultiLine(lines.toSeq).union: MultiLine
         }

      val mergedRouteTypeGeom =
        byRouteTypes
          .combineMaps
          .mapValues { multiLines =>
            val lines = multiLines.map(_.lines).flatten
            MultiLine(lines.toSeq).union: MultiLine
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
