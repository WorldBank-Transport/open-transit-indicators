package com.azavea.opentransit.service

import com.azavea.gtfs.RouteRecord
import com.azavea.opentransit.service.json.ScenariosGtfsRouteJsonProtocol
import com.azavea.opentransit.util.TestDatabaseFixture
import org.scalatest.{FunSuite, Matchers}
import spray.http.StatusCodes
import spray.testkit.ScalatestRouteTest

class ScenarioGtfsRouteSpec extends FunSuite  with ScalatestRouteTest
  with Matchers
  with ScenarioGtfsRoute
  with TestDatabaseFixture
{
  def actorRefFactory = system
  implicit val dispatcher = actorRefFactory.dispatcher

  import com.azavea.opentransit.service.json.ScenariosGtfsRouteJsonProtocol._

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
