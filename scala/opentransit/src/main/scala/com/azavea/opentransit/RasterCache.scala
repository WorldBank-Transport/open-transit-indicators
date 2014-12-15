package com.azavea.opentransit

import geotrellis.raster._
import geotrellis.raster.io.arg.ArgWriter
import geotrellis.vector._
import geotrellis.engine._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

import com.typesafe.config._

import scala.collection.mutable

import java.io._

case class RasterCacheKey(name: String)

trait RasterCache {
  def get(key: RasterCacheKey): Option[(Tile, Extent)]
  def set(key: RasterCacheKey, value: (Tile, Extent)): Unit
}

object RasterCache {
  def apply(system: ActorSystem): AkkaRasterCache =
    AkkaRasterCache(system)
}

class AkkaRasterCache private (val system: ActorSystem, path: String, val initialCache: Map[RasterCacheKey, (Tile, Extent)]) extends RasterCache {
  implicit val timeout = Timeout(500 seconds)

  final val directory = "raster-cache"

  val save: (RasterCacheKey, Tile, Extent) => Unit = {
    val f = new java.io.File(directory)
    if(!f.exists) { 
      f.mkdirs
    } else if(!f.isDirectory) {
      sys.error(s"RasterCache path ${f.getAbsolutePath} exists but is not a directory.")
    }

    { (key: RasterCacheKey, tile: Tile, extent: Extent) =>
      val tup = (tile, extent)

      val fileOut =
        new FileOutputStream(new File(f, key.name))

      val out = new ObjectOutputStream(fileOut)
      
      out.writeObject(tup)
      out.close()
      fileOut.close()
    }
  }

  private val cacheActor = {
    val props = Props(classOf[CacheActor[RasterCacheKey, (Tile, Extent)]], initialCache)
    system.actorOf(props)
  }

  def get(key: RasterCacheKey): Option[(Tile, Extent)] =
    Await.result(cacheActor ? CacheActor.Get(key), timeout.duration).asInstanceOf[Option[(Tile, Extent)]]

  def set(key: RasterCacheKey, value: (Tile, Extent)): Unit = {
    cacheActor ! CacheActor.Set(key, value)

    // Save to the catalog
    println(s"Saving raster $key to $path.")

    ArgWriter(value._1.cellType).write(s"$path/${key.name}.arg", value._1, value._2, key.name)
  }
}

object AkkaRasterCache {
  def apply(system: ActorSystem): AkkaRasterCache = {
    val catalogPath = ConfigFactory.load.getString("opentransit.catalog")
    val directory = new File(new File(catalogPath).getParentFile, "data").getAbsolutePath
    val catalog = Catalog.fromPath(catalogPath)

    // Read any existing raster data in the catalog.
    val initialCache =
      catalog
        .stores
        .values
        .map(_.getNames)
        .flatten
        .map { name =>
          val layer = catalog.getRasterLayer(LayerId(None, name)).get

          val tile: Tile = layer.getRaster
          val extent: Extent = layer.info.rasterExtent.extent

          (RasterCacheKey(name), (tile, extent))
         }
        .toMap

    new AkkaRasterCache(system, directory, initialCache)
  }
}

class CacheActor[K, V](initialCache: Map[K, V]) extends Actor {
  val cache = mutable.Map[K, V](initialCache.toSeq: _*)

  def receive = {
    case CacheActor.Get(key) => sender ! cache.get(key.asInstanceOf[K])
    case CacheActor.Set(key, value) => cache(key.asInstanceOf[K]) = value.asInstanceOf[V]
  }
}

object CacheActor {
  case class Get[K](key: K)
  case class Set[K, V](key: K, value: V)
}
