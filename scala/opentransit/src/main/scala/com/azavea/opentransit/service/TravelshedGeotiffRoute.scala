package com.azavea.opentransit.service

import com.azavea.opentransit._

import geotrellis.raster.io.geotiff.GeoTiffWriter
import geotrellis.proj4.LatLng

import spray.routing._
import spray.http._
import spray.httpx.encoding.Gzip

import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Session

import scala.concurrent._

import java.io._
import java.nio.file._

trait TravelshedGeotiffRoute extends Route { self: DatabaseInstance =>
  final val directoryName = "indicator-geotiffs"

  // Endpoint for downloading GeoTIFF of jobs travelshed raster.
  // Pulled from raster cache in ARG format, then converted to GeoTIFF.
  def travelshedGeotiffRoute =
    pathPrefix("jobs") {
      path("geotiff") {
        get {
            parameters('JOBID) { (jobId) =>
              // create geotiff directory if it does not exist
              val directory = new java.io.File(directoryName)
              if(!directory.exists) {
                directory.mkdirs
              } else if(!directory.isDirectory) {
                sys.error(s"Indicator geotiff path ${directory.getAbsolutePath} exists but is not a directory.")
              }
              val pathStr = new java.io.File(directory, "jobs_travelshed.tif").getPath()
              val geotiffPath = Paths.get(pathStr)
              // only have one job travelshed geotiff in directory at a time
              Files.deleteIfExists(geotiffPath)
              Main.rasterCache.get(RasterCacheKey(indicators.travelshed.JobsTravelshedIndicator.name + jobId)) match {
                case Some((tile, extent)) =>
                    GeoTiffWriter.write(pathStr, tile, extent, LatLng)
                case _ =>
                  Files.write(geotiffPath, Array[Byte]())
                }

              encodeResponse(Gzip) {
                getFromFile(pathStr)
              }
            }
          }
        }
      }

}
