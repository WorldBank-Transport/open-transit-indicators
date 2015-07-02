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
  def minMaxForTravelShed(indicator: String, jobId: String) = {
    val (min, max) = Main.rasterCache.get(RasterCacheKey(indicator + jobId)) match {
      case Some((tile, extent)) => tile.findMinMax
      case _ => (0,0)
    }
    JsObject("min" -> JsNumber(min), "max" -> JsNumber(max))
  }

  def jobsTravelshedMinMaxRoute =
    pathPrefix("jobs") {
      path("minmax") {
        get {
            parameters(
              'JOBID
            ) { (jobId) =>
              complete {
                minMaxForTravelShed(indicators.travelshed.JobsTravelshedIndicator.name, jobId)
            }
          }
        }
      }
    }

  def absoluteJobsMinMaxRoute =
    pathPrefix("abs-jobs") {
      path("minmax") {
        get {
            parameters(
              'JOBID
            ) { (jobId) =>
              complete {
                minMaxForTravelShed(indicators.travelshed.JobsTravelshedIndicator.absoluteName, jobId)
            }
          }
        }
      }
    }

  def percentageJobsMinMaxRoute =
    pathPrefix("pct-jobs") {
      path("minmax") {
        get {
            parameters(
              'JOBID
            ) { (jobId) =>
              complete {
                minMaxForTravelShed(indicators.travelshed.JobsTravelshedIndicator.percentageName, jobId)
            }
          }
        }
      }
    }
}

