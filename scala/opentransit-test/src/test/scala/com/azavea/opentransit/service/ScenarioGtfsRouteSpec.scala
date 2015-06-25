package com.azavea.opentransit.service

import com.azavea.gtfs.{FrequencyRecord, RouteRecord, RouteType, Stop, StopTimeRecord, TripRecord}
import com.azavea.opentransit.service.json.ScenariosGtfsRouteJsonProtocol
import com.azavea.opentransit.testkit.TestGtfsRecords
import com.azavea.opentransit.util.TestDatabaseFixture
import org.scalatest.{FunSuite, Matchers}
import spray.httpx.SprayJsonSupport
import spray.http.StatusCodes
import spray.json._
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
  val root = sealRoute(scenarioGtfsRoute(db))

  test("List the routes") {
    Get("/routes") ~> root ~> check {
      response.status should be (StatusCodes.OK)
      responseAs[List[RouteRecord]].map(_.id) should be (List(
        "EastWest",
        "NorthSouth",
        "WestBus",
        "EastBus",
        "WestRail",
        "EastRail"
      ))
    }
  }

  test("Get a route") {
    Get("/routes/EastWest") ~> root ~> check {
      response.status should be (StatusCodes.OK)
      responseAs[RouteRecord] should be (RouteRecord(
        "EastWest",
        "East West Subway",
        "Subway from EAST STATION to WEST STATION",
        RouteType(1),
        Some("SUBBERS")
      ))
    }
  }

  test("Get an invalid route") {
    Get("/routes/InvalidRoute") ~> root ~> check {
      response.status should be (StatusCodes.NotFound)
    }
  }

  test("List trips for a route") {
    Get("/routes/EastWest/trips") ~> root ~> check {
      response.status should be (StatusCodes.OK)
      responseAs[List[List[String]]] should be (List(List(
        "SUB_WEEKDAY_EVENING_EastWest",
        "SUB_WEEKDAY_AFTERNOON_EastWest",
        "SUB_WEEKDAY_MORNING_EastWest",
        "SUB_SUNDAY_EastWest",
        "SUB_SATURDAY_EastWest"
      )))
    }
  }

  test("List trips for an invalid route") {
    Get("/routes/InvalidRoute/trips") ~> root ~> check {
      response.status should be (StatusCodes.OK)
      responseAs[List[List[String]]] should be (Nil)
    }
  }

  test("Create a route") {
    val newRoute = RouteRecord(
      "NewEastWest",
      "New East West Subway",
      "New Subway from WEST STATION to EAST STATION",
      RouteType(1),
      Some("SUBBERS")
    )

    Post("/routes", newRoute) ~> root ~> check {
      response.status should be (StatusCodes.Created)
    }

    Get("/routes/NewEastWest") ~> root ~> check {
      response.status should be (StatusCodes.OK)
      responseAs[RouteRecord] should be (newRoute)
    }
  }

  test("Delete a route") {
    Get("/routes/NewEastWest") ~> root ~> check {
      response.status should be (StatusCodes.OK)
    }

    Delete("/routes/NewEastWest") ~> root ~> check {
      response.status should be (StatusCodes.OK)
    }

    Get("/routes/NewEastWest") ~> root ~> check {
      response.status should be (StatusCodes.NotFound)
    }
  }

  test("Get a trip") {
    Get("/routes/EastWest/trips/SUB_SUNDAY_EastWest") ~> root ~> check {
      response.status should be (StatusCodes.OK)
      responseAs[JsObject].getFields("tripId") match {
        case Seq(JsString(tripId)) => tripId should be ("SUB_SUNDAY_EastWest")
        case _ => throw new DeserializationException("tripId expected")
      }
    }
  }

  test("Delete a trip") {
    Get("/routes/EastWest/trips/SUB_SUNDAY_EastWest") ~> root ~> check {
      response.status should be (StatusCodes.OK)
    }
    Delete("/routes/EastWest/trips/SUB_SUNDAY_EastWest") ~> root ~> check {
      response.status should be (StatusCodes.OK)
    }
    Get("/routes/EastWest/trips/SUB_SUNDAY_EastWest") ~> root ~> check {
      response.status should be (StatusCodes.NotFound)
    }
  }

  test("Create a trip pattern") {
    Get("/routes/EastWest/trips/NewTrip") ~> root ~> check {
      response.status should be (StatusCodes.NotFound)
    }

    val records = TestGtfsRecords()
    val tripRecord = TripRecord(
      "SUB_WEEKDAY_MORNING_EastWest",
      "SUB_WEEKDAYS",
      "EastWest",
      None,
      Some("SUB_EastWest_SHAPE")
    )

    val tripTuple = TripTuple(
      tripRecord,
      records.stopTimeRecords.take(2).map(st => (st, records.stops.head)),
      records.frequencyRecords,
      Some(records.tripShapes.head)
    )

    Post("/routes/EastWest/trips/SUB_WEEKDAY_MORNING_EastWest", tripTuple) ~> root ~> check {
      response.status should be (StatusCodes.Created)
    }

    Get("/routes/EastWest/trips") ~> root ~> check {
      response.status should be (StatusCodes.OK)
      responseAs[List[List[String]]] should be (List(List(
        "SUB_WEEKDAY_MORNING_EastWest"
      )))
    }
  }

  test("Delete a route and its associated trips") {
    Get("/routes/EastWest/trips/SUB_WEEKDAY_MORNING_EastWest") ~> root ~> check {
      response.status should be (StatusCodes.OK)
    }

    Delete("/routes/EastWest") ~> root ~> check {
      response.status should be (StatusCodes.OK)
    }

    Get("/routes/EastWest/trips/SUB_WEEKDAY_MORNING_EastWest") ~> root ~> check {
      response.status should be (StatusCodes.NotFound)
    }
  }
}
