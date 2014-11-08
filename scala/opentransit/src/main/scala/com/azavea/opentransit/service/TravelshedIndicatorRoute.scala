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
import DefaultJsonProtocol._

import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Session

import scala.concurrent._

trait TravelshedIndicatorRoute extends Route { self: DatabaseInstance =>
  // Endpoint for obtaining map info (just extent for now)
  def travelshedIndicatorRoute =
    path("travelshed") {
      path("jobs") {
        path("breaks") {
          parameters('numBreaks.as[Int]) { (numBreaks) =>
            complete {
              val breaks =
                Main.rasterCache.get(RasterCacheKey(indicators.travelshed.JobsTravelshedIndicator.name)) match {
                  case Some((tile, _)) =>
                    tile
                      .classBreaks(numBreaks)
                  case _ => Array[Int]()
                }
              s"""{ "classBreaks" : ${breaks.mkString("[", ",", "]")} }"""
            }
          }
        } ~
        path("render") {
          parameters(
            'bbox,
            'width.as[Int],
            'height.as[Int],
            'breaks,
            'colorRamp) { (bbox, width, height, breaksString, colorRampKey) =>
            val extent = Extent.fromString(bbox)
            val rasterExtent = RasterExtent(extent, width, height)

            val png: Png = 
              Main.rasterCache.get(RasterCacheKey(indicators.travelshed.JobsTravelshedIndicator.name)) match {
                case Some((tile, extent)) =>
                  val breaks = breaksString.split(",").map(_.toInt)
                  val ramp = {
                    val cr = ColorRampMap.getOrElse(colorRampKey, ColorRamps.BlueToRed)
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
