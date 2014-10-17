package com.azavea.opentransit.service

import com.azavea.gtfs.io.database.{DatabaseRecordImport, DefaultProfile, GtfsTables}
import com.azavea.gtfs.op.GroupTrips
import com.azavea.gtfs.op.GroupTrips.TripTuple
import com.azavea.gtfs.{GtfsRecords, StopTimeRecord, FrequencyRecord, TripRecord, Stop}
import com.azavea.opentransit.DatabaseInstance
import com.sun.xml.internal.ws.encoding.soap.DeserializationException
import org.joda.time._
import org.joda.time.format.PeriodFormatterBuilder
import spray.http._
import spray.json.DefaultJsonProtocol
import spray.json._
import spray.httpx.SprayJsonSupport

case class TripPattern(trip: TripRecord, stopTimes: Seq[(StopTimeRecord, Stop)], frequencies: Seq[FrequencyRecord])


trait ScenariosRoute extends Route with SprayJsonSupport { self: DatabaseInstance =>

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
    tables.stopTimeRecordsTable.insertAll(pattern.stopTimes map {_._1}:_*)
    tables.stopsTable.insertAll(pattern.stopTimes map {_._2}:_*)
  }

  private def buildTripPattern(tt: TripTuple)(implicit s: Session): TripPattern = {
    val tripId = tt.trip.id
    val stops = (
      tables.stopTimeRecordsTable join tables.stopsTable on (_.stop_id === _.id)
      sortBy { case (st, _) => st.stop_sequence }
      map { case (_, stop) => stop }
      list
     )
    TripPattern(
      trip  = tt.trip,
      stopTimes = tt.stopTimes zip stops,
      frequencies = tables.frequencyRecordsTable.filter(_.trip_id === tripId).list
    )
  }


  /** This seems weird, but scalac will NOT find this implicit with simple import */
  implicit val tripPatternFormat = ScenariosJsonProtocol.tripPatternFormat

  def scenariosRoute =
    pathPrefix("scenarios" / Segment) { scenarioSlug =>
      //val db = ??? // TODO get the scenario database connection

      pathPrefix("routes" / Segment) { routeId =>
        path(""){
          /**
           * Load all trips in the route and bin them by their path, return only the one trip per bin
           * as a "representative" trip for that path pattern.
           */
          get {
            complete {
              import DefaultJsonProtocol._
              db withSession { implicit s =>
                fetchBinnedTrips(routeId) map {
                  _.head
                } map buildTripPattern
              }
            }
          } ~
          /**
           * Accept a trip pattern, use it as a basis for creating new TripRecord, StopTimesRecords and Stops.
           * Note:
           * Every save operation creates new stops because moving their location would also impact other routes.
           * Avoiding this behavior would require checking if the stop location has changed, which seems too expensive
           * to gain unclear benefits.
           */
          post { entity(as[TripPattern]) { pattern =>
            complete {
              db withSession { implicit s =>
                val bins = fetchBinnedTrips(routeId)
                for {// delete all trips that are in the same bin as our parameter
                  bin <- bins.find(_.exists(r => r.trip.id == pattern.trip.id))
                  tt <- bin
                } deleteTrip(tt.trip.id)
                // TODO Stops will never cascade delete, but we're filling up the DB with "trash stops" on every save ?!

                saveTripPattern(pattern)
              }

              StatusCodes.Created
            }
          } }
        } ~
        pathPrefix("trips"/ Segment) { tripId =>
          /** Fetch specific trip by id */
          get {
            complete{
              db withSession { implicit s =>
                buildTripPattern(buildTripTuple(tables.tripRecordsTable.filter(_.id === tripId).first))
              }
            }
          } ~
          /** Replace specific trip with parameter */
          post { entity(as[TripPattern]) { pattern =>
            complete{
              db withSession { implicit s =>
                deleteTrip(pattern.trip.id)
                saveTripPattern(pattern)
              }

              StatusCodes.Created
            }
          } } ~
          /** Delete all traces of the trip */
          delete {
            complete{
              db withSession { implicit s =>
                deleteTrip(tripId)
              }
              StatusCodes.OK
            }
          }
        }
      }
    }
}

object ScenariosJsonProtocol{
  import DefaultJsonProtocol._

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


  implicit val stopFormat = new JsonFormat[Stop]{
    def read(json: JsValue): Stop = ???

    def write(obj: Stop): JsValue = JsObject(
      "stop_id" -> obj.id.toJson,
      "name" -> obj.name.toJson,
      "lat" -> obj.point.geom.y.toJson,
      "long" -> obj.point.geom.x.toJson
    )
  }

   val tripPatternFormat = new RootJsonFormat[TripPattern] {
    def read(json: JsValue): TripPattern = null

    def write(obj: TripPattern): JsValue = JsObject(
      "trip_id" -> JsString(obj.trip.id),
      "service_id" -> JsString(obj.trip.serviceId),
      "route_id" -> JsString(obj.trip.routeId),
      "headsign" -> JsString(obj.trip.headsign.getOrElse("")),
      "stop_times" -> JsArray(
        obj.stopTimes
          map { case (st, stop) =>
          JsObject(
            "stop_sequence" -> st.sequence.toJson,
            "arrival_time" -> st.arrivalTime.toJson,
            "departureTime" -> st.departureTime.toJson,
            "stop" -> stop.toJson
          )
        }:_*
      )
    )
  }
}