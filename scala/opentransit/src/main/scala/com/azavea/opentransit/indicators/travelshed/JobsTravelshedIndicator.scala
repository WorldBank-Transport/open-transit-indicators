package com.azavea.opentransit.indicators.travelshed

import com.azavea.opentransit._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.parameters._
import com.azavea.gtfs.Timer

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.slick._

import geotrellis.network._
import geotrellis.network.graph._

import spire.syntax.cfor._
import grizzled.slf4j.Logging

trait TravelshedIndicator {
  def name: String

  def apply(rasterCache: RasterCache): Unit
}

object JobsTravelshedIndicator {
  val name = "jobs"
}

class JobsTravelshedIndicator(params: HasTravelshedGraph with RegionDemographics) extends TravelshedIndicator with Logging {
  def name = JobsTravelshedIndicator.name

  def apply(rasterCache: RasterCache): Unit = {
    val (graph, index, rasterExtent, startTime, duration) = {
      val tg = params.travelshedGraph.get

      val startTime: Time = Time(tg.startTime)
      val duration: Duration = Duration(tg.duration)

      (tg.graph, tg.index, tg.rasterExtent, startTime, duration)
    }

    val features: Array[MultiPolygonFeature[Double]] =
      params.jobsDemographics.toArray
        
    val (cols, rows) = (rasterExtent.cols, rasterExtent.rows)

    val vertexCount = graph.vertexCount
    val polyCount = features.size

    val vertexToPolyId = Array.ofDim[Int](vertexCount).fill(-1)
    val polyIdToValue = Array.ofDim[Double](polyCount)

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
//      println(feature)
      cfor(0)(_ < containedLen, _ + 1) { i =>
//        println(s"WE HAVE A WINNER!")
        val v = contained(i)
        vertexToPolyId(v) = polyIndex
      }
      polyIdToValue(polyIndex) = feature.data
    }

    // SPT parameters
    val maxDuration = duration.toInt
    val edgeTypes = 
      Walking :: 
        graph.transitEdgeModes
          .map(_.service)
          .toSet
          .map { s: String => ScheduledTransit(s, EveryDaySchedule) }.toList

    val tile = ArrayTile.empty(TypeDouble, cols, rows)

  //  return

    info(s"Running shortest path query. $rasterExtent. $rows, $cols")
    Timer.timedTask(s"Created the jobs indicator tile") {
      cfor(0)(_ < rows, _ + 1) { row =>
    //    Timer.timedTask(s"  Ran for row $row") {
          cfor(0)(_ < cols, _ + 1) { col =>
            // Find the nearest start vertex (TODO: Do time calcuation on travel to that vertex)
            val (lng, lat) = rasterExtent.gridToMap(col, row)
            val startVertex = index.nearest(lat, lng)

            var sum = 0.0
            val polyHits = zeros.clone

            // SHORTEST PATH CALCULATION

            /**
              * Array containing departure times of the current shortest
              * path to the index vertex.
              */
            val shortestPathTimes = emptySptArray.clone

            shortestPathTimes(startVertex) = 0

            // dijkstra's

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

            tile.setDouble(col, row, sum)
          }
        }
//      }
    }
    
    // Process the completed tile.
    rasterCache.set(RasterCacheKey(JobsTravelshedIndicator.name), (tile, rasterExtent.extent))
  }
}
