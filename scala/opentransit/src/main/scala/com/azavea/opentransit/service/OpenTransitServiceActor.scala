package com.azavea.opentransit.service

import com.azavea.opentransit._

import akka.actor._
import spray.util.LoggingContext
import spray.routing.ExceptionHandler
import spray.http.{HttpResponse, HttpRequest, Timedout}

import spray.http.StatusCodes.InternalServerError
import spray.routing.{ExceptionHandler, HttpService}

import scala.concurrent._

class OpenTransitServiceActor extends Actor
                                 with HttpService
                                 with IngestRoute
                                 with IndicatorsRoute
                                 with ScenarioRoute
                                 with ScenarioGtfsRoute
                                 with MapInfoRoute
                                 with ServiceDateRangeRoute
                                 with ProductionDatabaseInstance {
  // This is the execution context to use for this Actor
  implicit val dispatcher = context.dispatcher

  // The HttpService trait (which GeoTrellisService will extend) defines
  // only one abstract member, which connects the services environment
  // to the enclosing actor or test.
  def actorRefFactory = context

  // This will be picked up by the runRoute(_) and used to intercept Exceptions
  implicit def OpenTransitGeoTrellisExceptionHandler(implicit log: LoggingContext) =
    ExceptionHandler {
      case e: Exception =>
        requestUri { uri =>
          // print error message and stack trace to console so we dont have to go a-hunting
          // in the celery job logs
          println("In OpenTransitGeoTrellisExceptionHandler:")
          println(e.getMessage)
          println(e.getStackTrace.mkString("\n"))
          // replace double quotes with single so our message is more json safe
          val jsonMessage = e.getMessage.replace("\"", "'")
          complete(future(InternalServerError, s"""{ "success": false, "message": "${jsonMessage}" }""" ))
        }
    }

  def receive = handleTimeouts orElse runRoute {
    pathPrefix("gt") {
      pathPrefix("utils") {
        ingestRoute ~
        mapInfoRoute ~
        serviceDateRangeRoute
      } ~
      pathPrefix("indicators") {
        indicatorsRoute
      } ~
      pathPrefix("scenarios") {
        scenariosRoute
      }
    }
  }

  // timeout handling, from here:
  // http://spray.io/documentation/1.1-SNAPSHOT/spray-routing/key-concepts/timeout-handling/
  // return JSON message instead of default string message:
  // The server was not able to produce a timely response to your request.
  def handleTimeouts: Receive = {
    case Timedout(x: HttpRequest) =>
      sender ! HttpResponse(InternalServerError,
                            """{ "success": false, "message": "Spray timeout encountered" }""")
  }
}

