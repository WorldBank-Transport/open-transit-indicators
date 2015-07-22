package com.azavea.opentransit.service

import com.azavea.opentransit._

import geotrellis.raster.io.geotiff._
import geotrellis.raster.reproject._
import geotrellis.proj4.{LatLng, WebMercator}

import spray.routing._
import spray.http._
import spray.httpx.encoding.Gzip

import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Session

import scala.concurrent._

import java.io._
import java.nio.file._

trait TravelshedGeotiffRoute extends Route { self: DatabaseInstance =>
  // Endpoint for downloading GeoTIFF of jobs travelshed raster.
  // Pulled from raster cache in ARG format, then converted to GeoTIFF.
  def travelshedGeotiffRoute =
    pathPrefix("jobs") {
      path("geotiff") {
        get {
            // INDICATOR parameter is indicator name property
            // (indicators.travelshed.JobsTravelshedIndicator.name for base jobs indicator)
            parameters('JOBID,
                       'INDICATOR) { (jobId, indicatorName) =>
              val geotiffBytes = Main.rasterCache.get(RasterCacheKey(indicatorName + jobId)) match {
                case Some((tile, extent)) => {
                    val (rTile, rExtent) = tile.reproject(extent, WebMercator, LatLng)
                    GeoTiff.render(rTile, rExtent, LatLng, Uncompressed)
                }
                case _ =>
                  Array[Byte]()
                }

              // return 404 if empty
              rejectEmptyResponse {
                encodeResponse(Gzip) {
                  val filename = indicatorName + ".tif"
                  respondWithHeader(HttpHeaders.`Content-Disposition`.apply(
                                    "attachment", Map("filename" -> filename))) {
                    complete {
                      HttpData(geotiffBytes)
                    }
                  }
                }
              }
            }
          }
        }
      }

}
