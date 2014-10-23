package com.azavea.opentransit.service

import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport
import spray.json._
import spray.routing.{ExceptionHandler, HttpService}
import scala.concurrent._
import spray.util.LoggingContext
import spray.routing._

import scala.util.Success

trait Route extends HttpService with SprayJsonSupport {
  // Required for marshalling futures
  implicit val dispatcher: ExecutionContext

  def successMessage(msg: String) =
    JsObject(
      "success" -> JsBoolean(true),
      "message" -> JsString(msg)
    )

  implicit def myExceptionHandler(implicit log: LoggingContext) =
    ExceptionHandler {
      case e: Exception =>
        requestUri { uri =>
          import spray.json.DefaultJsonProtocol._
          log.warning("Request to {} could not be handled normally", uri)
          complete(StatusCodes.InternalServerError -> JsObject(
            "success" -> JsBoolean(false),
            "message" -> JsString(e.getMessage)
          ))
        }
    }
}
