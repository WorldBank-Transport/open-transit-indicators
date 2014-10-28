package com.azavea.opentransit.service

import com.azavea.gtfs.RouteRecord
import com.azavea.opentransit.DjangoClientComponent
import com.azavea.opentransit.indicators.SamplePeriod
import com.azavea.opentransit.scenarios.ScenarioCreationRequest
import com.azavea.opentransit.service.json.ScenariosGtfsRouteJsonProtocol
import com.azavea.opentransit.util.{MockDjangoClient, TestDatabaseFixture}
import org.joda.time.LocalDateTime
import org.scalatest.{FunSuite, Matchers}
import spray.http.StatusCodes
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Await
import scala.concurrent.duration._

class ScenarioRouteSpec extends FunSuite
  with ScalatestRouteTest
  with Matchers
  with ScenarioRoute
  with TestDatabaseFixture
  with DjangoClientComponent
{
  val djangoClient = new MockDjangoClient {}

  def actorRefFactory = system
  implicit val dispatcher = actorRefFactory.dispatcher

  import com.azavea.opentransit.json._
  import com.azavea.opentransit.service.json.ScenariosGtfsRouteJsonProtocol._

  // pick the route we want and seal it so the error handling kicks in
  def root = sealRoute(
    pathPrefix("scenarios") {
      scenariosRoute
    }
  )

  test("Can create scenario in response to request") {
    val request = ScenarioCreationRequest("token", "TEST", mainDbName,
      SamplePeriod(1, "night",
        new LocalDateTime("2014-05-01T00:00:00.000"),
        new LocalDateTime("2014-05-01T08:00:00.000")
      )
    )

    // register for request before doing things to trigger it
    val djangoRequestFuture = djangoClient.nextRequest

    Post("/scenarios", request) ~> root ~> check {
      response.status should equal (StatusCodes.Accepted)
    }

    // block to wait for the request to come, time-out will throw
    val djangoRequest = Await.result(djangoRequestFuture, 10 seconds)

    // can inspect the request here
    println("DJANGO REQUEST", djangoRequest.entity)

    // maybe validate it some too
    djangoRequest.entity.asString should include ("complete")

  }

  test("Get stuff out of the new scenario") {
    Get("/scenarios/TEST/routes") ~> root ~> check {
      response.status should equal (StatusCodes.OK)
      responseAs[List[RouteRecord]] should not be (empty)
    }
  }
}
