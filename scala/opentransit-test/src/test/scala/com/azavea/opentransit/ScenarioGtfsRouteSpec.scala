package com.azavea.opentransit

import com.azavea.gtfs.RouteRecord
import com.azavea.opentransit.service.ScenarioGtfsRoute
import org.scalatest.{FunSuite, Outcome, Matchers, FunSpec}
import spray.http.StatusCodes
import spray.json._
import spray.testkit.ScalatestRouteTest
import com.azavea.opentransit.service.json.ScenariosGtfsRouteJsonProtocol

class ScenarioGtfsRouteSpec extends FunSuite  with ScalatestRouteTest
  with Matchers
  with ScenarioGtfsRoute
  with TestDatabaseFixture
{
  def actorRefFactory = system
  implicit val dispatcher = actorRefFactory.dispatcher

  import ScenariosGtfsRouteJsonProtocol._
  import spray.httpx.SprayJsonSupport._

  // we're just going to run the tests against the main DB, as they should be equivalent
  val root = scenarioGtfsRoute(db)

  test("lists the routes") {
    Get("/routes") ~> root ~> check {
      responseAs[List[RouteRecord]] should not be (empty)
      response.status === StatusCodes.OK
    }
  }

  test("group trips in route by trip path") {
    Get("/routes/EastWest/trips") ~> root ~> check {
      response.status === StatusCodes.OK
      val tripGroups = responseAs[List[List[String]]]
      tripGroups should not be (empty)
      tripGroups.head should contain("SUB_WEEKDAY_EVENING_EastWest")
    }
  }

}
