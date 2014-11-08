package com.azavea.opentransit.indicators.parameters

import com.azavea.opentransit.indicators._
import com.azavea.opentransit.database.RoadsTable
import com.azavea.gtfs._

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.network._
import geotrellis.network.graph._
import geotrellis.transit.loader._
import geotrellis.transit.loader.gtfs._
import geotrellis.transit.loader.osm._

import grizzled.slf4j.Logging

import scala.slick.jdbc.JdbcBackend.{Database, DatabaseDef, Session}

import com.typesafe.config.ConfigFactory

case class TravelshedParams()

trait HasTravelshedGraph {
  def hasTravelshedGraph: Boolean = travelshedGraph.isDefined
  def travelshedGraph: Option[TravelshedGraph]
}

case class TravelshedGraph(graph: TransitGraph, index: SpatialIndex[Int], rasterExtent: RasterExtent, startTime: Int, duration: Int)

object TravelshedGraph extends Logging {
  // Meters that the travelshed should buffer out from the region envelope for creating the raster.
  val travelshedExtentBuffer = 10.0

  // Creates a graph of the transit system for the representative weekday of the periods.
  def apply(
    periods: Seq[SamplePeriod],
    builder: TransitSystemBuilder,
    regionBoundary: MultiPolygon, 
    resolution: Double, 
    startTime: Int, 
    duration: Int
  )(implicit session: Session): Option[TravelshedGraph] = {
    SamplePeriod.getRepresentativeWeekday(periods).map { weekday =>
      val system =
        builder.systemOn(weekday)

      info("Creating transit graph")
      Timer.timedTask("Transit graph created") {
        val osmParsedResult: ParseResult =
          Timer.timedTask("Parsed in OSM results") {
            OsmParser.parseLines(RoadsTable.allRoads)
          }

        val systemResult: ParseResult =
          GtfsDateParser.parse("transit", system)

        val ParseResult(unpackedGraph, _, _) =
          Timer.timedTask("Merged parse results.") {
            osmParsedResult.merge(systemResult)
          }

        val streetVertexIndex =
          SpatialIndex(unpackedGraph.vertices.filter(_.vertexType == StreetVertex)) { v =>
            (v.location.long, v.location.lat)
          }

        val stationVertices =
          unpackedGraph.vertices.filter(_.vertexType == StationVertex).toSeq

        val transferToStationDistance = 200

        Timer.timedTask(s"Created transfer vertices.") {
          var transferEdgeCount = 0
          var noTransferEdgesCount = 0
          for(v <- stationVertices) {
//            println(s"LATLNG: ${v.location.lat}, ${v.location.long}")
            val extent =
              // vertex map coords are actually UTM, not lat\long
              Extent(
                v.location.long - transferToStationDistance,
                v.location.lat - transferToStationDistance,
                v.location.long + transferToStationDistance,
                v.location.lat + transferToStationDistance
              )

            val nearest = {
              val l = streetVertexIndex.pointsInExtent(extent)
              val (px, py) = (v.location.long, v.location.lat)
              if(l.isEmpty) { None }
              else {
                var n = l.head
                var minDist = {
                  val (x, y) = (n.location.long, n.location.lat)
                  streetVertexIndex.measure.distance(x, y, px, py)
                }
                for(t <- l.tail) {
                  val (x, y) = (t.location.long, t.location.lat)
                  val d = streetVertexIndex.measure.distance(px, py, x, y)
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
                val duration = {
                  val x = v.location.long - nearest.location.long
                  val y = v.location.lat - nearest.location.lat
                  Duration((math.sqrt(x*x + y*y) / Speeds.walking).toInt)
                }

                unpackedGraph.addEdge(v, WalkEdge(nearest, duration))
                unpackedGraph.addEdge(nearest, WalkEdge(v, duration))
                unpackedGraph.addEdge(v, BikeEdge(nearest, duration))
                unpackedGraph.addEdge(nearest, BikeEdge(v, duration))
                transferEdgeCount += 2
              case _ =>
                warn(s"NO TRANSFER EDGES CREATED FOR STATION ${v.name} at ${v.location}")
                noTransferEdgesCount += 1
            }
          }
          info(s"   $transferEdgeCount tranfer edges created")
          if(noTransferEdgesCount > 0) {
            warn(s"THERE WERE $noTransferEdgesCount STATIONS WITH NO TRANSFER EDGES.")
          }
        }


        val graph =
          Timer.timedTask("Packed graph") {
            TransitGraph.pack(unpackedGraph)
          }

        val index =
          Timer.timedTask("Created spatial index") {
            SpatialIndex(0 until graph.vertexCount) { v =>
              val l = graph.location(v)
//              println(s"${(l.long, l.lat)}")
              (l.long,l.lat)
            }
          }

        val extent = {
          println(regionBoundary)
          regionBoundary.envelope.buffer(travelshedExtentBuffer)
        }
        val cols = (extent.width / resolution).toInt
        val rows = (extent.height / resolution).toInt

        val rasterExtent = RasterExtent(extent, cols, rows)

        TravelshedGraph(graph, index, rasterExtent, startTime, duration)
      }
    }
  }
}
