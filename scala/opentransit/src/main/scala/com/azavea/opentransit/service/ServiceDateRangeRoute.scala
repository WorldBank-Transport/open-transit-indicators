package com.azavea.opentransit.service

import com.azavea.opentransit._

import com.github.nscala_time.time.Imports._
import spray.routing.HttpService

import spray.json._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._

import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Session

import scala.concurrent._

trait ServiceDateRangeRoute extends Route { self: DatabaseInstance =>
  // For performing date range queries
  case class ServiceDateRange(start: String, end: String)
  implicit val getServiceDateRangeResult = GetResult(r => ServiceDateRange(r.<<, r.<<))

  // Endpoint for obtaining range of dates for which loaded feed has service
  def serviceDateRangeRoute =
    path("service-dates") {
      get {
        complete {
          future {
            db withSession { implicit session: Session =>
              try {
                val q = Q.queryNA[ServiceDateRange]("""
                  SELECT MIN(start_date) AS start, MAX(end_date) AS end FROM gtfs_calendar;""")
                val serviceRange = q.list.head

                // construct the json response, using null if no data is available
                val serviceRangeJson = 
                  if (serviceRange.start == null || serviceRange.end == null) JsNull else
                  JsObject(
                    // return dates as ANSI-formatted strings (YYYY-MM-DD)
                    "start" -> JsString(serviceRange.start),
                    "end" -> JsString(serviceRange.end)
                  )
                // return the service date range json
                JsObject("service-dates" -> serviceRangeJson) 
              } catch {
                case e: Exception =>
                        println("Error checking feed service date range!")
                        println(e.getMessage)
                        println(e.getStackTrace.mkString("\n"))
                        // return error instead
                        JsObject(
                          "error" -> JsString("Error getting feed service date range.\n" + 
                                              e.getMessage.replace("\"", "'"))
                        )
              }
            }
          }
        }
      }
    }
}
