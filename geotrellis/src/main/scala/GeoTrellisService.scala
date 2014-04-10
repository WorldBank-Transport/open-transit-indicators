package opentransitgt

import spray.routing.{ExceptionHandler, HttpService}
import spray.http.MediaTypes
import spray.http.StatusCodes.InternalServerError
import spray.util.LoggingContext

import geotrellis._
import geotrellis.source.{ValueSource, RasterSource}
import geotrellis.process.{Error, Complete}
import geotrellis.render.ColorRamps
import geotrellis.statistics.Histogram


trait GeoTrellisService extends HttpService {

  def rootRoute = pingRoute

  // Endpoint for testing: browsing to /ping should return the text
  def pingRoute = path("ping") {
    get {
      complete("pong!")
    }
  }

  // This will be picked up by the runRoute(_) and used to intercept Exceptions
  implicit def OpenTransitGeoTrellisExceptionHandler(implicit log: LoggingContext) =
    ExceptionHandler {
      case e: Exception =>
        requestUri { uri =>
          complete(InternalServerError, s"Message: ${e.getMessage}\n Trace: ${e.getStackTrace.mkString("</br>")}" )
        }
    }
}
