package com.azavea.opentransit.service

import com.azavea.opentransit._

import akka.actor._
import spray.util.LoggingContext
import spray.routing.ExceptionHandler
import spray.http.StatusCodes._

import spray.http.StatusCodes.InternalServerError
import spray.routing.{ExceptionHandler, HttpService}

import scala.concurrent._

class OpenTransitServiceActor extends Actor
                                 with HttpService
                                 with IngestRoute 
                                 with IndicatorsRoute
                                 with MapInfoRoute
                                 with ProductionDatabaseInstance {
  // This is the execution context to use for this Actor
  implicit val dispatcher = context.dispatcher

  // The HttpService trait (which GeoTrellisService will extend) defines
  // only one abstract member, which connects the services environment
  // to the enclosing actor or test.
  def actorRefFactory = context

  def receive = runRoute {
    pathPrefix("gt") {
      ingestRoute ~
      indicatorsRoute ~
      mapInfoRoute
    }
  }

  // This will be picked up by the runRoute(_) and used to intercept Exceptions
  implicit def OpenTransitGeoTrellisExceptionHandler(implicit log: LoggingContext) =
    ExceptionHandler {
      case e: Exception =>
        requestUri { uri =>
          // print error message and stack trace to console so we dont have to go a-hunting
          // in the celery job logs
          println(e.getMessage)
          println(e.getStackTrace.mkString("\n"))
          // replace double quotes with single so our message is more json safe
          val jsonMessage = e.getMessage.replace("\"", "'")
          complete(future(InternalServerError, s"""{ "success": false, "message": "${jsonMessage}" }""" ))
        }
    }
}
