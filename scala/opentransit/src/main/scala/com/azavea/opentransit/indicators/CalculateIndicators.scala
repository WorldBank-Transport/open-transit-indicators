package com.azavea.opentransit.indicators

import scala.collection.mutable
import scala.slick.jdbc.JdbcBackend.{Database, Session, DatabaseDef}

import com.azavea.gtfs._
import com.azavea.gtfs.Timer.timedTask
import com.azavea.opentransit._
import com.azavea.opentransit.JobStatus._
import com.azavea.opentransit.JobStatusWithMessage
import com.azavea.opentransit.JobStatusWithMessage._
import com.azavea.opentransit.JobStatusType
import com.azavea.opentransit.JobStatusType._
import com.azavea.opentransit.indicators.parameters._
import com.azavea.opentransit.indicators.travelshed._
import com.azavea.opentransit.indicators.WeeklyServiceHours._
import com.azavea.opentransit.indicators.calculators._
import geotrellis.vector._
import geotrellis.slick._

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
    System.gc()
    try {
      println(s"Processing indicator ${indicator.name}")
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

  def runWeeklySvcHours(
    periods: Seq[SamplePeriod],
    builder: TransitSystemBuilder,
    overallLineGeoms: SystemLineGeometries,
    statusManager: CalculationStatusManager,
    calculateAllTime: Boolean,
    trackStatus: (String, JobStatus) => Unit
  ): Unit = {
    println(s"""Calculating indicators ${if (calculateAllTime) "with" else "without" } alltime.""")
    // This indicator only needs to be calculated when there's a full set of sample periods
    if (calculateAllTime) {
      println("Done processing periodic indicators; going to calculate weekly service hours...")
      timedTask("Processed indicator: hours_service") {
        WeeklyServiceHours(periods, builder, overallLineGeoms, statusManager, trackStatus) }
      println("Done processing indicators in CalculateIndicators")
    }
  }

  def runTravelshed(
    periods: Seq[SamplePeriod],
    builder: TransitSystemBuilder,
    request: IndicatorCalculationRequest,
    db: Database,
    trackStatus: (String, JobStatus) => Unit
  ): Unit = {
    // Run travelshed indicators
    val reqs = request.paramsRequirements
    // Alright, we needto decide whether or not the jobs field of demographics must be set for
    // any demographics indicators to be run or if a more granular approach to demographics data
    // is worth exploring IF SO TODO: set request.paramsRequirements.jobDemographics based upon whether
    // or not the demographics data has job information
    if(reqs.demographics && reqs.osm) {
      db withSession { implicit session: Session =>
        TravelshedGraph(
          periods,
          builder,
          100,  // TODO: How do we decide on the resolution?
          request.arriveByTime - request.maxCommuteTime,
          request.maxCommuteTime
        )
      } match {
        case Some(travelshedGraph) =>
          val indicator = new JobsTravelshedIndicator(travelshedGraph, RegionDemographics(db))
          val name = indicator.name
          trackStatus(name, JobStatus.Processing)
          try {
            println("Calculating travelshed indicator...")
            timedTask("Processed indicator: Travelshed") {
              indicator(Main.rasterCache)
              trackStatus(name, JobStatus.Complete)
            }
          } catch {
            case e: Exception =>
              println(e.getMessage)
              println(e.getStackTrace.mkString("\n"))

              trackStatus(name, JobStatus.Failed)
          }
        case None =>
          println("Could not create travelshed graph")
      }
    }
    System.gc()
  }

  def genSysGeom(
    system: TransitSystem
  ): SystemLineGeometries = {
    val periodGeometry: SystemLineGeometries =
        timedTask(s"Calculated system geometries.") {
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

  def satisfiedIndicators(
    request: IndicatorCalculationRequest,
    builder: TransitSystemBuilder,
    period: SamplePeriod,
    dbByName: String => Database
  ): Seq[String] = {
    val sys = builder.systemBetween(period.start, period.end)
    Indicators.list(IndicatorParams(request, sys, period, dbByName)).map(_.name)
  }

  def runAllCalculations(
    builder: TransitSystemBuilder,
    dbByName: String => Database,
    periods: Seq[SamplePeriod],
    request: IndicatorCalculationRequest,
    statusManager: CalculationStatusManager
  ): Unit = {
    // Each of these holds data collected over the course of iteration so that the GC
    // can remove as much as possible after each iteration
    val resultHolder = mutable.Map[String, mutable.Map[SamplePeriod, AggregatedResults]]()
    val allBuffers = mutable.Map[SamplePeriod, SystemBufferGeometries]()
    val periodGeoms = periods.map { period =>
      period ->  genSysGeom(builder.systemBetween(period.start, period.end))
    }.toMap
    val overallLineGeoms = SystemLineGeometries.merge(periodGeoms.values.toSeq)

    // Helper for tracking indicator calculation status
    val trackStatus = {
      val status = mutable.Map[String, JobStatus]() ++
        satisfiedIndicators(request, builder, periods.head, dbByName)
          .map(name => (name, JobStatus.Submitted))

      def sendStatus = statusManager.statusChanged(status.toMap)

      // Send initial status to quickly inform the UI what indicators are being calculated
      sendStatus

      (indicatorName: String, newStatus: JobStatus) => {
        status(indicatorName) = newStatus
        sendStatus
      }
    }

    val calculateAllTime = request.samplePeriods.length != periods.length
    // This iterator will run through all the periods, generating a system for each
    // The bulk of calculations are done here
    println("Now running travelshed")
    runTravelshed(periods, builder, request, dbByName(request.auxDbName), trackStatus)

    for (period <- periods) {
      println(s"Calculating indicators in period: ${period.periodType}...")
      val system = builder.systemBetween(period.start, period.end)
      val params = IndicatorParams(request, system, period, dbByName)
      allBuffers(period) = genSysBuffers(system, period, params)

      // Do the calculation
      for(indicator <- Indicators.list(params)) {
        trackStatus(indicator.name, JobStatus.Processing)
        singleCalculation(indicator, period, system, resultHolder)
      }
    }

    println("Calculating weekly service hours")
    runWeeklySvcHours(periods, builder, overallLineGeoms, statusManager, calculateAllTime, trackStatus)

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
            case _ => periodGeoms(period)
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
                                                           overallLineGeoms: SystemLineGeometries)
        statusManager.indicatorFinished(periodIndicatorResults ++ overallIndicatorResults)
      }
      trackStatus(indicatorName, JobStatus.Complete)
      statusManager.statusChanged(Map(indicatorName -> JobStatus.Complete))
    }
    System.gc()
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
