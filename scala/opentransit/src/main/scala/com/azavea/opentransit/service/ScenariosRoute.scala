package com.azavea.opentransit.service

import com.azavea.gtfs.io.database.{DatabaseRecordImport, DefaultProfile, GtfsTables}
import com.azavea.gtfs.op.GroupTrips
import com.azavea.gtfs.op.GroupTrips.TripTuple
import com.azavea.gtfs.{GtfsRecords, StopTimeRecord, FrequencyRecord, TripRecord}
import com.azavea.opentransit.DatabaseInstance
import com.azavea.opentransit.indicators.IndicatorCalculationRequest
import com.sun.xml.internal.ws.encoding.soap.DeserializationException
import org.joda.time._
import org.joda.time.format.PeriodFormatterBuilder
import spray.http._
import spray.http.HttpHeaders._
import spray.http.HttpMethods._
import spray.json.DefaultJsonProtocol
import spray.routing.PathMatchers.Segment
import spray.routing._
import spray.json._
import spray.httpx.SprayJsonSupport
import SprayJsonSupport._


object ScenariosJsonProtocol extends DefaultJsonProtocol {
  import DefaultJsonProtocol._

  case class TripPattern(trip: TripRecord, stopTimes: Seq[StopTimeRecord], frequencies: Seq[FrequencyRecord])

  implicit val periodFormat = new JsonFormat[Period] {
    val formatter = new PeriodFormatterBuilder()
      .minimumPrintedDigits(2)
      .printZeroAlways()
      .appendHours().appendSuffix(":")
      .appendMinutes().appendSuffix(":")
      .appendSeconds()
      .toFormatter

    def read(json: JsValue): Period = json match {
      case JsString(period) => formatter.parsePeriod(period)
      case _ => throw new DeserializationException("Period in hh:mm:ss expected")
    }

    def write(obj: Period): JsValue = JsString(obj.toString(formatter))
  }

  implicit val durationFormat = new JsonFormat[Duration] {
    def read(json: JsValue): Duration = json match {
      case JsNumber(seconds) => Duration.standardSeconds(seconds.toInt)
      case _ => throw new DeserializationException("Duration in seconds expected")
    }
    def write(obj: Duration): JsValue = JsNumber(obj.toStandardSeconds.getSeconds)
  }

  implicit val stopTimeRecordFormat = new JsonFormat[StopTimeRecord] {
    def read(json: JsValue): StopTimeRecord = ???

    def write(obj: StopTimeRecord): JsValue = JsObject(
      "stop_id" -> obj.stopId.toJson,
      "stop_sequence" -> obj.sequence.toJson,
      "arrival_time" -> obj.arrivalTime.toJson,
      "departureTime" -> obj.departureTime.toJson
    )
  }
  implicit val frequencyRecordFormat = jsonFormat4(FrequencyRecord)
  implicit val tripRecordFormat = jsonFormat5(TripRecord)
  implicit val tripPatternFormat = jsonFormat3(TripPattern)
  implicit val tripTuplePatternFormat = new JsonFormat[TripTuple] {
    def read(json: JsValue): TripTuple = ???

    def write(obj: TripTuple): JsValue = JsObject(
      "trip_id" -> JsString(obj.trip.id),
      "service_id" -> JsString(obj.trip.serviceId),
      "route_id" -> JsString(obj.trip.routeId),
      "headsign" -> JsString(obj.trip.headsign.getOrElse("")),
      "stop_times" -> JsArray(obj.stopTimes.map(_.toJson):_*)
    )

  }
}

trait ScenariosRoute extends Route { self: DatabaseInstance =>
  import ScenariosJsonProtocol._
  private val tables = new GtfsTables with DefaultProfile
  import tables.profile.simple._

  private def buildTripTuple(trip: TripRecord)(implicit s: Session): TripTuple =
    TripTuple(trip, tables.stopTimeRecordsTable.filter( _.trip_id === trip.id).sortBy(_.stop_sequence).list)

  private def fetchBinnedTrips(routeId: String)(implicit s: Session): Array[Array[TripTuple]] = {
    val trips = tables.tripRecordsTable
      .filter(_.route_id === routeId)
      .list
      .map { trip => buildTripTuple(trip) }
    GroupTrips.groupByPath(trips)
  }

  private def deleteTrip(tripId: String)(implicit s: Session): Unit = {
    tables.stopTimeRecordsTable.filter(_.trip_id === tripId).delete
    tables.frequencyRecordsTable.filter(_.trip_id === tripId).delete
    tables.tripRecordsTable.filter(_.id === tripId).delete

  }
  private def saveTripPattern(pattern: TripPattern)(implicit s: Session): Unit = {
    tables.tripRecordsTable.insert(pattern.trip)
    tables.frequencyRecordsTable.insertAll(pattern.frequencies:_*)
    tables.stopTimeRecordsTable.insertAll(pattern.stopTimes:_*)
  }

  // TODO this probably needs a stops list as well, since they are movable

  def scenariosRoute =
    pathPrefix("scenarios" / Segment) { scenarioSlug =>
      //val db = ??? // TODO get the scenario database connection


      pathPrefix("routes" / Segment) { routeId =>

        /**
         * Load all trips in the route and bin them by their path, return only the one trip per bin
         * as a "representative" trip for that path pattern.
         */
        get {
          val unique = db withSession { implicit s =>
            fetchBinnedTrips(routeId) map { _.head }
          }

          // TODO link frequencies and stops
          complete(unique)
        } ~
        /**
         * Accept a trip pattern, use it as a basis for creating new TripRecord, StopTimesRecords and Stops.
         * Note:
         * Every save operation creates new stops because moving their location would also impact other routes.
         * Avoiding this behavior would require checking if the stop location has changed, which seems too expensive
         * to gain unclear benefits.
         */
        post { entity(as[TripPattern]) { pattern =>
          db withSession { implicit s =>
            val bins = fetchBinnedTrips(routeId)
            for { // delete all trips that are in the same bin as our parameter
              bin <- bins.find(_.exists(r => r.trip.id == pattern.trip.id))
              tt <- bin
            } deleteTrip(tt.trip.id)
            // TODO Stops will never cascade delete, but we're filling up the DB with "trash stops" on every save ?!

            saveTripPattern(pattern)
          }

          complete(StatusCodes.Created)
        } ~
        pathPrefix("trips"/ Segment) { tripId =>
          /** Fetch specific trip by id */
          get {
            val trip = db withSession { implicit s =>
              buildTripTuple(tables.tripRecordsTable.filter(_.id === tripId).first)
            }
            complete(trip)
          } ~
          /** Replace specific trip with parameter */
          post { entity(as[TripPattern]) { pattern =>
            db withSession { implicit s =>
              deleteTrip(pattern.trip.id)
              saveTripPattern(pattern)
            }
            complete(StatusCodes.Created)
          } } ~
          /** Delete all traces of the trip */
          delete {
            db withSession { implicit s =>
              deleteTrip(tripId)
            }
            complete(StatusCodes.OK)
          }
        }
        }
      }
    }
}
