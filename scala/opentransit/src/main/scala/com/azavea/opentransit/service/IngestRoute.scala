package com.azavea.opentransit.service

import com.azavea.gtfs._
import com.azavea.gtfs.Timer.timedTask

import com.azavea.opentransit._
import com.azavea.opentransit.io.GtfsIngest

import spray.http.StatusCodes.Accepted
import spray.routing._
import spray.routing.HttpService

import spray.json._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._

import scala.util.{Success, Failure}

import scala.concurrent._

trait IngestRoute extends Route { self: DatabaseInstance =>
  implicit val dispatcher: ExecutionContext

  // Endpoint for uploading a GTFS file
  def ingestRoute =
    path("gtfs") {
      post {
        parameter('gtfsDir.as[String]) { gtfsDir =>
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
              case Success(routeCount: Int) =>
                println("GTFS fully ingested")
              case Failure(e) =>
                println("Error parsing GTFS!")
                println(e.getMessage)
                println(e.getStackTrace.mkString("\n"))
            }

            Accepted -> successMessage("Import started")
          }
        }
      }
    }
}
