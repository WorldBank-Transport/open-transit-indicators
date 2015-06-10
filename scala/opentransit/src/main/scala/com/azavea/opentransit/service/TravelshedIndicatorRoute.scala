package com.azavea.opentransit.service

import com.azavea.opentransit._

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.raster.render._
import geotrellis.raster.stats._

import spray.routing._
import spray.http._

import spray.json._
import spray.httpx.SprayJsonSupport._

import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Session

import scala.concurrent._

trait TravelshedIndicatorRoute extends Route { self: DatabaseInstance =>
  final val numberOfClassBreaks = 10

  def travelshedIndicatorRoute =
    pathPrefix("jobs") {
      path("render") {
        get {
          parameters(
            'INDICATOR,
            'JOBID,
            'BBOX,
            'WIDTH.as[Int],
            'HEIGHT.as[Int]) { (indicatorName, jobId, bbox, width, height) =>

            val requestExtent = Extent.fromString(bbox)
            val rasterExtent = RasterExtent(requestExtent, width, height)

            val png: Png =
              Main.rasterCache.get(RasterCacheKey(indicatorName + jobId)) match {
                case Some((tile, extent)) =>
                  val breaks = tile.classBreaks(numberOfClassBreaks)
                  val ramp = {
                    val cr = ColorRamps.LightToDarkGreen
                    if(cr.toArray.length < breaks.length) { cr.interpolate(breaks.length) }
                    else { cr }
                  }

                  tile
                    .warp(extent, rasterExtent)
                    .renderPng(ramp, breaks)
                case _ =>
                  ArrayTile.empty(TypeByte, width, height).renderPng
              }

            respondWithMediaType(MediaTypes.`image/png`) {
              complete(png.bytes)
            }
          }
        }
      }
    }
}
