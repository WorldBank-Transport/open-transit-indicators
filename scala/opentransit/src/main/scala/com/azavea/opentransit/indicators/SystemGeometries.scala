package com.azavea.opentransit.indicators

import scala.util.{Try, Success, Failure}
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
        }.toMap

    val byRouteType: Map[RouteType, MultiLine] =
      Try(byRoute
        .groupBy { case (route, multiLine) => route.routeType }
        .map { case(routeType, seq) =>
          val lines = seq.values.map(_.lines).flatten.toSeq
          (routeType, MultiLine(lines).union: MultiLine)
        }.toMap) match {
          case Success(brt) => brt
          case Failure(e) =>
            println(s"Failure in byRouteType: $e")
            Map()
        }

    val bySystem: MultiLine =
      Try(MultiLine(byRouteType.values.map(_.lines).flatten.toSeq).union) match {
        case Success(bs) => bs
        case Failure(e) =>
          println(s"Failure in bySystem: $e")
          MultiLine.EMPTY
      }

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
