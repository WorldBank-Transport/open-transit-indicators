package com.azavea.opentransit.indicators.stations

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators.parameters._
import com.azavea.opentransit.database._

import geotrellis.raster.interpolation._

import spire.syntax.cfor._
import com.typesafe.config.{ConfigFactory, Config}

object CalculateStationStats {

  def calculate(
    bufferRadius: Double,
    commuteTime: Int,
    interpolator: Interpolation
  ): Unit = {
    // New CSV
    val csv = StationStatsCSV(bufferRadius, commuteTime)

    // Configuration
    val config = ConfigFactory.load
    val dbName = config.getString("database.name")
    val dbGeomNameUtm = config.getString("database.geom-name-utm")
    val dbi = new ProductionDatabaseInstance {}

    // Top level vals
    val db = dbi.dbByName(dbName)
    val stops: Seq[Stop] = db withSession { implicit session =>
      GtfsRecords.fromDatabase(dbGeomNameUtm).stops
    }
    val allDemographics: List[Demographic] = db withSession { implicit session =>
      DemographicsTable.allDemographics
    }

    cfor(0)(_ < stops.size, _ + 1) { stopId =>
      val workingStop = stops(stopId)
      val stopBuff = workingStop.point.geom.buffer(bufferRadius)
      val nearbyDemographics: Seq[Demographic] =
        allDemographics.filter { d: Demographic => stopBuff.contains(d.geom) }

      // num dem1 within x meters of station
      val bufferedDemog1: Int =
        nearbyDemographics.foldLeft(0)(_ + _.populationMetric1.toInt)

      // num dem2 within x meters of station
      val bufferedDemog2: Int =
        nearbyDemographics.foldLeft(0)(_ + _.populationMetric2.toInt)

      // num jobs within x meters of station
      val bufferedDest1: Int =
        nearbyDemographics.foldLeft(0)(_ + _.destinationMetric1.toInt)

      // num jobs accessible from station within x minutes
      val reachableJobs: Int =
        interpolator.interpolate(workingStop.point.x, workingStop.point.y)

      csv.StationStats(
        workingStop.id,
        workingStop.name,
        bufferedDemog1,
        bufferedDemog2,
        bufferedDest1,
        reachableJobs
      ).write()
    }
    csv.save(Success)
  }
}
