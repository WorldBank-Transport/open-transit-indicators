package com.azavea.opentransit.service

import com.azavea.opentransit._
import com.azavea.opentransit.JobStatus
import com.azavea.opentransit.JobStatus._
import com.azavea.opentransit.json._
import com.azavea.opentransit.scenarios._

import spray.http.MediaTypes
import spray.http.StatusCodes.{Created, InternalServerError}
import spray.routing.{ExceptionHandler, HttpService}
import spray.util.LoggingContext
import scala.concurrent._

import spray.json._
import spray.httpx.SprayJsonSupport
import SprayJsonSupport._
import DefaultJsonProtocol._

case class Scenario(
  dbName: String = "",
  jobStatus: JobStatus
)

trait ScenariosRoute extends Route { self: DatabaseInstance =>
  // Endpoint for creating a new scenario
  def scenariosRoute = {
    path("scenarios") {
      post {
        entity(as[ScenarioCreationRequest]) { request =>
          complete {
            var err: JsObject = null
            TaskQueue.execute {
              try {
                println(s"Creating scenario with dbName: ${request.dbName}")
                CreateScenario(request, dbByName) { status =>
                  DjangoClient.updateScenario(request.token, Scenario(request.dbName, status))
                }
                println("Finished creating scenario")
              } catch {
              case e: Exception =>
                println("Error creating scenario!")
                println(e.getMessage)
                println(e.getStackTrace.mkString("\n"))
                err = JsObject(
                  "success" -> JsBoolean(false),
                  "message" -> JsString(s"Couldn't create scenario: ${e.getMessage}")
                )

                try {
                  DjangoClient.updateScenario(request.token,
                    Scenario(request.dbName, JobStatus.Failed))
                } catch {
                  case ex: Exception =>
                    println("Failed to update scenario")
                }
              }
            }

            if (err == null) {
              Created -> JsObject(
                "success" -> JsBoolean(true),
                "message" -> JsString(s"Scenario creation started")
              )
            } else {
              err
            }
          }
        }
      }
    }
  }
}
