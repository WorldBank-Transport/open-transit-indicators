package com.azavea.opentransit

import geotrellis.raster._
import geotrellis.vector._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

import scala.collection.mutable

case class RasterCacheKey(name: String)

class RasterCache(val system: ActorSystem) {
  implicit val timeout = Timeout(500 seconds)

  private val cacheActor =
    system.actorOf(Props(classOf[CacheActor[RasterCacheKey, (Tile, Extent)]]))

  def get(key: RasterCacheKey): Option[(Tile, Extent)] =
    Await.result(cacheActor ? CacheActor.Get(key), timeout.duration).asInstanceOf[Option[(Tile, Extent)]]

  def set(key: RasterCacheKey, value: (Tile, Extent)) =
    cacheActor ! CacheActor.Set(key, value)
}

class CacheActor[K, V] extends Actor {
  val cache = mutable.Map[K, V]()

  def receive = {
    case CacheActor.Get(key) => sender ! cache.get(key.asInstanceOf[K])
    case CacheActor.Set(key, value) => cache(key.asInstanceOf[K]) = value.asInstanceOf[V]
  }
}

object CacheActor {
  case class Get[K](key: K)
  case class Set[K, V](key: K, value: V)
}
