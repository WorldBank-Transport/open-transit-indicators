package geotrellis.transit.loader

import geotrellis.transit._
import geotrellis.network._
import geotrellis.network.graph._

import geotrellis.transit.loader.gtfs.{GtfsDateFiles}
import geotrellis.transit.loader.osm.OsmFileSet

import geotrellis.vector._

import scala.collection.mutable

import java.io._

object Loader {
  def write[T](path: String, o: T) = {
    val file = new FileOutputStream(path)
    val buffer = new BufferedOutputStream(file)
    val output = new ObjectOutputStream(buffer)
    try {
      output.writeObject(o)
    } catch {
      case e: Exception =>
        val f = new File(path)
        if(f.exists) { f.delete }
        throw e
    } finally{
      output.close()
    }
    Logger.log(s"Wrote graph to $path")
  }

  def buildGraph(outputDirectory: String, fileSets: Iterable[GraphFileSet]) = {
    Logger.log("BUILDING GRAPH")
    val (transitGraph, transitVertices, transitEdges) = 
      build(fileSets.toSeq)

    write(new File(outputDirectory, "transit.graph").getPath, transitGraph)
    write(new File(outputDirectory, "transit.vertices").getPath, transitVertices)
    write(new File(outputDirectory, "transit.edges").getPath, transitEdges)

    Logger.log(s"Wrote graph data to ${outputDirectory}")
  }

  def loadFileSet(fileSet: GraphFileSet): ParseResult = {
    Logger.timedCreate(s"Loading ${fileSet.name} data into unpacked graph...",
                        "Upacked graph created.") { () =>
      fileSet.parse
    }
  }

  def build(fileSets: Seq[GraphFileSet]): (TransitGraph, NamedLocations, NamedWays) = {
    if(fileSets.length < 1) { sys.error("Argument error: Empty list of file sets.") }

    // Merge the graphs from all the File Sets into eachother.
    val mergedResult = 
      fileSets.drop(1)
              .foldLeft(loadFileSet(fileSets(0))) { (result, fileSet) =>
                 result.merge(loadFileSet(fileSet))
               }

    val index =     
      Logger.timedCreate("Creating location spatial index...", "Spatial index created.") { () =>
        SpatialIndex(mergedResult.graph.vertices.filter(_.vertexType == StreetVertex)) { v =>
          (v.location.lat, v.location.long)
        }
      }

    Logger.timed("Creating edges between stations.", "Transfer edges created.") { () =>
      val stationVertices = 
        Logger.timedCreate(" Finding all station vertices...", " Done.") { () =>
          mergedResult.graph.vertices.filter(_.vertexType == StationVertex).toSeq
        }

      val transferToStationDistance = 200

      Logger.timed(s" Iterating through ${stationVertices.length} " + 
                   "stations to connect to street vertices...",
                   s" Done.") { () =>
        var transferEdgeCount = 0
        var noTransferEdgesCount = 0
        for(v <- stationVertices) {
          val extent =
            Distance.getBoundingBox(v.location.lat, v.location.long, transferToStationDistance)

          val nearest = {
            val l = index.pointsInExtent(extent)
            val (px, py) = (v.location.lat, v.location.long)
            if(l.isEmpty) { None }
            else {
              var n = l.head
              var minDist = {
                val (x, y) = (n.location.lat, n.location.long)
                index.measure.distance(x, y, px, py)
              }
              for(t <- l.tail) {
                val (x, y) = (t.location.lat, t.location.long)
                val d = index.measure.distance(px, py, x, y)
                if(d < minDist) {
                  n = t
                  minDist = d
                }
              }
              Some(n)
            }

          }

          nearest match {
            case Some(nearest) =>
              val duration = 
                Duration((Distance.distance(v.location, nearest.location) / Speeds.walking).toInt)
              mergedResult.graph.addEdge(v, WalkEdge(nearest, duration))
              mergedResult.graph.addEdge(nearest, WalkEdge(v, duration))
              mergedResult.graph.addEdge(v, BikeEdge(nearest, duration))
              mergedResult.graph.addEdge(nearest, BikeEdge(v, duration))
              transferEdgeCount += 2
            case _ => 
              Logger.warn(s"NO TRANSFER EDGES CREATED FOR STATION ${v.name} at ${v.location}")
              noTransferEdgesCount += 1
          }
        }
        Logger.log(s"   $transferEdgeCount tranfer edges created")
        if(noTransferEdgesCount > 0) {
          Logger.warn(s"THERE WERE $noTransferEdgesCount STATIONS WITH NO TRANSFER EDGES.")
        }
      }
    }

    val graph = mergedResult.graph

    val we = graph.edgeCount(Walking)
    val be = graph.edgeCount(Biking)

    val transitGraphs = 
      (for(fs <- fileSets) yield {
        fs match {
          case GtfsDateFiles(name, dataPath, date) =>
            Some((name, List(
              graph.edgeCount(ScheduledTransit(name, WeekDaySchedule)),
              graph.edgeCount(ScheduledTransit(name, DaySchedule(Saturday))),
              graph.edgeCount(ScheduledTransit(name, DaySchedule(Sunday))),
              graph.edgeCount(ScheduledTransit(name, EveryDaySchedule))
            )))
          case _ => None
        }
      }).flatten.toMap
    val totalTransit = transitGraphs.values.map(_.foldLeft(0)(_ + _)).foldLeft(0)(_ + _)

    Logger.log(s"Graph Info: ")
    Logger.log(s"  Walk Edge Count: ${we}")
    Logger.log(s"  Bike Edge Count: ${be}")

    for(service <- transitGraphs.keys) {
      val l = transitGraphs(service)
      Logger.log(s"  $service Weekday Transit Edge Count: ${l(0)}")
      Logger.log(s"  $service Saturday Transit Edge Count: ${l(1)}")
      Logger.log(s"  $service Sunday Transit Edge Count: ${l(2)}")
      Logger.log(s"  $service Everyday Transit Edge Count: ${l(3)}")
    }
    Logger.log(s"  Total Edge Count: ${we + be + totalTransit}")
    Logger.log(s"  Vertex Count: ${graph.vertexCount}")

    val packed =
      Logger.timedCreate("Packing graph...",
        "Packed graph created.") { () =>
        graph.pack
      }

    val totalEnvelope = 
      MultiPoint(mergedResult.namedLocations.values.map{ nl =>
        val Location(lat, lng) = nl.location
        Point(lng, lat)
      }).envelope

    Logger.log(s"System is contained within $totalEnvelope")


    (packed, mergedResult.namedLocations, mergedResult.namedWays)
  }
}
