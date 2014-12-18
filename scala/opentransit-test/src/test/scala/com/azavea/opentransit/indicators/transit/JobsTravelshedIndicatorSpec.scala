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

import spire.syntax.cfor._

class MockRasterCache extends RasterCache {
  var raster: Option[(Tile, Extent)] = None

  def get(key: RasterCacheKey): Option[(Tile, Extent)] = raster
  def set(key: RasterCacheKey, value: (Tile, Extent)): Unit = { raster = Some(value) }
}


class JobsTravelshedIndicatorSpec
    extends FunSpec
    with Matchers {
  import TestGtfsRecords.stopLocations._

  describe("working with a simple system (just subway)") {
    val westJobs = 1.0
    val centerJobs = 2.0
    val eastJobs = 3.0

    def regionDemographics(rasterExtent: RasterExtent) = {
      val cellArea = rasterExtent.cellwidth * rasterExtent.cellheight
      new  RegionDemographics {
        def jobsDemographics: Seq[JobsDemographics] = {
          val westRegion = {
            val poly = stopWest.buffer(5.0)

            JobsDemographics(
              MultiPolygon(Seq(poly)),
              westJobs,
              poly.area / cellArea
            )
          }

          val centerRegion = {
            val poly = stopCenter.buffer(5.0)

            JobsDemographics(
              MultiPolygon(Seq(poly)),
              centerJobs,
              poly.area / cellArea
            )
          }

          val eastRegion = {
            val poly = stopEast.buffer(5.0)
            JobsDemographics(
              MultiPolygon(Seq(poly)),
                eastJobs,
                poly.area / cellArea
            )
          }

          Seq(
            westRegion,
            centerRegion,
            eastRegion
          )
        }
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
      val arriveTime = 9 * 60 * 60
      val duration = 1

      val travelshedGraph = TravelshedGraph(periods, builder, resolution, arriveTime, duration, roads).get
      val rasterExtent = travelshedGraph.rasterExtent

      val indicator = new JobsTravelshedIndicator(travelshedGraph, regionDemographics(rasterExtent), "0")

      val rasterCache = new MockRasterCache
      indicator.apply(rasterCache)
      val (tile, _) = rasterCache.get(RasterCacheKey("")).get
      val extent = rasterExtent.extent
      
      val (colWest, rowWest) = rasterExtent.mapToGrid(stopWest.x, stopWest.y)
      val (colCenter, rowCenter) = rasterExtent.mapToGrid(stopCenter.x, stopCenter.y)
      val (colEast, rowEast) = rasterExtent.mapToGrid(stopEast.x, stopEast.y)

      tile.getDouble(colWest, rowWest) should be (westJobs)
      tile.getDouble(colCenter, rowCenter) should be (centerJobs)
      tile.getDouble(colEast, rowEast) should be (eastJobs)

    }

    it("should include everything going east to west for a long travel time") {

      val arriveTime = 9 * 60 * 60
      val duration = 10000

      val travelshedGraph = TravelshedGraph(periods, builder, resolution, arriveTime, duration, roads).get
      val rasterExtent = travelshedGraph.rasterExtent

      val indicator = new JobsTravelshedIndicator(travelshedGraph, regionDemographics(rasterExtent), "0")

      val rasterCache = new MockRasterCache
      indicator.apply(rasterCache)
      val (tile, _) = rasterCache.get(RasterCacheKey("")).get
      val extent = rasterExtent.extent
      
      val (colWest, rowWest) = rasterExtent.mapToGrid(stopWest.x, stopWest.y)
      val (colCenter, rowCenter) = rasterExtent.mapToGrid(stopCenter.x, stopCenter.y)
      val (colEast, rowEast) = rasterExtent.mapToGrid(stopEast.x, stopEast.y)

      tile.getDouble(colWest, rowWest) should be (westJobs)
      tile.getDouble(colCenter, rowCenter) should be (centerJobs + westJobs)
      tile.getDouble(colEast, rowEast) should be (eastJobs + centerJobs + westJobs)
    }
  }

  describe("working with a system and population variance (just subway east-west)") {
    val westJobs = 1.0
    val centerJobs = 2.0
    val eastJobs = 3.0

    def regionDemographics(rasterExtent: RasterExtent) = {
      val cellArea = rasterExtent.cellwidth * rasterExtent.cellheight
      new  RegionDemographics {
        def jobsDemographics: Seq[JobsDemographics] = {
          val westRegion = {
            val poly = stopWest.buffer(5.0)

            JobsDemographics(
              MultiPolygon(Seq(poly)),
              westJobs,
              poly.area
            )
          }

          val centerRegion = {
            val poly = stopCenter.buffer(5.0)

            JobsDemographics(
              MultiPolygon(Seq(poly)),
              centerJobs,
              poly.area
            )
          }

          val eastRegion = {
            val poly = stopEast.buffer(5.0)
            JobsDemographics(
              MultiPolygon(Seq(poly)),
                eastJobs,
                poly.area
            )
          }

          Seq(
            westRegion,
            centerRegion,
            eastRegion
          )
        }
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
      val arriveTime = 9 * 60 * 60
      val duration = 1

      val travelshedGraph = TravelshedGraph(periods, builder, resolution, arriveTime, duration, roads).get
      val rasterExtent = travelshedGraph.rasterExtent
      val cellArea = rasterExtent.cellwidth * rasterExtent.cellheight

      val indicator = new JobsTravelshedIndicator(travelshedGraph, regionDemographics(rasterExtent), "0")

      val rasterCache = new MockRasterCache
      indicator.apply(rasterCache)
      val (tile, _) = rasterCache.get(RasterCacheKey("")).get
      val extent = rasterExtent.extent
      
      val (colWest, rowWest) = rasterExtent.mapToGrid(stopWest.x, stopWest.y)
      val (colCenter, rowCenter) = rasterExtent.mapToGrid(stopCenter.x, stopCenter.y)
      val (colEast, rowEast) = rasterExtent.mapToGrid(stopEast.x, stopEast.y)

      tile.getDouble(colWest, rowWest) should be (westJobs / cellArea)
      tile.getDouble(colCenter, rowCenter) should be (centerJobs / cellArea)
      tile.getDouble(colEast, rowEast) should be (eastJobs / cellArea)

    }

    it("should include everything going east to west for a long travel time") {

      val arriveTime = 9 * 60 * 60
      val duration = 10000

      val travelshedGraph = TravelshedGraph(periods, builder, resolution, arriveTime, duration, roads).get
      val rasterExtent = travelshedGraph.rasterExtent
      val cellArea = rasterExtent.cellwidth * rasterExtent.cellheight

      val indicator = new JobsTravelshedIndicator(travelshedGraph, regionDemographics(rasterExtent), "0")

      val rasterCache = new MockRasterCache
      indicator.apply(rasterCache)
      val (tile, _) = rasterCache.get(RasterCacheKey("")).get
      val extent = rasterExtent.extent
      
      val (colWest, rowWest) = rasterExtent.mapToGrid(stopWest.x, stopWest.y)
      val (colCenter, rowCenter) = rasterExtent.mapToGrid(stopCenter.x, stopCenter.y)
      val (colEast, rowEast) = rasterExtent.mapToGrid(stopEast.x, stopEast.y)

      tile.getDouble(colWest, rowWest) should be (westJobs / cellArea)
      tile.getDouble(colCenter, rowCenter) should be ((centerJobs + westJobs) / cellArea)
      tile.getDouble(colEast, rowEast) should be ((eastJobs + centerJobs + westJobs) / cellArea)
    }

    it("should work with varying population features and include everything going east to west for a long travel time") {

      val arriveTime = 9 * 60 * 60
      val duration = 10000

      val travelshedGraph = TravelshedGraph(periods, builder, resolution, arriveTime, duration, roads).get
      val rasterExtent = travelshedGraph.rasterExtent
      val cellArea = rasterExtent.cellwidth * rasterExtent.cellheight

      val regionDemographics = {
        new  RegionDemographics {
          def jobsDemographics: Seq[JobsDemographics] = {
            val westRegion = {
              val poly = stopWest.buffer(5.0)

              JobsDemographics(
                MultiPolygon(Seq(poly)),
                westJobs,
                poly.area / 2
              )
            }

            val centerRegion = {
              val poly = stopCenter.buffer(5.0)

              JobsDemographics(
                MultiPolygon(Seq(poly)),
                centerJobs,
                poly.area / 3
              )
            }

            val eastRegion = {
              val poly = stopEast.buffer(5.0)
              JobsDemographics(
                MultiPolygon(Seq(poly)),
                eastJobs,
                poly.area / 4
              )
            }

            Seq(
              westRegion,
              centerRegion,
              eastRegion
            )
          }
        }
      }

      val indicator = new JobsTravelshedIndicator(travelshedGraph, regionDemographics, "0")

      val rasterCache = new MockRasterCache
      indicator.apply(rasterCache)
      val (tile, _) = rasterCache.get(RasterCacheKey("")).get
      val extent = rasterExtent.extent
      
      val (colWest, rowWest) = rasterExtent.mapToGrid(stopWest.x, stopWest.y)
      val (colCenter, rowCenter) = rasterExtent.mapToGrid(stopCenter.x, stopCenter.y)
      val (colEast, rowEast) = rasterExtent.mapToGrid(stopEast.x, stopEast.y)

      val wm =  2 / cellArea
      val cm =  3 / cellArea
      val em = 4 / cellArea

      tile.getDouble(colWest, rowWest) should be (wm * westJobs)
      tile.getDouble(colCenter, rowCenter) should be (cm * (westJobs + centerJobs))
      tile.getDouble(colEast, rowEast) should be (em * (westJobs + centerJobs + eastJobs))
    }
  }
}
