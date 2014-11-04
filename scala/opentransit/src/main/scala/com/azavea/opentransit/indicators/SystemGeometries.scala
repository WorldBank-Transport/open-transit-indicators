package com.azavea.opentransit.indicators

import scala.util.{Try, Success, Failure}
import com.azavea.gtfs._
import com.vividsolutions.jts.io.WKBWriter;
import geotrellis.slick._
import geotrellis.vector._
import geotrellis.vector.json._
import geotrellis.vector.reproject._
import geotrellis.proj4._
import spray.json._

trait SystemGeometries {
  type G <: Geometry

  // For writing Well Known Binary
  lazy val wkbWriter = new WKBWriter
  def toWkb(g: G) = {
    JsString(WKBWriter.toHex(wkbWriter.write(g.jtsGeom)))
  }

  def byRoute(route: Route): Option[G]
  def byRouteType(routeType: RouteType): Option[G]
  def bySystem: G

  val byRouteWkb: Map[Route, JsValue]
  val byRouteTypeWkb: Map[RouteType, JsValue]
  val bySystemWkb: JsValue

}

class SystemLineGeometries (geomsByRoute: Map[Route, MultiLine], geomsByRouteType: Map[RouteType, MultiLine], geomForSystem: MultiLine) extends SystemGeometries {
  type G = MultiLine

  def toTuple = (geomsByRoute, geomsByRouteType, geomForSystem)

  def byRoute(route: Route): Option[MultiLine] =
    geomsByRoute.get(route)

  def byRouteType(routeType: RouteType): Option[MultiLine] =
    geomsByRouteType.get(routeType)

  def bySystem: MultiLine =
    geomForSystem

  // Memoize the Json serialization so it only happens once per instance.
  lazy val byRouteWkb: Map[Route, JsValue] =
    Timer.timedTask("Created byRoute Wkb") {
      geomsByRoute.map { case (route, g) => (route, toWkb(g))}.toMap
    }

  lazy val byRouteTypeWkb: Map[RouteType, JsValue] =
    Timer.timedTask("Created byRouteType Wkb") {
      geomsByRouteType.map { case (routeType, g) => (routeType, toWkb(g))}.toMap
    }

  lazy val bySystemWkb: JsValue =
    Timer.timedTask("Created bySystem Wkb") {
      toWkb(geomForSystem)
    }

}

object SystemLineGeometries {
  /** Find the transform for a transit system to LatLng. Creating the transform only once speeds things up a lot. */
  private def findTransform(transitSystem: TransitSystem): Transform = {
    val srid =
      (for(
        route <- transitSystem.routes.headOption;
        trip <- route.trips.headOption;
        tripShape <- trip.tripShape
      ) yield { tripShape.line.srid }) match {
        case Some(i) => i
        case None => sys.error(s"Transit system is required to have an SRID")
      }

    val crs = CRS.fromName(s"EPSG:${srid}")
    Transform(crs, LatLng)
  }

  def apply(transitSystem: TransitSystem): SystemLineGeometries = {
    val transform = findTransform(transitSystem)

    val byRoute: Map[Route, MultiLine] =
      transitSystem.routes
        .map { route =>
          val lines =
            route.trips
              .map { trip => trip.tripShape.map(_.line.geom.reproject(transform)) }
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

    new SystemLineGeometries(byRoute, byRouteType, bySystem)
  }

  def merge(geometries: Seq[SystemLineGeometries]): SystemLineGeometries = {
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
        bySystems.map(_.lines)
          .flatten
          .dissolve

    new SystemLineGeometries(mergedRouteGeom, mergedRouteTypeGeom, mergedSystemGeom)
  }
}

class SystemBufferGeometries (geomForSystem: MultiPolygon) extends SystemGeometries {
  type G = MultiPolygon

  def byRoute(route: Route): Option[MultiPolygon] =
    None

  def byRouteType(routeType: RouteType): Option[MultiPolygon] =
    None

  def bySystem: MultiPolygon =
    geomForSystem

  // Memoize the Json serialization so it only happens once per instance.
  lazy val byRouteWkb: Map[Route, JsValue] = Map()

  lazy val byRouteTypeWkb: Map[RouteType, JsValue] = Map()

  lazy val bySystemWkb: JsValue =
    Timer.timedTask("Created bySystem Wkb") {
      toWkb(geomForSystem)
    }
}

object SystemBufferGeometries {
  /** Find the transform for a transit system to LatLng. Creating the transform only once speeds things up a lot. */
   // TODO: Find a way to avoid copy-pasting this while still making sure it's calculated
   // only once and is still private to SystemGeometries
  private def findTransform(transitSystem: TransitSystem): Transform = {
    val srid =
      (for(
        route <- transitSystem.routes.headOption;
        trip <- route.trips.headOption;
        tripShape <- trip.tripShape
      ) yield { tripShape.line.srid }) match {
        case Some(i) => i
        case None => sys.error(s"Transit system is required to have an SRID")
      }

    val crs = CRS.fromName(s"EPSG:${srid}")
    Transform(crs, LatLng)
  }

  def apply(transitSystem: TransitSystem, buffer: Projected[MultiPolygon]): SystemBufferGeometries = {
    val transform = findTransform(transitSystem)
    val systemBufferGeom = Projected(buffer.geom.reproject(transform), 4326)
    new SystemBufferGeometries(systemBufferGeom)
  }

  def merge(geometries: Seq[SystemBufferGeometries]): SystemBufferGeometries = {
    val union = geometries.map(_.bySystem).unioned
    val mergedBufferGeom = union match {
      case PolygonResult(p) => MultiPolygon(p)
      case MultiPolygonResult(mp) => mp
    }
    new SystemBufferGeometries(mergedBufferGeom)
  }
}
