package com.azavea.opentransit.service

import com.azavea.opentransit._
import com.azavea.opentransit.JobStatus
import com.azavea.opentransit.JobStatus._
import com.azavea.opentransit.json._
import com.azavea.opentransit.scenarios._
import grizzled.slf4j.Logging

import spray.http.MediaTypes
import spray.http.StatusCodes
import spray.routing.{ExceptionHandler, HttpService}
import spray.util.LoggingContext
import scala.concurrent._

import spray.json._
import spray.httpx.SprayJsonSupport
import SprayJsonSupport._
import DefaultJsonProtocol._

import scala.util.{Failure, Success}

case class Scenario(
  dbName: String = "",
  jobStatus: JobStatus
)

trait ScenarioRoute extends Route with ScenarioGtfsRoute  with Logging { self: DatabaseInstance =>
  // Endpoint for creating a new scenario
  def scenariosRoute = {
    /** Create Scenario DB and prepare it's transactions */
    pathEnd {
      post {
        entity(as[ScenarioCreationRequest]) { request =>
          complete {
            TaskQueue.execute {
              CreateScenario(request, dbByName)
            }.onComplete {
              case Success(_) =>
                DjangoClient.updateScenario(request.token, Scenario(request.dbName, JobStatus.Complete))
              case Failure(ex) =>
                DjangoClient.updateScenario(request.token, Scenario(request.dbName, JobStatus.Failed))
            }

            StatusCodes.Accepted -> successMessage("Scenario creation started.")
          }
        }
      }
    } ~
    pathPrefix(Segment)  { scenarioId =>
      if (scenarioId == "transit_indicators")
        complete(StatusCodes.InternalServerError -> "This database is not for you")
      else {
        val scenarioDB = dbByName(scenarioId)
        scenarioGtfsRoute(scenarioDB)
      }
    }
  }
}
