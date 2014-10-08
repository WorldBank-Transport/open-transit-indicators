package com.azavea.opentransit.service

import com.azavea.gtfs.Timer.timedTask

import com.azavea.opentransit._
import com.azavea.opentransit.io.GtfsIngest

import spray.routing._
import spray.routing.HttpService

import spray.json._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._

import scala.concurrent._

trait IngestRoute extends Route { self: DatabaseInstance =>
  implicit val dispatcher: ExecutionContext

  // Endpoint for uploading a GTFS file
  def ingestRoute =
    path("gtfs") {
      post {
        parameter('gtfsDir.as[String]) { gtfsDir =>
          complete {
            var err: JsObject = null
            TaskQueue.execute {
              println(s"parsing GTFS data from: $gtfsDir")
              val routeCount =
                db withSession { implicit session =>
                  try {
                    timedTask("Ingested GTFS") { GtfsIngest(gtfsDir) }
                    } catch {
                      case e: Exception =>
                        println("Error parsing GTFS!")
                        println(e.getMessage)
                        println(e.getStackTrace.mkString("\n"))
                        err = JsObject(
                          "success" -> JsBoolean(false),
                          "message" -> JsString("Error parsing GTFS.\n" + 
                                                e.getMessage.replace("\"", "'"))
                        )
                    }
                }

              if (err == null)
                JsObject(
                  "success" -> JsBoolean(true),
                  "message" -> JsString(s"Imported $routeCount routes")
                )
              else err
            }
          }
        }
      }
    }
}
