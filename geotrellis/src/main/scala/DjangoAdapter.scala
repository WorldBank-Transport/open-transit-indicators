package opentransitgt

import akka.actor.ActorSystem
import com.github.nscala_time.time.Imports._
import org.joda.time.format.ISODateTimeFormat
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

// JSON support
import spray.json._
import spray.json.AdditionalFormats
import spray.httpx.SprayJsonSupport
import SprayJsonSupport._
import DefaultJsonProtocol._

// HTTP client support
import spray.client._
import spray.client.pipelining._
import spray.http.HttpMethods._
import spray.http.HttpHeaders._
import spray.http.HttpRequest
import spray.http.HttpResponse
import spray.http.MediaTypes._


// Fosters communication between GeoTrellis and Django via case classes,
// formatters, and methods for transferring JSON data.
object DjangoAdapter {
  // Endpoint URIs
  val BASE_URI = "http://localhost/api"
  val INDICATOR_URI = s"$BASE_URI/indicators/"

  // Sample period parameters
  case class SamplePeriod(
    id: Int,
    `type`: String,
    period_start: DateTime,
    period_end: DateTime
  )

  // Calculation request parameters
  case class CalcParams(
    token: String,
    version: Int,
    sample_periods: List[SamplePeriod]
  )

  // Indicator
  case class Indicator(
    `type`: String,
    sample_period: String,
    aggregation: String = "system",
    route_id: String = "",
    route_type: Int = 0,
    city_bounded: Boolean = false,
    version: Int = 0,
    value: Double = 0,
    the_geom: String = ""
  )

  // Custom JSON formatters
  object JsonImplicits extends DefaultJsonProtocol with SprayJsonSupport {
    // DateTime (nscala-time) isn't parsed by default, so we need to write our own
    implicit object DateTimeFormat extends RootJsonFormat[DateTime] {
      private val isoParser = ISODateTimeFormat.dateTimeNoMillis();
      def write(dt: DateTime) = JsString(isoParser.print(dt))
      def read(value: JsValue) = value match {
        case JsString(s) => isoParser.parseDateTime(s)
        case _ => throw new DeserializationException(s"Error parsing DateTime: $value")
      }
    }

    // Use built-in JSON formats for our case classes
    implicit val samplePeriodFormat = jsonFormat4(SamplePeriod)
    implicit val calcParamsFormat = jsonFormat3(CalcParams)
    implicit val indicatorFormat = jsonFormat9(Indicator)
  }

  // Execution context for futures
  import system.dispatcher

  // Bring the actor system in scope
  implicit val system = ActorSystem()

  // Class for interfacing with the Django API
  class DjangoClient(implicit system: ActorSystem) {
    // Pipeline for sending HTTP requests and receiving responses
    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    // Sends a POST request to the indicators endpoint
    def postIndicator(token: String, indicator: Indicator) = {
      import JsonImplicits._

      val post = Post(INDICATOR_URI, indicator) ~> addHeader("Authorization", s"Token $token")
      pipeline(post).map(_.entity.asString) onComplete {
        case Success(response) => println(response)
        case Failure(error) => println("An error has occured: " + error.getMessage)
      }
    }
  }

  // Create the client used for sending requests to Django
  val djangoClient = new DjangoClient()(system)

  // Sends all indicator calculations to Django for storage
  //
  // TODO: refactor this to use some sort of introspection system so it is
  //   no longer necessary to individually store each indicator.
  // TODO: Add the_geom for mode indicators
  def storeIndicators(token: String, version: Int, period: SamplePeriod, calc: IndicatorsCalculator) = {

    def stringGeomForRouteId(routeID: String) = {
        calc.lineForRouteIDLatLng(routeID) match {
            case None => ""
            case Some(routeLine) => routeLine.toString
        }
    }

    // Number of routes
    for ((mode, value) <- calc.numRoutesPerMode) {
      djangoClient.postIndicator(token, Indicator(
        `type`="num_routes", sample_period=period.`type`, aggregation="mode",
        route_type=mode, version=version,value=value)
      )
    }

    // Stops per route
    for ((route, value) <- calc.maxStopsPerRoute) {
      djangoClient.postIndicator(token, Indicator(
        `type`="num_stops", sample_period=period.`type`, aggregation="route",
        route_id=route, version=version,value=value,
        the_geom=stringGeomForRouteId(route)
        )
      )
    }

    // Stops per mode
    for ((mode, value) <- calc.numStopsPerMode) {
      djangoClient.postIndicator(token, Indicator(
        `type`="num_stops", sample_period=period.`type`, aggregation="mode",
        route_type=mode, version=version,value=value)
      )
    }

    // Length per route
    for ((route, value) <- calc.maxTransitLengthPerRoute) {
      djangoClient.postIndicator(token, Indicator(
        `type`="length", sample_period=period.`type`, aggregation="route",
        route_id=route, version=version,value=value,
        the_geom=stringGeomForRouteId(route))
      )
    }

    // Length per mode
    for ((mode, value) <- calc.avgTransitLengthPerMode) {
      djangoClient.postIndicator(token, Indicator(
        `type`="length", sample_period=period.`type`, aggregation="mode",
        route_type=mode, version=version,value=value)
      )
    }

    // Avg time between stops per route
    for ((route, value) <- calc.avgTimeBetweenStopsPerRoute) {
      djangoClient.postIndicator(token, Indicator(
        `type`="time_traveled_stops", sample_period=period.`type`, aggregation="route",
        route_id=route, version=version, value=value,
        the_geom=stringGeomForRouteId(route))
      )
    }

    // Avg time between stops per mode
    for ((mode, value) <- calc.avgTimeBetweenStopsPerMode) {
      djangoClient.postIndicator(token, Indicator(
        `type`="time_traveled_stops", sample_period=period.`type`, aggregation="mode",
        route_type=mode, version=version, value=value)
      )
    }

    // Avg service frequency/headway per route
    for ((route, value) <- calc.headwayByRoute) {
      djangoClient.postIndicator(token, Indicator(
        `type`="avg_service_freq", sample_period=period.`type`, aggregation="route",
        route_id=route, version=version, value=value,
        the_geom=stringGeomForRouteId(route))
      )
    }

    // Avg service frequency/headway per mode
    for ((mode, value) <- calc.headwayByMode) {
      djangoClient.postIndicator(token, Indicator(
        `type`="avg_service_freq", sample_period=period.`type`, aggregation="mode",
        route_type=mode, version=version, value=value)
      )
    }
  }
}
