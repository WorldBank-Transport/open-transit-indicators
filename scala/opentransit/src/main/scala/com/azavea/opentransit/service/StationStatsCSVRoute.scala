package com.azavea.opentransit.service

import com.azavea.opentransit._
import com.azavea.opentransit.database._

import geotrellis.raster.io.geotiff._
import geotrellis.proj4.LatLng

import spray.routing._
import spray.http._
import spray.httpx.encoding.Gzip

import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Session

import scala.concurrent._

trait StationStatsCSVRoute extends Route { self: DatabaseInstance =>
  // Endpoint for downloading station statistics CSV
  def stationStatsCSVRoute =
    path("station-csv") {
      get {
        parameters('gtfsdb) { gtfsDbName =>
          encodeResponse(Gzip) {
            rejectEmptyResponse {  // 404 just in case None
              StationCSVDatabase.get(gtfsDbName) match {
                case Some(csvJob) => {
                  csvJob.status match {
                    case Success =>
                      val filename = s"bufferD${csvJob.bufferDistance}commuteT${csvJob.commuteTime}stats.csv"
                        respondWithHeader(HttpHeaders.`Content-Disposition`.apply(
                          "attachment", Map("filename" -> filename))) {
                          complete {
                            HttpData(csvJob.data)
                          }
                        }
                    case _ => complete(StatusCodes.NotFound)
                  }
                }
                case _ => complete(StatusCodes.NotFound)
              }
            }
          }
        }
      }
    }
}
