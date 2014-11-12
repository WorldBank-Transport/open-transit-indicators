package com.azavea.opentransit

import com.azavea.opentransit.database._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.parameters._
import com.azavea.opentransit.indicators.travelshed._
import com.azavea.gtfs._

import com.github.nscala_time.time.Imports._
import com.typesafe.config._

import geotrellis.vector._
import geotrellis.slick._
import scala.slick.jdbc.JdbcBackend.{Database, DatabaseDef, Session}

import com.azavea.opentransit.testkit._

/** Note: This is not a unit test that is run in the suite.
  * This is an integration test you can run with `test:run`
  * 
  * Requires that the OSM and GTFS data is loaded into the database.
  */
object TravelshedCreationTest {
  def main(args: Array[String]): Unit = {
    // val db = 
    //   Database.forURL(s"jdbc:postgresql:transit_indicators", driver = "org.postgresql.Driver",
    //     user = "transit_indicators", password = "transit_indicators")

    // val records = {
    //   val config = ConfigFactory.load
    //   val dbGeomNameUtm = config.getString("database.geom-name-utm")

    //   // Import the files into the database to do the reprojection.
    //   db withSession { implicit session =>
    //     GtfsRecords.fromDatabase(dbGeomNameUtm).force
    //   }
    // }
    // val systemBuilder = TransitSystemBuilder(records)

    // val periods =
    //   Seq(
    //     SamplePeriod(1, "night",
    //       new LocalDateTime("2014-04-07T00:00:00.000"),
    //       new LocalDateTime("2014-04-07T08:00:00.000")),

    //     SamplePeriod(1, "morning",
    //       new LocalDateTime("2014-04-07T08:00:00.000"),
    //       new LocalDateTime("2014-04-07T11:00:00.000")),

    //     SamplePeriod(1, "midday",
    //       new LocalDateTime("2014-04-07T11:00:00.000"),
    //       new LocalDateTime("2014-04-07T16:30:00.000")),

    //     SamplePeriod(1, "evening",
    //       new LocalDateTime("2014-04-07T16:30:00.000"),
    //       new LocalDateTime("2014-04-07T23:59:59.999")),

    //     SamplePeriod(1, "weekend",
    //       new LocalDateTime("2014-05-02T00:00:00.000"),
    //       new LocalDateTime("2014-05-02T23:59:59.999"))
    //   )

    // val systems =
    //   periods.map { period =>
    //     (period, systemBuilder.systemBetween(period.start, period.end))
    //   }.toMap

    // val params = 
    //   new HasTravelshedGraph with Boundaries with RegionDemographics {
    //     val (cityBoundary, regionBoundary) = 
    //       db withSession { implicit session =>
    //         val (cityId, regionId) = (1, 2)
    //         (Boundaries.cityBoundary(cityId),
    //          Boundaries.regionBoundary(regionId)
    //         )
    //       }
    //     val rd = RegionDemographics(db)
    //     def regionDemographics(featureFunc: RegionDemographic => MultiPolygonFeature[Double]): Seq[MultiPolygonFeature[Double]] = 
    //       rd.regionDemographics(featureFunc)

    //     val travelshedGraph = {
    //       db withSession { implicit session =>
    //         TravelshedGraph(
    //           systems.keys.toSeq,
    //           systemBuilder,
    //           regionBoundary,
    //           200,
    //           60 * 60 * 8,
    //           60 * 60
    //         )
    //       }
    //     }
    //   }

    // val indicator = new JobsTravelshedIndicator(params)

    try {
//      indicator.apply(Main.rasterCache)
      println(Main.rasterCache.initialCache)
      val (tile, extent) = Main.rasterCache.get(RasterCacheKey("jobs_travelshed")).get
      println(tile.findMinMax)
    } finally {
      Main.actorSystem.shutdown
    }
  }
}
