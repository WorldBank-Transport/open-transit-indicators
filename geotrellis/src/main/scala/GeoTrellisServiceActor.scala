package opentransitgt

import akka.actor._
import spray.util.LoggingContext
import spray.routing.ExceptionHandler
import spray.http.StatusCodes._

class GeoTrellisServiceActor extends GeoTrellisService with Actor {
  // The HttpService trait (which GeoTrellisService will extend) defines
  // only one abstract member, which connects the services environment
  // to the enclosing actor or test.
  def actorRefFactory = context

  def receive = runRoute(rootRoute)
}
