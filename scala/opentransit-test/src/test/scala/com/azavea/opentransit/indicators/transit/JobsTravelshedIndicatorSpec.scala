package com.azavea.opentransit.indicators.travelshed

import geotrellis.raster._
import geotrellis.vector._

import com.azavea.gtfs._

import com.azavea.opentransit._
import com.azavea.opentransit.testkit._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.calculators._
import com.azavea.opentransit.indicators.parameters._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ConfigFactory,Config}

import org.scalatest._
import org.scalatest.OptionValues._
import scala.collection.mutable

class MockRasterCache extends RasterCache {
  var raster: Option[(Tile, Extent)] = None

  def get(key: RasterCacheKey): Option[(Tile, Extent)] = raster
  def set(key: RasterCacheKey, value: (Tile, Extent)): Unit = { raster = Some(value) }
}


class JobsTravelshedIndicatorSpec
    extends FunSpec
    with Matchers {
  import TestGtfsRecords.stopLocations._

  // Set up a simple test

  //case class TravelshedGraph(graph: TransitGraph, index: SpatialIndex[Int], rasterExtent: RasterExtent, arriveTime: Int, duration: Int, crs: CRS)

  // def apply(
  //   periods: Seq[SamplePeriod],
  //   builder: TransitSystemBuilder,
  //   resolution: Double,
  //   arriveTime: Int,
  //   duration: Int
  // )


  // Subway weekday trips in the morning, afternoon and evening (which run on frequencies).
  // Morning = 5 AM - 10 AM  every 10 minutes
  // Afternoon = 10 AM - 4 PM  every 20 minutes
  // Evening = 4 PM - 11 PM  every 10 minutes
  // TripRecord("SUB_WEEKDAY_MORNING_EastWest", "SUB_WEEKDAYS", "EastWest", None, Some("SUB_EastWest_SHAPE")),
  // TripRecord("SUB_WEEKDAY_AFTERNOON_EastWest", "SUB_WEEKDAYS", "EastWest", None, Some("SUB_EastWest_SHAPE")),
  // TripRecord("SUB_WEEKDAY_EVENING_EastWest", "SUB_WEEKDAYS", "EastWest", None, Some("SUB_EastWest_SHAPE")),
  // TripRecord("SUB_WEEKDAY_MORNING_NorthSouth", "SUB_WEEKDAYS", "NorthSouth", None, Some("SUB_NorthSouth_SHAPE")),
  // TripRecord("SUB_WEEKDAY_AFTERNOON_NorthSouth", "SUB_WEEKDAYS", "NorthSouth", None, Some("SUB_NorthSouth_SHAPE")),
  // TripRecord("SUB_WEEKDAY_EVENING_NorthSouth", "SUB_WEEKDAYS", "NorthSouth", None, Some("SUB_NorthSouth_SHAPE")),

  describe("working with a simple system (just subway)") {
    val westJobs = 1.0
    val centerJobs = 2.0
    val eastJobs = 3.0

    val regionDemographics =
      new  RegionDemographics {
        def jobsDemographics: Seq[MultiPolygonFeature[Double]] = {
          val westRegion = 
            MultiPolygonFeature(
              MultiPolygon(Seq(stopWest.buffer(5.0))),
              westJobs
            )

          val centerRegion =
            MultiPolygonFeature(
              MultiPolygon(Seq(stopCenter.buffer(5.0))),
              centerJobs
            )

          val eastRegion =
            MultiPolygonFeature(
              MultiPolygon(Seq(stopEast.buffer(5.0))),
              eastJobs
            )

          Seq(
            westRegion,
            centerRegion,
            eastRegion
          )
        }
      }

    val roads = 
      List[Line](
        Line(stopWest, Point(stopWest.x - 3.0, stopWest.y - 3.0)),
        Line(stopCenter, Point(stopCenter.x - 0.5, stopCenter.y - 0.5)),
        Line(stopEast, Point(stopEast.x + 3.0, stopEast.y + 3.0))
      )

    val periods = Seq(
      SamplePeriod(
        1,
        "weekday",
        new LocalDateTime(2014,12,10, 0, 0),
        new LocalDateTime(2014,12,10, 11, 59)
      )
    )

    val builder = TransitSystemBuilder(TestGtfsRecords()).filterByRoute(Subway)

    val resolution = 2.0

    it("should only include the start vertex if you can't go anywhere") {
      for(p <- regionDemographics.jobsDemographics) {
        println(p.envelope)
      }
      val arriveTime = 9 * 60 * 60
      val duration = 1

      val travelshedGraph = TravelshedGraph(periods, builder, resolution, arriveTime, duration, roads).get
      for(i <- 0 until travelshedGraph.graph.vertexCount) {
        println(s"   VERTEX $i: ${travelshedGraph.graph.vertexFor(i)}")
      }
      println(s"VERTEX COUNT: ${travelshedGraph.graph.vertexCount}")

      val indicator = new JobsTravelshedIndicator(travelshedGraph, regionDemographics, "0")

      val rasterCache = new MockRasterCache
      indicator.apply(rasterCache)
      val (tile, extent) = rasterCache.get(RasterCacheKey("")).get
      val rasterExtent = RasterExtent(extent, tile.cols, tile.rows)
      
      val (colWest, rowWest) = rasterExtent.mapToGrid(stopWest.x, stopWest.y)
      val (colCenter, rowCenter) = rasterExtent.mapToGrid(stopCenter.x, stopCenter.y)
      val (colEast, rowEast) = rasterExtent.mapToGrid(stopEast.x, stopEast.y)

      println(tile.asciiDraw)
      tile.get(colWest, rowWest) should be (westJobs)
      tile.get(colCenter, rowCenter) should be (centerJobs)
      tile.get(colEast, rowEast) should be (eastJobs)

    }
  }
}
