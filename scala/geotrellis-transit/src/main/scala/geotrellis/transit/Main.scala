package geotrellis.transit

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.transit.loader._
import geotrellis.transit.loader.gtfs._
import geotrellis.transit.loader.osm._
import geotrellis.network._
import geotrellis.network.graph._

import com.github.nscala_time.time.Imports.LocalDate

import spire.syntax.cfor._

import scala.collection.mutable

import geotrellis.jetty.WebRunner

import java.io._

object Main {
  private var _context: GraphContext = null
  def context = _context

  def initContext(configPath: String) = {
    _context = Configuration.loadPath(configPath).graph.getContext
    println("Initializing shortest path tree array...")
    ShortestPathTree.initSptArray(context.graph.vertexCount)
  }

  def main(args: Array[String]): Unit = {
    if(args.length < 1) {
      Logger.error("Must use subcommand")
      System.exit(1)
    }

    def inContext(f: ()=>Unit) = {
      val configPath = args(1)
      initContext(configPath)
      f
    }

    val call = 
      args(0) match {
        case "buildgraph" =>
          val configPath = args(1)
          () => buildGraph(configPath)
        case "buildworldbank" =>
          () => buildWorldBank()
        case "server" =>
          inContext(() => mainServer(args))
        case "info" =>
          inContext(() => graphInfo())
        case "debug" =>
          inContext(() => debug())
        case "memdebug" =>
          inContext(() => memdebug())
        case s =>
          Logger.error(s"Unknown subcommand $s")
          System.exit(1)
          () => { }
      }

    call()
  }

  def buildGraph(configPath: String) = {
    Logger.log(s"Building graph data from configuration $configPath")
    val config = Configuration.loadPath(configPath)
    Loader.buildGraph(config.graph.dataDirectory, config.loader.fileSets)
  }

  def buildWorldBank() = {
    val fileSets = 
      Seq(
        GtfsDateFiles("bus", "/Users/rob/data/philly/gtfs/google_bus", new LocalDate(2014, 2, 1)),
        GtfsDateFiles("train", "/Users/rob/data/philly/gtfs/google_rail", new LocalDate(2014, 2, 1)),
        OsmFileSet("Philadelphia", "/Users/rob/data/philly/osm/philadelphia.osm")
      )

    val outDir = "/Users/rob/proj/wb/data/philly-wb/"
    Loader.buildGraph(outDir, fileSets)
  }

  def mainServer(args: Array[String]) =
    WebRunner.run()

  def graphInfo() = {
    val graph = _context.graph
    var totalEdgeCount = 0
    Logger.log(s"Graph Info: ")
    for(mode <- graph.anytimeEdgeSets.keys) {
      val ec = graph.anytimeEdgeSets(mode).edgeCount
      totalEdgeCount += ec
      Logger.log(s"  $mode Edge Count: ${ec}")
    }
    for(mode <- graph.scheduledEdgeSets.keys) {
      val ec = graph.scheduledEdgeSets(mode).edgeCount
      totalEdgeCount += ec
      Logger.log(s"  $mode Edge Count: ${ec}")
    }

    Logger.log(s"  Total Edge Count: ${totalEdgeCount}")
    Logger.log(s"  Vertex Count: ${graph.vertexCount}")
  }

  def debug() = {
    val graph = _context.graph
    val vc = graph.vertexCount

    Logger.log("Finding suspicious walk edges...")
    for(i <- 0 until vc) {
      val sv = graph.vertexFor(i)
      graph.getEdgeIterator(Walking, EdgeDirection.Incoming).foreachEdge(i, Time.ANY.toInt) { (t, w) =>
        val tv = graph.vertexFor(t)
        val d = Distance.distance(sv.location, tv.location)
        if(d > 2000) {
          println(s"$sv  ->  $tv is $d meters.")
        }
      }
    }
    Logger.log("Done.")
  }

  def memdebug() = {
    val graph = _context.graph

    Logger.log("Checking for mem leak...")
    val ldelta: Float = 0.0018f

    Logger.log("Starting in 10 seconds...")
    Thread sleep 10000

    for(i <- 0 until 25) {
      val spt = ShortestPathTree.departure(i, Time(100), graph, Duration(60 * 60), Biking)
      val rv = ReachableVertices.fromSpt(spt).get
      val ReachableVertices(subindex, extent) = rv

      val (cols, rows) = (1000, 1000)
      val rasterExtent = RasterExtent(extent, cols, rows)

      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < rows, _ + 1) { row =>
          val destLong = rasterExtent.gridColToMap(col)
          val destLat = rasterExtent.gridRowToMap(row)

          val e = Extent(destLong - ldelta, destLat - ldelta, destLong + ldelta, destLat + ldelta)
          val l = subindex.pointsInExtent(e)
        }
      }
    }

    Logger.log("Done.")
  }
}
