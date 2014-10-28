package com.azavea.opentransit.service

import com.azavea.gtfs._
import com.azavea.gtfs.Timer.timedTask

import com.azavea.opentransit._
import com.azavea.opentransit.JobStatus
import com.azavea.opentransit.JobStatus._
import com.azavea.opentransit.io.GtfsIngest

import spray.http.StatusCodes.Accepted
import spray.routing._
import spray.routing.HttpService

import spray.json._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._

import scala.util.{Success, Failure}

import scala.concurrent._

case class GtfsFeed(
  id: Int = 0,
  jobStatus: JobStatus
)

trait IngestRoute extends Route { self: DatabaseInstance with DjangoClientComponent =>
  // Endpoint for uploading a GTFS file
  def ingestRoute =
    path("gtfs") {
      post {
        parameters('gtfsDir.as[String], 'token.as[String], 'id.as[Int]) { (gtfsDir, token, id) =>
          complete {
            TaskQueue.execute {
              println(s"parsing GTFS data from: $gtfsDir")
              db withSession { implicit session =>
                timedTask("Ingested GTFS") {
                  val records = GtfsRecords.fromFiles(gtfsDir)
                  GtfsIngest(records)
                }
              }
            }.onComplete {
              case Success(_) =>
                djangoClient.updateGtfsFeed(token, GtfsFeed(id, JobStatus.Complete))
              case Failure(e) =>
                println("Error parsing GTFS!")
                println(e.getMessage)
                println(e.getStackTrace.mkString("\n"))
                djangoClient.updateGtfsFeed(token, GtfsFeed(id, JobStatus.Failed))
            }

            Accepted -> successMessage("Import started")
          }
        }
      }
    }
}
