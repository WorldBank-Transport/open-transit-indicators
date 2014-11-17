package geotrellis.transit

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

object SptInfoCache {
  implicit val timeout = Timeout(500 seconds)

  // Create cache actor
  private val system = geotrellis.engine.GeoTrellis.engine.system
  private val cacheActor = 
    system.actorOf(Props(
      classOf[CacheActor[SptInfoRequest,SptInfo]],
      10000L,
      1000L,
      { request:SptInfoRequest => SptInfo(request) }))

  def get(request:SptInfoRequest) =
    Await.result(cacheActor ? CacheLookup(request), timeout.duration).asInstanceOf[SptInfo]
}
