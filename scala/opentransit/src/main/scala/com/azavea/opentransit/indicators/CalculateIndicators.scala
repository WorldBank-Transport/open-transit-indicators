package com.azavea.opentransit.indicators

import scala.collection.mutable
import scala.slick.jdbc.JdbcBackend.{Database, Session, DatabaseDef}

import com.azavea.gtfs._
import com.azavea.gtfs.Timer.timedTask
import com.azavea.opentransit.JobStatus
import com.azavea.opentransit.JobStatus._
import com.azavea.opentransit.indicators.parameters._
import com.azavea.opentransit.indicators.calculators._
import com.azavea.opentransit.indicators.WeeklyServiceHours._
import geotrellis.vector._
import geotrellis.slick._
import com.azavea.opentransit.indicators.parameters._

import com.github.nscala_time.time.Imports._
import org.joda.time.Seconds

import com.typesafe.config.{ConfigFactory, Config}

trait CalculationStatusManager {
  def indicatorFinished(containerGenerators: Seq[ContainerGenerator]): Unit
  def statusChanged(status: Map[String, JobStatus]): Unit
}

object CalculateIndicators {
  val config = ConfigFactory.load
  val dbGeomNameUtm = config.getString("database.geom-name-utm")

  // Run the calculation specified and store the resulting value inside a stateholder
  def singleCalculation(
    indicator: Indicator,
    period: SamplePeriod,
    system: TransitSystem,
    stateHolder: mutable.Map[String, mutable.Map[SamplePeriod, AggregatedResults]]
  ): Unit = {
    try {
      timedTask(s"Processed indicator ${indicator.name} in period ${period.periodType}") {
        stateHolder.getOrElseUpdate(indicator.name, mutable.Map())
        stateHolder(indicator.name)(period) = indicator.calculation(period)(system)
      }
    } catch {
      case e: Exception => {
        println(e.getMessage)
        println(e.getStackTrace.mkString("\n"))
      }
    }
  }

  def genSysGeom(system: TransitSystem,
    period: SamplePeriod,
    mergedGeoms: SystemLineGeometries
  ): SystemLineGeometries = {
    val periodGeometry: SystemLineGeometries =
        timedTask(s"Calculated system geometries for period ${period.periodType}.") {
          SystemLineGeometries(system)
        }
    periodGeometry
  } // NEED TO MERGE FOR MEMORY'S SAKE

  def genSysBuffers(
    system: TransitSystem,
    period: SamplePeriod,
    params: IndicatorParams
  ): SystemBufferGeometries = {
    println(s"Creating stop buffer geometries for period ${period.periodType}...")
    val systemBuffers : Projected[MultiPolygon] =
      timedTask(s"Calculated system geometries for period ${period.periodType}.") {
        params.bufferForPeriod(period)
      }
    SystemBufferGeometries(system, systemBuffers)
  }

  def runAllCalculations(
    builder: TransitSystemBuilder,
    dbByName: String => Database,
    periods: Seq[SamplePeriod],
    request: IndicatorCalculationRequest,
    statusManager: CalculationStatusManager
  ): Unit = {

    // Helper for tracking indicator calculation status
    val trackStatus = {
      val status = mutable.Map[String, JobStatus]()
      (indicatorName: String, newStatus: JobStatus) => {
        status(indicatorName) = newStatus
        statusManager.statusChanged(status.toMap)
      }
    }

    val calculateAllTime = request.samplePeriods.length != periods.length
    // Each of these holds data collected over the course of iteration so that the GC
    // can remove as much as possible after each iteration
    val resultHolder = mutable.Map[String, mutable.Map[SamplePeriod, AggregatedResults]]()
    val allBuffers = mutable.Map[SamplePeriod, SystemBufferGeometries]()
    var geomSoFar = new SystemLineGeometries(Map(), Map(), MultiLine.EMPTY)
    // This iterator will run through all the periods, generating a system for each
    // The bulk of calculations are done here
    for (period <- periods) {
      println(s"Calculating indicators in period: ${period.periodType}...")
      val system = builder.systemBetween(period.start, period.end)
      val params = IndicatorParams(request, system, period, dbByName)
      val periodGeometry = genSysGeom(system, period, geomSoFar)
      geomSoFar = SystemLineGeometries.merge(Seq(periodGeometry, geomSoFar))
      allBuffers(period) = genSysBuffers(system, period, params)

      // Do the calculation
      for(indicator <- Indicators.list(params)) {
        singleCalculation(indicator, period, system, resultHolder)
      }
    }

    println(s"""Calculating indicators ${if (calculateAllTime) "with" else "without" } alltime.""")
    // This indicator only needs to be calculated when there's a full set of sample periods
    if (calculateAllTime) {
      println("Done processing periodic indicators; going to calculate weekly service hours...")
      timedTask("Processed indicator: hours_service") {
        WeeklyServiceHours(periods, builder, geomSoFar, statusManager, trackStatus) }
      println("Done processing indicators in CalculateIndicators")
    }

    resultHolder.map { case (indicatorName, periodToResults) =>
      val periodIndicatorResults: Seq[ContainerGenerator] =
        periodToResults.map { case (period, result) =>
          val (results, geometries) = (result, indicatorName match {
            case (
              "coverage_ratio_stops_buffer" |
              "service_freq_weighted" |
              "service_freq_weighted_low" |
              "system_access" |
              "system_access_low"
            ) => allBuffers(period)
            case _ => geomSoFar
          })
          PeriodIndicatorResult.createContainerGenerators(indicatorName,
                                                          period,
                                                          results,
                                                          geometries)
        }
        .toSeq
        .flatten

      if (!calculateAllTime) {
        statusManager.indicatorFinished(periodIndicatorResults)
      } else {
        val overallResults: AggregatedResults = PeriodResultAggregator(periodToResults)
        val overallIndicatorResults: Seq[ContainerGenerator] =
          OverallIndicatorResult.createContainerGenerators(indicatorName,
                                                           overallResults,
                                                           geomSoFar: SystemLineGeometries)
        statusManager.indicatorFinished(periodIndicatorResults ++ overallIndicatorResults)
      }
    }
  }

  /** Computes all indicators, and sends results and intermediate statuses to the
    * CalculationStatusManager object. The CalculationStatusManager methods should
    * be thread safe.
    */
  def apply(
    request: IndicatorCalculationRequest,
    dbByName: String => Database,
    statusManager: CalculationStatusManager
  ): Unit = {


    // This is where GTFS Records are gathered
    val gtfsRecords =
      dbByName(request.gtfsDbName) withSession { implicit session =>
        GtfsRecords.fromDatabase(dbGeomNameUtm)
      }
    // The alltime period needs special handling. If it's requested, process it separately.
    val periods: Seq[SamplePeriod] = request.samplePeriods.filter(_.periodType != "alltime")
    val builder = TransitSystemBuilder(gtfsRecords)

    runAllCalculations(builder, dbByName, periods, request, statusManager)
  }
}
