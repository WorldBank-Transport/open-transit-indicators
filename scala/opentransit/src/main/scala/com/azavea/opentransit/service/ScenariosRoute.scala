package com.azavea.opentransit.service

import com.azavea.gtfs.io.database.{DatabaseRecordImport, DefaultProfile, GtfsTables}
import com.azavea.gtfs._
import com.azavea.opentransit.DatabaseInstance
import com.sun.xml.internal.ws.encoding.soap.DeserializationException
import geotrellis.slick.Projected
import geotrellis.vector.Point
import org.joda.time._
import org.joda.time.format.PeriodFormatterBuilder
import spray.http._
import spray.json.DefaultJsonProtocol
import spray.json._
import spray.httpx.SprayJsonSupport

case class TripTuple(trip: TripRecord, stopTimes: Seq[(StopTimeRecord, Stop)], frequencies: Seq[FrequencyRecord])

trait ScenariosRoute extends Route with SprayJsonSupport { self: DatabaseInstance =>

  private val tables = new GtfsTables with DefaultProfile
  import tables.profile.simple._

  /** Load all stop_times for route and use them to group trip_ids by trip path */
  private def fetchTripBins(routeId: String)(implicit s: Session): Array[Array[TripId]] = {
    /** extract part of trip that identifies unique path */
    def tripOffset(stopTimes: Seq[StopTimeRecord]) = {
      import com.github.nscala_time.time.Imports._

      val offset = stopTimes.head.arrivalTime
      stopTimes map { st =>
        (st.stopId, (st.arrivalTime - offset).toStandardSeconds , (st.departureTime - offset).toStandardSeconds)
      }
    }

    val stopTimesAll = tables.tripRecordsTable.filter(_.route_id === routeId)
      .join(tables.stopTimeRecordsTable).on(_.id === _.trip_id)
      .sortBy(_._2.stop_sequence)
      .map(_._2)
      .list

    val tripStopTimes = stopTimesAll
      .groupBy(_.tripId)
      .map{ case (tripId, stops) => tripId -> stops.sortBy(_.sequence) }
      .toArray

    // note: we also have the the trip headway information, if it's ever useful

    tripStopTimes
      .groupBy{ case (tripId, stops) => tripOffset(stops) }
      .values // the keys are trip patterns
      .map(tripStopList => tripStopList.map(_._1)) // discarding the stop-times, wasteful ?
      .toArray
  }

  private def deleteTrip(tripId: String)(implicit s: Session): Unit = {
    //Delete stops created through this service, if they exist for this trip
    val stopIds = { tables.stopTimeRecordsTable filter (_.trip_id === tripId) map (_.stop_id) }
    ( tables.stopsTable
      filter ( stop => stop.id.like(s"${ScenariosJsonProtocol.STOP_PREFIX}-${tripId}%") && stop.id.in(stopIds))
      delete
    )

    tables.stopTimeRecordsTable.filter(_.trip_id === tripId).delete
    tables.frequencyRecordsTable.filter(_.trip_id === tripId).delete
    tables.tripRecordsTable.filter(_.id === tripId).delete
  }

  private def saveTripPattern(pattern: TripTuple)(implicit s: Session): Unit = {
    tables.tripRecordsTable.insert(pattern.trip)
    tables.frequencyRecordsTable.insertAll(pattern.frequencies:_*)
    tables.stopTimeRecordsTable.insertAll(pattern.stopTimes map {_._1}:_*)
    tables.stopsTable.insertAll(pattern.stopTimes map {_._2}:_*)
  }

  private def buildTripPattern(trip: TripRecord)(implicit s: Session): TripTuple = {
    val stops = (
      tables.stopTimeRecordsTable
        filter (_.trip_id === trip.id)
        join tables.stopsTable on (_.stop_id === _.id)
        sortBy { case (st, _) => st.stop_sequence }
        list
    )
    val frequencies = tables.frequencyRecordsTable.filter(_.trip_id === trip.id).list

    val shape = trip.tripShapeId map { shapeId => tables.tripShapesTable.filter(_.id === shapeId)}

    TripTuple(trip, stops, frequencies)
  }


  /** This seems weird, but scalac will NOT find this implicit with simple import */
  implicit val tripPatternFormat = ScenariosJsonProtocol.tripTupleFormat
  implicit val routeFormat = ScenariosJsonProtocol.routeFormat

  def scenariosRoute =
    pathPrefix("scenarios" / Segment) { scenarioSlug =>
      //val db = ??? // TODO get the scenario database connection
      pathPrefix("routes") {
        pathEnd {
          complete {
            import DefaultJsonProtocol._
            db withSession { implicit s =>
              val routes: List[RouteRecord] = tables.routeRecordsTable.list
              routes
            }
          }
        } ~
        pathPrefix(Segment) { routeId =>
          pathPrefix("trips") {
            pathEnd {
              /** List all trip_ids in the route and bin them by their path */
              get {
                complete {
                  import DefaultJsonProtocol._
                  db withSession { implicit s =>
                    fetchTripBins(routeId)
                  }
                }
              }
            } ~
            pathPrefix(Segment) { tripId =>
              val trip = tables.tripRecordsTable.filter(trip => trip.id === tripId && trip.route_id === routeId)

              /** Fetch specific trip by id */
              get {
                complete {
                  db withSession { implicit s =>
                    trip.firstOption.map(buildTripPattern)
                  }
                }
              } ~
              /** Accept a trip pattern, use it as a basis for creating new TripRecord, StopTimesRecords and Stops. */
              post {
                entity(as[TripTuple]) { pattern =>
                  complete {
                    db withTransaction { implicit s =>
                      val bins = fetchTripBins(routeId)
                      for {
                        bin <- bins.find(_.contains(pattern.trip.id))
                        tripId <- bin // delete all trips that are in the same bin as our parameter
                      } deleteTrip(tripId)

                      saveTripPattern(pattern)
                      StatusCodes.Created
                    }
                  }
                }
              } ~
              /** Delete all traces of the trip */
              delete {
                complete {
                  db withTransaction { implicit s =>
                    trip.firstOption
                      .map { _ =>
                      deleteTrip(tripId)
                      StatusCodes.OK
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
}

object ScenariosJsonProtocol{
  import DefaultJsonProtocol._

  /** We use this prefix when to generate stop names when saving from a POST request */
  final val STOP_PREFIX = "TEMP"

  /** This REST API has no concept of service, so we synthesize */
  final val TRIP_SERVICE_ID = "ALWAYS"

  implicit object routeTypeFormat extends JsonFormat[RouteType]{
    def read(json: JsValue): RouteType = json match {
      case JsString(name) => RouteType(name)
      case _ => throw new DeserializationException("RouteType index expected")
    }

    def write(obj: RouteType): JsValue =
      JsString(obj.name)
  }

  implicit val routeFormat = jsonFormat9(RouteRecord)
  
  implicit object periodFormat extends JsonFormat[Period] {
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

  implicit object durationFormat extends JsonFormat[Duration] {
    def read(json: JsValue): Duration = json match {
      case JsNumber(seconds) => Duration.standardSeconds(seconds.toInt)
      case _ => throw new DeserializationException("Duration in seconds expected")
    }
    def write(obj: Duration): JsValue = JsNumber(obj.toStandardSeconds.getSeconds)
  }

  implicit object frequencyFormat extends JsonWriter[FrequencyRecord] {
    def read(json: JsValue)(tripId: String): FrequencyRecord =
      json.asJsObject.getFields("start", "end", "headway") match {
        case Seq(start, end, headway) =>
          FrequencyRecord(tripId, start.convertTo[Period], end.convertTo[Period], headway.convertTo[Duration])
        case _ => throw new DeserializationException("Frequency expected")
      }

    def write(obj: FrequencyRecord) = JsObject(
      "start" -> obj.start.toJson,
      "end" -> obj.start.toJson,
      "headway" -> obj.start.toJson
    )
  }

  implicit object stopFormat extends JsonWriter[Stop]{
    def read(json: JsValue)(tripId: String, seq: Int): Stop =
      json.asJsObject.getFields("stop_id","name","lat","long") match {
        case Seq(JsString(stopId), JsString(name), JsNumber(lat), JsNumber(long)) =>
          Stop(s"${STOP_PREFIX}-${tripId}-${seq}", name, None, Projected(Point(long.toDouble, lat.toDouble), 4326))
        case _ => throw new DeserializationException("Stop expected")
      }

    def write(obj: Stop): JsValue = JsObject(
      "stop_id" -> obj.id.toJson,
      "name" -> obj.name.toJson,
      "lat" -> obj.point.geom.y.toJson,
      "long" -> obj.point.geom.x.toJson
    )
  }

  implicit object stopTimeFormat extends JsonWriter[(StopTimeRecord, Stop)]{
    def read(json: JsValue)(tripId: String): (StopTimeRecord, Stop) =
    json.asJsObject.getFields("stop", "stop_sequence", "arrival_time", "departure_time") match {
      case Seq(stopJson: JsObject, JsNumber(seq), arrival, departure) =>
        val stop = stopFormat.read(stopJson)(tripId, seq.toInt)
        val st = StopTimeRecord(stop.id, tripId, seq.toInt, arrival.convertTo[Period], departure.convertTo[Period])
        st -> stop
      case _ =>  throw new DeserializationException("Stop Time expected")
    }

    def write(obj: (StopTimeRecord, Stop)): JsValue = {
      val (st, stop) = obj
      JsObject(
        "stop_sequence" -> st.sequence.toJson,
        "arrival_time" -> st.arrivalTime.toJson,
        "departure_time" -> st.departureTime.toJson,
        "stop" -> stop.toJson
      )
    }
  }

   val tripTupleFormat = new RootJsonFormat[TripTuple] {
    def read(json: JsValue): TripTuple =
      json.asJsObject.getFields("trip_id", "route_id", "headsign", "stop_times", "frequencies") match {
        case Seq(JsString(tripId), JsString(routeId), headsign, JsArray(stopTimesJson) , JsArray(freqsJson)) =>
          val stopTimes = stopTimesJson map { js => stopTimeFormat.read(js)(tripId) }
          val freqs = freqsJson map { js => frequencyFormat.read(js)(tripId) }
          val trip = TripRecord(tripId, TRIP_SERVICE_ID, routeId, headsign.convertTo[Option[String]])
          TripTuple(trip, stopTimes, freqs)
        case _ =>  throw new DeserializationException("TripTuple expected")
      }

    def write(obj: TripTuple): JsValue = JsObject(
      "trip_id" -> JsString(obj.trip.id),
      "route_id" -> JsString(obj.trip.routeId),
      "headsign" -> JsString(obj.trip.headsign.getOrElse("")),
      "stop_times" -> JsArray(obj.stopTimes map (_.toJson): _*),
      "frequencies" -> JsArray( obj.frequencies map (_.toJson):_*)
    )
  }
}