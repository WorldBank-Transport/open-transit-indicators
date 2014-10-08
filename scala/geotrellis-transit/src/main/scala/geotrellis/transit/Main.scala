package geotrellis.transit

import geotrellis.raster._
import geotrellis.vector._
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


  import geotrellis.raster.io.arg.ArgWriter
  import geotrellis.raster.io.geotiff._
  import geotrellis.raster.io.geotiff.reader._
  import geotrellis.proj4._
import geotrellis.raster.reproject._

  import scala.collection.mutable.PriorityQueue


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
        case "jobs" =>
          inContext(() => createJobIndicator)
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

  def createJobIndicator() =
    createJobIndicator2()

  def createJobIndicator2() = {
    val durations = List[Duration](
      Duration(15 * 60)
      // Duration(30 * 60),
      // Duration(45 * 60),
      // Duration(60 * 60),
      // Duration(90 * 60),
      // Duration(120 * 60)
    )

    val runs = 
      durations.map { duration =>
        val name = s"zhengzhou-jobs-indicator-${duration.toInt / 60}-min"
        val startTime = Time(9*60*60 - duration.toInt)
        val start = System.currentTimeMillis
        createFor(
          "/Users/rob/proj/wb/data/zhengzhou/jobs-small.tif",
          s"/Users/rob/proj/wb/data/zhengzhou/${name}.tif",
          name,
          startTime,
          duration
        )
        val end = System.currentTimeMillis
        (duration, end - start)
      }

    println(s"Done.")
    for((duration, time) <- runs) {
      println(s"  $duration took ${time} ms (${(time) / 1000 } seconds, ${(time).toDouble / (1000*60) } minutes) ")
    }
  }

  def createFor(inPath: String, outPath: String, outName: String, startTime: Time, duration: Duration): Unit = {
    val graph = _context.graph
    val index = _context.index

    val features: Array[MultiPolygonFeature[Int]] = {
      val path = "/Users/rob/proj/wb/data/zhengzhou/shapefiles/demographics-ll.shp"
      geotrellis.geotools.ShapeFileReader.readMultiPolygonFeatures[Long](path, "Employment")
        .map { feature =>
          MultiPolygonFeature(feature.geom, feature.data.toInt)
         }
        .toArray
    }

    val (jobTile, extent, crs) = GeoTiffReader(inPath).read.imageDirectories.head.toRaster
    val cols: Int = jobTile.cols
    val rows: Int = jobTile.rows
    val rasterExtent = RasterExtent(extent, cols, rows)

    val vertexCount = graph.vertexCount
    val polyCount = features.size

    val vertexToPolyId = Array.ofDim[Int](vertexCount).fill(-1)
    val polyIdToValue = Array.ofDim[Int](polyCount)

    // Setup clonable vertex array for SPT calculation
    val emptySptArray = Array.ofDim[Int](vertexCount).fill(-1)

    // Set up a clonable 0 array
    val zeros = Array.ofDim[Byte](polyCount)
    // Set up a byte that tells if a poly has been added to the sum
    val polyHit = 1.toByte

    cfor(0)(_ < features.size, _ + 1) { polyIndex =>
      val feature = features(polyIndex)
      val envelope = feature.geom.envelope
      val contained = index.pointsInExtent(envelope).toArray
      val containedLen = contained.size
      cfor(0)(_ < containedLen, _ + 1) { i =>
        val v = contained(i)
        vertexToPolyId(v) = polyIndex
      }
      polyIdToValue(polyIndex) = feature.data
    }

    // var count = 0
    // cfor(0)(_ < vertexToPolyId.size, _ + 1) { i =>
    //   if(vertexToPolyId(i) == -1) count += 1
    // }
    // println(s"There are ${vertexToPolyId.size} vertices, and $count outside of jobs. 0 poly value is ${polyIdToValue(0)}")
    // return 

    // SPT parameters
    val maxDuration = duration.toInt
    val edgeTypes = 
      Walking :: 
        _context.graph.transitEdgeModes
          .map(_.service)
          .toSet
          .map { s: String => ScheduledTransit(s, EveryDaySchedule) }.toList


    val tile = ArrayTile.empty(TypeDouble, cols, rows)

    cfor(0)(_ < rows, _ + 1) { row =>
      Logger.timedCreate(s"   Filling for row $row of $rows") { () =>
        cfor(0)(_ < cols, _ + 1) { col =>

          // Find the nearest start vertex (TODO: Do time calcuation on travel to that vertex)
          val (lng, lat) = rasterExtent.gridToMap(col, row)
          val startVertex = _context.index.nearest(lat, lng)

          var sum = 0
          val polyHits = zeros.clone

          // SHORTEST PATH CALCULATION

            /**
              * Array containing departure times of the current shortest
              * path to the index vertex.
              */
          val shortestPathTimes = emptySptArray.clone

          shortestPathTimes(startVertex) = 0

          // dijkstra's

          object VertexOrdering extends Ordering[Int] {
            def compare(a:Int, b:Int) =
              shortestPathTimes(a) compare shortestPathTimes(b)
          }

          val queue = new IntPriorityQueue(shortestPathTimes)

          val tripStart = startTime.toInt
          val duration = tripStart + maxDuration

          val edgeIterator =
            graph.getEdgeIterator(edgeTypes, EdgeDirection.Outgoing)

          edgeIterator.foreachEdge(startVertex,tripStart) { (target,weight) =>
            val t = tripStart + weight
            if(t <= duration) {
              shortestPathTimes(target) = t
              queue += target
              val polyId = vertexToPolyId(target)
              if(polyId != -1 && polyHits(polyId) != polyHit) {
                sum += polyIdToValue(polyId)
                polyHits(polyId) = polyHit
              }
            }
          }

          while(!queue.isEmpty) {
            val currentVertex = queue.dequeue
            val currentVertexShortestPathTime = shortestPathTimes(currentVertex)

            edgeIterator.foreachEdge(currentVertex, currentVertexShortestPathTime) { (target, weight) =>
              val t = currentVertexShortestPathTime + weight
              if(t <= duration) {
                val timeAtTarget = shortestPathTimes(target)
                if(timeAtTarget == -1 || t < timeAtTarget) {
                  val polyId = vertexToPolyId(target)
                  if(polyId != -1 && polyHits(polyId) != polyHit) {
                    sum += polyIdToValue(polyId)
                    polyHits(polyId) = polyHit
                  }

                  shortestPathTimes(target) = t
                  queue += target
                }
              }
            }
          }

          tile.set(col, row, sum)
        }
      }
    }

    println(s"MIN MAX ${tile.findMinMaxDouble}")

    //    new ArgWriter(TypeDouble).write("/Users/rob/proj/gt/geotrellis/raster-test/data/data/zhengzhou-jobs-indicator.arg", resultTile, extent, "zhengzhou-jobs-indicator")
    GeoTiffWriter.write(outPath, tile, extent, LatLng)

    val (wmTile, wmExtent) = tile.reproject(extent, LatLng, WebMercator)

    new ArgWriter(TypeDouble).write(s"/Users/rob/proj/gt/geotrellis/raster-test/data/data/${outName}.arg", wmTile, wmExtent, outName)
  }

  def createJobIndicator1() = {
    val graph = _context.graph

    val modes = 
      Walking :: 
        _context.graph.transitEdgeModes
          .map(_.service)
          .toSet
          .map { s: String => ScheduledTransit(s, EveryDaySchedule) }.toList

//    val (tile, extent, crs) = GeoTiffReader("/Users/rob/proj/wb/data/zhengzhou/jobs.tif").read.imageDirectories.head.toRaster  // Large Raster
    val (jobTile, extent, crs) = GeoTiffReader("/Users/rob/proj/wb/data/zhengzhou/jobs-small.tif").read.imageDirectories.head.toRaster
    val (cols, rows) = (jobTile.cols, jobTile.rows)
    val rasterExtent = RasterExtent(extent, cols, rows)

    val time = Time(9*60*60)
    val duration = Duration(60 * 60) // 1 Hour
    val maxDuration = duration.toInt

    val ldelta = 0.0018f
//    val ldelta = 0.0098f
    val ldelta2 = ldelta * ldelta

    val totalCells = cols * rows

    val resultTile =
      geotrellis.transit.Logger.timedCreate("Brute force multisource walkshed...", "Raster created.") { () =>

        val tile = ArrayTile.empty(TypeDouble, cols, rows)

        cfor(0)(_ < rows, _ + 1) { row =>
//        cfor(0)(_ < 3, _ + 1) { row =>
          Logger.timedCreate(s"   Filling for row $row of $rows") { () =>
            cfor(0)(_ < cols, _ + 1) { col =>
              val (lng, lat) = rasterExtent.gridToMap(col, row)
//              val (lat, lng) = rasterExtent.mapToGrid(col, row)
              val startVertex = _context.index.nearest(lat, lng)
              
//              println(s"START VERTEX IS ${graph.vertexFor(startVertex)} nearest to ($lat, $lng)")
              val spt =
                ShortestPathTree.departure(startVertex, time, graph, duration, modes:_*)

              ReachableVertices.fromSpt(spt) match {
                case Some(ReachableVertices(subindex, subextent)) =>
                  val gb  = rasterExtent.gridBoundsFor(subextent)
//                  val (colMin, rowMin, colMax, rowMax) = (0, 0, cols - 1, rows - 1)
                  val (colMin, rowMin, colMax, rowMax) = (math.max(0, gb.colMin), math.max(0, gb.rowMin), math.min(cols - 1, gb.colMax), math.min(rows - 1, gb.rowMax))
                  // println(s"$subextent in $extent?")
                  // println(gb)
                  var sum = 0.0

                  cfor(colMin)(_ <= colMax, _ + 1) { col2 =>
                    cfor(rowMin)(_ <= rowMax, _ + 1) { row2 =>
                      val destLong = rasterExtent.gridColToMap(col2)
                      val destLat = rasterExtent.gridRowToMap(row2)

                      val e = Extent(destLong - ldelta, destLat - ldelta, destLong + ldelta, destLat + ldelta)
                      val l = subindex.pointsInExtent(e)

                      if (!l.isEmpty) {
                        var s = 0.0
                        var c = 0
                        var ws = 0.0
                        val length = l.length
                        cfor(0)(_ < length, _ + 1) { i =>
                          val target = l(i).asInstanceOf[Int]
                          val t = spt.travelTimeTo(target).toInt
                          val loc = Main.context.graph.location(target)
                          val dlat = (destLat - loc.lat)
                          val dlong = (destLong - loc.long)
                          val d = dlat * dlat + dlong * dlong
                          if (d < ldelta2) {
                            val w = 1 / d
                            s += t * w
                            ws += w
                            c += 1
                          }
                        }
                        val mean = s / ws
                        if (c != 0 && mean.toInt <= maxDuration) {
                          sum += jobTile.getDouble(col2, row2)
                        }
//                        println("NOT EMPTY!")
                      }
                    }
                  }
                  tile.setDouble(col, row, sum * 100)
                case _ =>
                  // pass
              }
            }
          }
        }
      tile
      }

    println(s"MIN MAX ${resultTile.findMinMaxDouble}")

//    new ArgWriter(TypeDouble).write("/Users/rob/proj/gt/geotrellis/raster-test/data/data/zhengzhou-jobs-indicator.arg", resultTile, extent, "zhengzhou-jobs-indicator")
    GeoTiffWriter.write("/Users/rob/proj/wb/data/zhengzhou/indicator.tif", resultTile, extent, LatLng)
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
