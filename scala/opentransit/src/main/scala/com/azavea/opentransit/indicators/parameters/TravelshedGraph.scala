package com.azavea.opentransit.indicators.parameters

import com.azavea.opentransit.indicators._
import com.azavea.opentransit.database.RoadsTable
import com.azavea.gtfs._

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.network.graph._
import geotrellis.transit.loader._
import geotrellis.transit.loader.gtfs._
import geotrellis.transit.loader.osm._

import grizzled.slf4j.Logging

import scala.slick.jdbc.JdbcBackend.{Database, DatabaseDef, Session}

import com.typesafe.config.ConfigFactory

case class TravelshedParams(rasterExtent: RasterExtent, startTime: Int, duration: Int)

/**
 * Trait used for travelshed indicators.
 */
trait TravelshedGraph {
  def travelshedDirecotry = TravelshedGraph.travelshedDirectory

  def graph: TransitGraph
  def index: SpatialIndex[Int]


  def travelshedParams: TravelshedParams
}

object TravelshedGraph extends Logging {
  val config = ConfigFactory.load
  val travelshedExtentBuffer = config.getDouble("opentransit.boundaries.travelshed-extent-buffer")
  val travelshedDirectory = config.getString("opentransit.travelshed.directory")

  def params(regionBoundary: MultiPolygon, resolution: Double, startTime: Int, duration: Int): TravelshedParams = {
    val extent = regionBoundary.envelope.buffer(travelshedExtentBuffer)
    val cols = (extent.width / resolution).toInt
    val rows = (extent.height / resolution).toInt

    TravelshedParams(RasterExtent(extent, cols, rows), startTime, duration)
  }

  // TODO: Only run for weekday.
  def createGraph(systems: Map[SamplePeriod, TransitSystem])(implicit session: Session): (TransitGraph, SpatialIndex[Int]) = {
    // Require period types to be distinct
    if(systems.keys.map(_.periodType).toSeq.distinct.size != systems.keys.map(_.periodType).size) {
      sys.error(s"Period Types in a set of SamplePeriods must be distinct")
    }

    info("Creating transit graph")
    Timer.timedTask("Transit graph created") {
      val osmParsedResults: ParseResult = 
        Timer.timedTask("Parsed in OSM results") {
          OsmParser.parseLines(RoadsTable.allRoads)
        }

      val systemResults: Seq[ParseResult] =
        (systems.map { case (period, system) =>
          Timer.timedTask(s"Parsed in results for period ${period.periodType}") {
            // NOTE: periodType's must be distinct.
            GtfsDateParser.parse(period.periodType, system)
          }
        }).toSeq

      val mergedSystemResult: ParseResult = 
        Timer.timedTask("Parsed in merged systems") {
          val mergedSystem = TransitSystem.merge(systems.values.toSeq)
          GtfsDateParser.parse(IndicatorResultContainer.OVERALL_KEY, mergedSystem)
        }

      val ParseResult(unpackedGraph, _, _) = 
        Timer.timedTask("Merged parse results.") {
          (Seq(osmParsedResults, mergedSystemResult) ++ systemResults).reduce(_.merge(_))
        }

      val graph = 
        Timer.timedTask("Packed graph") {
          TransitGraph.pack(unpackedGraph)
        }

      val index = 
        Timer.timedTask("Created spatial index") {
          SpatialIndex(0 until graph.vertexCount) { v =>
            val l = graph.location(v)
            (l.lat,l.long)
          }
        }
      (graph, index)
    }
  }
}
