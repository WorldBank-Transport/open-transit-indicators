package geotrellis.transit

import akka.actor._
import scala.concurrent.duration._

import scala.collection.mutable

case object CleanRequest

case class BuildRequest[TK](key:TK)
case class BuildResponse[TK,TV](key:TK,value:TV)

case class CacheLookup[TK](key:TK)

case class BuildWorkInProgress(builder:ActorRef,requesters:mutable.ListBuffer[ActorRef])

class CacheActor[TK,TV](expireTime:Long,
                        cleanInterval:Long = 1000L,
                        buildFunc:TK=>TV) extends Actor {

  val cache = mutable.Map[TK,TV]()
  val lastAccess = mutable.Map[TK,Long]()
  val pending = mutable.Map[TK,BuildWorkInProgress]()

  var cleanTick:Cancellable = null

  import context.dispatcher

  override
  def preStart() = { 
    cleanTick = context.system.scheduler.schedule(
                                           0 milliseconds,
                                           cleanInterval milliseconds,
                                           self,
                                           CleanRequest)
  }

  override
  def postStop() = if(cleanTick != null) { cleanTick.cancel }

  def receive = {
    case CleanRequest => cleanCache()
    case CacheLookup(key) => cacheLookup(key.asInstanceOf[TK],sender)
    case BuildResponse(key,value) => buildDone(key.asInstanceOf[TK],value.asInstanceOf[TV])
  }

  private def cleanCache() =
    for(key <- lastAccess.keys.toList) {
      if(System.currentTimeMillis - lastAccess(key) > expireTime) {
        cache.remove(key)
        lastAccess.remove(key)
      }
    }

  private def cacheLookup(key:TK,sender:ActorRef) =
    if(cache.contains(key)) {
      lastAccess(key) = System.currentTimeMillis
      sender ! cache(key)
    } else {
      if(pending.contains(key)) {
        pending(key).requesters += sender
      } else {
        val builder =
          context.actorOf(Props(classOf[BuilderActor[TK,TV]],self,buildFunc))
        pending(key) = BuildWorkInProgress(builder,mutable.ListBuffer(sender))
        builder ! BuildRequest(key)
      }
    }

  private def buildDone(key:TK,value:TV) = {
    if(!pending.contains(key)) { sys.error("Build done on a key not in pending.") }
    cache(key) = value
    lastAccess(key) = System.currentTimeMillis
    val BuildWorkInProgress(builder,requesters) = pending(key)
    builder ! PoisonPill
    for(r <- requesters) { r ! value }
    pending.remove(key)
  }
}

case class BuilderActor[TK,TV](cacheActor:ActorRef,buildFunc:TK=>TV) extends Actor {
  def receive = {
    case BuildRequest(key) => 
      val k = key.asInstanceOf[TK]
      cacheActor ! BuildResponse(k,buildFunc(k))
  }
}
