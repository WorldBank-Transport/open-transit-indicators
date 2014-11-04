package com.azavea.opentransit

import com.azavea.opentransit.service.{GtfsFeed, IndicatorJob, Scenario}
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

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{ Success, Failure }


trait DjangoClientComponent {
  val djangoClient: DjangoClient
}

trait DjangoClient {
  def processResponse(request: HttpRequest)

  // Amount of time to wait in between requests
  val THROTTLE_INDICATORS_MILLIS = 5000
  val THROTTLE_INDICATORJOBS_MILLIS = 500

  // Endpoint URIs
  val BASE_URI = "http://localhost/api"
  val INDICATOR_URI = s"$BASE_URI/indicators/"

  /** Send a request with an authorization header */
  def sendRequest(token: String, request: HttpRequest) =
    processResponse(request ~> addHeader("Authorization", s"Token $token"))

  /** Send a PATCH to update processing status for indicator calculation job */
  def updateIndicatorJob(token: String, indicatorJob: IndicatorJob) = {
    // Sleep between updates to help prevent Django timeouts
    Thread.sleep(THROTTLE_INDICATORJOBS_MILLIS)
    sendRequest(token, Patch(s"$BASE_URI/indicator-jobs/${indicatorJob.id}/", indicatorJob))
  }

  /** Send a PATCH to update processing status for scenario creation */
  def updateScenario(token: String, scenario: Scenario) =
    sendRequest(token, Patch(s"$BASE_URI/scenarios/${scenario.dbName}/", scenario))

  /** Send a PATCH to update processing status for GTFS feed */
  def updateGtfsFeed(token: String, gtfsFeed: GtfsFeed) = {
    sendRequest(token, Patch(s"$BASE_URI/gtfs-feeds/${gtfsFeed.id}/", gtfsFeed))
  }

  /** Sends a POST request to the indicators endpoint */
  def postIndicators(token: String, indicators: Seq[IndicatorResultContainer]) = {
    // Sleep between updates to help prevent Django timeouts
    Thread.sleep(THROTTLE_INDICATORS_MILLIS)
    sendRequest(token, Post(INDICATOR_URI, indicators.map(_.toJson)))
  }
}

// Fosters communication between the Spray service and Django.
trait ProductionDjangoClient extends DjangoClient{
  // Execution context for futures
  import system.dispatcher

  // Bring the actor system in scope
  implicit val system = Main.actorSystem

  private val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  def processResponse(request: HttpRequest) {
    pipeline(request).map(_.entity.asString) onComplete {
      case Success(response) => println(response)
      case Failure(error) => println("An error has occured: " + error.getMessage)
    }
  }
}
