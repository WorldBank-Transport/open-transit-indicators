package com.azavea.opentransit.service

import com.azavea.opentransit._
import com.azavea.opentransit.io.GtfsIngest

import spray.routing.HttpService

import spray.json._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._

import scala.concurrent._

trait IngestRoute extends Route { self: DatabaseInstance =>
  implicit val dispatcher: ExecutionContext
  
  // time how long each step takes.  from here:
  // http://stackoverflow.com/questions/9160001/how-to-profile-methods-in-scala
  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    println("Total elapsed time: " + ((t1 - t0) / 1000000000.0) + " s")
    result
  }

  // Endpoint for uploading a GTFS file
  def ingestRoute =
    path("gtfs") {
      post {
        parameter('gtfsDir.as[String]) { gtfsDir =>
          complete {
            TaskQueue.execute {
              println(s"parsing GTFS data from: $gtfsDir")
              val routeCount =
                db withSession { implicit session =>
                  time { GtfsIngest(gtfsDir) }
                }

              JsObject(
                "success" -> JsBoolean(true),
                "message" -> JsString(s"Imported $routeCount routes")
              )
            }
          }
        }
      }
    }
}
