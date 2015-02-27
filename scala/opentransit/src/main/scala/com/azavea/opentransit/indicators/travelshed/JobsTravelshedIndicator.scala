package com.azavea.opentransit.indicators.travelshed

import scala.math.max

import com.azavea.opentransit._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.parameters._
import com.azavea.gtfs.Timer

import geotrellis.vector._
import geotrellis.vector.reproject._
import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.slick._
import geotrellis.proj4._

import geotrellis.network._
import geotrellis.network.graph._

import spire.syntax.cfor._
import grizzled.slf4j.Logging

/**
 * Build rasters for jobs accessibility by population.
 *
 * @param travelshedGraph the actual graph of jobs accessibility
 * @param regionDemographics the input demographic data
 * @param cacheId string uniquely identifying current indicator calculation set, used to key cached rasters
 */


object JobsTravelshedIndicator {
  val name = "jobs_travelshed"

  def writeToDatabase(result: Double,
                      overallLineGeoms: SystemLineGeometries,
                      statusManager: CalculationStatusManager): Unit = {
    val aggResults = AggregatedResults.systemOnly(result)
    val results = OverallIndicatorResult.createContainerGenerators("job_access",
      aggResults,
      overallLineGeoms)
    statusManager.indicatorFinished(results)
  }

  def run(travelshedGraph: TravelshedGraph,
          regionDemographics: RegionDemographics,
          cacheId: String,
          rasterCache: RasterCache,
          overallLineGeoms: SystemLineGeometries,
          statusManager: CalculationStatusManager): Unit = {

    val result = calculate(travelshedGraph, regionDemographics, cacheId, rasterCache)
    writeToDatabase(result, overallLineGeoms, statusManager)
  }

  def calculate(travelshedGraph: TravelshedGraph,
    regionDemographics: RegionDemographics,
    cacheId: String,
    rasterCache: RasterCache): Double = {
    val (graph, index, rasterExtent, arriveTime, duration, crs) = {
      val tg = travelshedGraph

      val arriveTime: Time = Time(tg.arriveTime)
      val duration: Duration = Duration(tg.duration)

      (tg.graph, tg.index, tg.rasterExtent, arriveTime, duration, tg.crs)
    }

    println(s"RUNNING JOB INDICATORS FOR ARRIVAL TIME $arriveTime WITH $duration TRAVEL TIME")

    val features: Array[JobsDemographics] =
      regionDemographics.jobsDemographics.toArray

    val (cols, rows) =
      (rasterExtent.cols, rasterExtent.rows)

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

    // Create the empty tile. This will be filled with the number of jobs
    // accessible from the tile
    val tile = ArrayTile.empty(TypeDouble, cols, rows)

    // Population Tile that will be combined with jobs data
    val populationTile = ArrayTile.empty(TypeDouble, cols, rows)

    // Create an index of the raster cells
    val mappedCoords =
      GridBounds(0, 0, tile.cols - 1, tile.rows - 1).coords.map { case (col, row) =>
        (col, row, rasterExtent.gridColToMap(col), rasterExtent.gridRowToMap(row))
      }
    val tileIndex =
      SpatialIndex(mappedCoords) { case (col, row, x, y) =>
        (x, y)
      }
    val cellArea = rasterExtent.cellwidth * rasterExtent.cellheight

    var totalJobs: Double = 0.0
    var totalPopulation: Double = 0.0
    cfor(0)(_ < features.size, _ + 1) { polyIndex =>
      val feature = features(polyIndex)
      val envelope = feature.geom.envelope
      totalJobs += feature.jobs
      totalPopulation += feature.population

      // Fill out the raster cells with the population
      val cellsContained = tileIndex.pointsInExtent(envelope).toArray
      val cellsContainedLen = cellsContained.size
      if(cellsContainedLen > 0) {
        val population = feature.populationPerArea * cellArea
        cfor(0)(_ < cellsContainedLen, _ + 1) { i =>
          val (col, row, x, y) = cellsContained(i)
          if(feature.geom.contains(x, y)) {
            populationTile.setDouble(col, row, population)
          }
        }
      }

      // Find the map of vertex -> polygonId and polygonId -> job count.
      val contained = index.pointsInExtent(envelope).toArray
      val containedLen = contained.size
      cfor(0)(_ < containedLen, _ + 1) { i =>
        val (v, x, y) = contained(i)
        if(feature.geom.contains(x, y)) {
          vertexToPolyId(v) = polyIndex
        }
      }
      polyIdToValue(polyIndex) = feature.jobs
    }

    // SPT parameters
    val maxDuration = duration.toInt
    val edgeTypes =
      Walking ::
        graph.transitEdgeModes
          .map(_.service)
          .toSet
          .map { s: String => ScheduledTransit(s, EveryDaySchedule) }.toList

    var totalJobAccessNumerator = 0.0

    println(s"Running shortest path query. $rasterExtent. $rows, $cols")
    Timer.timedTask(s"Created the jobs indicator tile") {
      cfor(0)(_ < rows, _ + 1) { row =>
    //    Timer.timedTask(s"  Ran for row $row") {
        cfor(0)(_ < cols, _ + 1) { col =>
          val polyHits = zeros.clone
          /**
            * Array containing departure times of the current shortest
            * path to the index vertex.
            */
          val shortestPathTimes = emptySptArray.clone


          // Find the nearest start vertex (TODO: Do time calcuation on travel to that vertex)
          val (x, y) = rasterExtent.gridToMap(col, row)
          val (startVertex, _, _) = index.nearest(x, y)
          val startPolyId = vertexToPolyId(startVertex)

          var sum =
            if(startPolyId != -1) {
              polyHits(startPolyId) = polyHit
              polyIdToValue(startPolyId)
            } else {
              0.0
            }
          // SHORTEST PATH CALCULATION

          shortestPathTimes(startVertex) = 0

          // dijkstra's

          val queue = new IntPriorityQueue(shortestPathTimes)

          val tripEnd = arriveTime.toInt
          val tripStart = tripEnd - maxDuration

          val edgeIterator =
            graph.getEdgeIterator(edgeTypes, EdgeDirection.Outgoing)


          edgeIterator.foreachEdge(startVertex, tripStart) { (target,weight) =>
            val t = tripStart + weight
            if(t <= tripEnd) {
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
              if(t <= tripEnd) {
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

          if(sum > 0) {
            val population = populationTile.getDouble(col, row) match {
              case tile if tile.isNaN => 0.0
              case tile => tile
            }
            val numerator = sum * population
            totalJobAccessNumerator += numerator
            val result = numerator / totalJobs
            tile.setDouble(col, row, result)
          } else {
            tile.setDouble(col, row, Double.NaN)
          }
        }
      }
    }

    // Reproject
    println(s"Reprojecting extent ${rasterExtent.extent} to WebMercator.")
    val (rTile, rExtent) =
      tile.reproject(rasterExtent.extent, crs, WebMercator)

    println(s"Setting result of job indicator calculation to raster-cache-key $cacheId")
    rasterCache.set(RasterCacheKey(JobsTravelshedIndicator.name + cacheId), (rTile, rExtent))

    val regionalJobAccess = totalJobAccessNumerator / totalPopulation
    regionalJobAccess
  }
}
