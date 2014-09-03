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

  case class IndicatorJob(
    version: Int = 0,
    job_status: String = "processing"
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
    implicit val indicatorJobFormat = jsonFormat2(IndicatorJob)
  }

  // Execution context for futures
  import system.dispatcher

  // Bring the actor system in scope
  implicit val system = ActorSystem()

  // Class for interfacing with the Django API
  class DjangoClient(implicit system: ActorSystem) {
    // Pipeline for sending HTTP requests and receiving responses
    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    def processResponse(response: HttpRequest) {
      pipeline(response).map(_.entity.asString) onComplete {
        case Success(response) => println(response)
        case Failure(error) => println("An error has occured: " + error.getMessage)
      }
    }

    // Send a PATCH to update processing status for celery job
    def updateIndicatorJob(token: String, indicatorJob: IndicatorJob) = {
      import JsonImplicits._

      val indicator_job_uri = s"$BASE_URI/indicator-jobs/${indicatorJob.version}/"
      val patch = Patch(indicator_job_uri, indicatorJob) ~> addHeader("Authorization", s"Token $token")
      processResponse(patch)
    }

    // Sends a POST request to the indicators endpoint
    def postIndicators(token: String, indicators: List[Indicator]) = {
      import JsonImplicits._

      val post = Post(INDICATOR_URI, indicators) ~> addHeader("Authorization", s"Token $token")
      processResponse(post)
    }
  }

  // Create the client used for sending requests to Django
  val djangoClient = new DjangoClient()(system)
}
