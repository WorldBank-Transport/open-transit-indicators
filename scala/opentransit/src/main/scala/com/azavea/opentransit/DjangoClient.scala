package com.azavea.opentransit

import com.azavea.opentransit.service.{ IndicatorJob, Scenario }
import com.azavea.opentransit.indicators.IndicatorResultContainer
import com.azavea.opentransit.json._

import spray.client._
import spray.client.pipelining._
import spray.http.HttpMethods._
import spray.http.HttpHeaders._
import spray.http.HttpRequest
import spray.http.HttpResponse
import spray.http.MediaTypes._
import spray.httpx.SprayJsonSupport._
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future
import scala.util.{ Success, Failure }

// Fosters communication between the Spray service and Django.
object DjangoClient {
  // Endpoint URIs
  val BASE_URI = "http://localhost/api"
  val INDICATOR_URI = s"$BASE_URI/indicators/"

  // Execution context for futures
  import system.dispatcher

  // Bring the actor system in scope
  implicit val system = Main.actorSystem

  private val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  def processResponse(response: HttpRequest) {
    pipeline(response).map(_.entity.asString) onComplete {
      case Success(response) => println(response)
      case Failure(error) => println("An error has occured: " + error.getMessage)
    }
  }

  // Send a PATCH to update processing status for indicator calculation job
  def updateIndicatorJob(token: String, indicatorJob: IndicatorJob) = {
    val uri = s"$BASE_URI/indicator-jobs/${indicatorJob.version}/"
    val patch = Patch(uri, indicatorJob) ~> addHeader("Authorization", s"Token $token")
    processResponse(patch)
  }

  // Send a PATCH to update processing status for scenatio creation
  def updateScenario(token: String, scenario: Scenario) = {
    val uri = s"$BASE_URI/scenarios/${scenario.dbName}/"
    val patch = Patch(uri, scenario) ~> addHeader("Authorization", s"Token $token")
    processResponse(patch)
  }

  // Sends a POST request to the indicators endpoint
  def postIndicators(token: String, indicators: Seq[IndicatorResultContainer]) = {
    val post = Post(INDICATOR_URI, indicators.map(_.toJson)) ~> addHeader("Authorization", s"Token $token")
    processResponse(post)
  }
}
