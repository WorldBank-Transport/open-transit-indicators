package com.azavea.opentransit.service

import com.azavea.opentransit._

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.raster.render._
import geotrellis.raster.stats._

import spray.routing._

import spray.json._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._

import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Session

import scala.concurrent._

trait TravelshedMinMaxRoute extends Route { self: DatabaseInstance =>

  def travelshedMinMaxRoute =
    pathPrefix("jobs") {
      path("minmax") {
        get {
            parameters(
              'JOBID
            ) { (jobId) =>
              complete {
                println(jobId)


                val (min, max) =
                  Main.rasterCache.get(RasterCacheKey(indicators.travelshed.JobsTravelshedIndicator.name + jobId)) match {
                    case Some((tile, extent)) =>
                      tile.findMinMax
                    case _ =>
                      (0,0)
                  }
                JsObject("min" -> JsNumber(min), "max" -> JsNumber(max))
            }
          }
        }
      }
    }
}

