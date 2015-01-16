package com.azavea.opentransit.service.json

import com.azavea.gtfs._
import com.azavea.opentransit.service.TripTuple
import com.sun.xml.internal.ws.encoding.soap.DeserializationException
import geotrellis.slick.Projected
import geotrellis.vector.json.GeoJsonSupport
import geotrellis.vector.{Line, Point}
import org.joda.time._
import org.joda.time.format.PeriodFormatterBuilder
import spray.json._

object ScenariosGtfsRouteJsonProtocol extends DefaultJsonProtocol with GeoJsonSupport {

  /** We use this prefix when to generate stop names when saving from a POST request */
  final val STOP_PREFIX = "TEMP"

  /** This REST API has no concept of service, so we synthesize */
  final val TRIP_SERVICE_ID = "ALWAYS"

  implicit object routeTypeFormat extends JsonFormat[RouteType]{
    def read(json: JsValue): RouteType = json match {
      case JsNumber(id) => RouteType(id.toInt)
      case _ => throw new DeserializationException("RouteType index expected")
    }

    def write(obj: RouteType): JsValue =
      JsNumber(obj.id)
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
      "end" -> obj.end.toJson,
      "headway" -> obj.headway.toJson
    )
  }

  implicit object stopFormat extends JsonWriter[Stop]{
    def read(json: JsValue)(tripId: String, seq: Int): Stop =
      json.asJsObject.getFields("stopId","name","lat","long") match {
        case Seq(JsString(stopId), JsString(name), JsNumber(lat), JsNumber(long)) =>
          Stop(s"${STOP_PREFIX}-${tripId}-${seq}", name, None, Projected(Point(long.toDouble, lat.toDouble), 4326))
        case _ => throw new DeserializationException("Stop expected")
      }

    def write(obj: Stop): JsValue = JsObject(
      "stopId" -> obj.id.toJson,
      "name" -> obj.name.toJson,
      "lat" -> obj.point.geom.y.toJson,
      "long" -> obj.point.geom.x.toJson
    )
  }

  implicit object stopTimeFormat extends JsonWriter[(StopTimeRecord, Stop)]{
    def read(json: JsValue)(tripId: String): (StopTimeRecord, Stop) =
    json.asJsObject.getFields("stop", "stopSequence", "arrivalTime", "departureTime") match {
      case Seq(stopJson: JsObject, JsNumber(seq), arrival, departure) =>
        val stop = stopFormat.read(stopJson)(tripId, seq.toInt)
        val st = StopTimeRecord(stop.id, tripId, seq.toInt, arrival.convertTo[Period], departure.convertTo[Period])
        st -> stop
      case _ =>  throw new DeserializationException("Stop Time expected")
    }

    def write(obj: (StopTimeRecord, Stop)): JsValue = {
      val (st, stop) = obj
      JsObject(
        "stopSequence" -> st.sequence.toJson,
        "arrivalTime" -> st.arrivalTime.toJson,
        "departureTime" -> st.departureTime.toJson,
        "stop" -> stop.toJson
      )
    }
  }

  implicit object shapeFormat extends JsonWriter[Option[TripShape]]{
    def write(obj: Option[TripShape]): JsValue =
      obj match {
        case Some(shape) => shape.line.geom.toJson
        case None => JsNull
      }

    def read(json: JsValue)(tripId: String): Option[TripShape] = json match {
      case _: JsObject => Some(TripShape(s"${STOP_PREFIX}-${tripId}", Projected(json.convertTo[Line], 4326)))
      case JsNull => None
      case _ => throw new DeserializationException("Shape line expected")
    }
  }

  implicit object tripTupleFormat extends RootJsonFormat[TripTuple] {
    def read(json: JsValue): TripTuple =
      json.asJsObject.getFields("tripId", "routeId", "headsign", "stopTimes", "frequencies", "shape") match {
        case Seq(JsString(tripId), JsString(routeId), headsign, JsArray(stopTimesJson) , JsArray(freqsJson), shapeJson) =>
          val stopTimes = stopTimesJson map { js => stopTimeFormat.read(js)(tripId) }
          val freqs = freqsJson map { js => frequencyFormat.read(js)(tripId) }
          val shape = shapeFormat.read(shapeJson)(tripId)
          val trip = TripRecord(tripId, TRIP_SERVICE_ID, routeId, headsign.convertTo[Option[String]], shape.map(_.id))
          TripTuple(trip, stopTimes, freqs, shape)
        case _ =>  throw new DeserializationException("TripTuple expected")
      }

    def write(obj: TripTuple): JsValue = JsObject(
      "tripId" -> JsString(obj.trip.id),
      "routeId" -> JsString(obj.trip.routeId),
      "headsign" -> JsString(obj.trip.headsign.getOrElse("")),
      "stopTimes" -> JsArray(obj.stopTimes map (_.toJson): _*),
      "frequencies" -> JsArray( obj.frequencies map (_.toJson):_*),
      "shape" -> obj.shape.toJson
    )
  }
}
