package com.azavea.opentransit.service

import com.azavea.opentransit._

import spray.routing.HttpService

import spray.json._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._

import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Session

import scala.concurrent._

trait MapInfoRoute extends Route { self: DatabaseInstance =>
  // For performing extent queries
  case class Extent(xmin: Double, xmax: Double, ymin: Double, ymax: Double)
  implicit val getExtentResult = GetResult(r => Extent(r.<<, r.<<, r.<<, r.<<))

  // Endpoint for obtaining map info (just extent for now)
  def mapInfoRoute =
    path("map-info") {
      get {
        complete {
          future {
            db withSession { implicit session: Session =>
              // use the stops to find the extent, since they are required
              val q = Q.queryNA[Extent]("""
              SELECT ST_XMIN(ST_Extent(the_geom)) as xmin, ST_XMAX(ST_Extent(the_geom)) as xmax,
                     ST_YMIN(ST_Extent(the_geom)) as ymin, ST_YMAX(ST_Extent(the_geom)) as ymax
              FROM gtfs_stops;
            """)
              val extent = q.list.head

              // construct the extent json, using null if no data is available
              val extentJson = extent match {
                case Extent(0, 0, 0, 0) =>
                  JsNull
                case _ =>
                  JsObject(
                    "southWest" -> JsObject(
                      "lat" -> JsNumber(extent.ymin),
                      "lng" -> JsNumber(extent.xmin)
                    ),
                    "northEast" -> JsObject(
                      "lat" -> JsNumber(extent.ymax),
                      "lng" -> JsNumber(extent.xmax)
                    )
                  )
              }

              // return the map info json
              JsObject("extent" -> extentJson)
            }
          }
        }
      }
    }
}
