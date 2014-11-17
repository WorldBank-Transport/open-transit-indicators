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
  /** Computes all indicators, and sends results and intermediate statuses to the
    * CalculationStatusManager object. The CalculationStatusManager methods should
    * be thread safe.
    */
  def apply(request: IndicatorCalculationRequest, gtfsRecords: GtfsRecords,
    dbByName: String => Database, statusManager: CalculationStatusManager): Unit = {

    // The alltime period needs special handling. If it's requested, process it separately.
    val periods = request.samplePeriods.filter(_.periodType != "alltime")
    val calculateAllTime = request.samplePeriods.length != periods.length
    println(s"""Calculating indicators ${if (calculateAllTime) "with" else "without" } alltime.""")

    val builder = TransitSystemBuilder(gtfsRecords)
    val systemsByPeriod =
      periods.map { period =>
        (period, builder.systemBetween(period.start, period.end))
      }.toMap

    // Injecting the builder is Bad. But I'm doing it anywa.
    // Params should be refactored. So that this isn't necessary.
    val params = IndicatorParams(request, systemsByPeriod, builder, dbByName)

    val indicators = Indicators.list(params)
    val travelshedIndicators = Indicators.travelshedIndicators(params)

    // Helper for tracking indicator calculation status
    val trackStatus = {
      val status = mutable.Map[String, JobStatus]() ++
        (indicators.map(_.name) ++ travelshedIndicators.map(_.name)).map((_, JobStatus.Submitted))

      def sendStatus = statusManager.statusChanged(status.toMap)

      // Send initial status to quickly inform the UI what indicators are being calculated
      sendStatus

      (indicatorName: String, newStatus: JobStatus) => {
        status(indicatorName) = newStatus
        sendStatus
      }
    }

    val periodGeometries = periods.map { period =>
      println(s"Creating System Geometries for period ${period.periodType}...")
      val systemGeometries =
        timedTask(s"Calculated system geometries for period ${period.periodType}.") {
          SystemLineGeometries(systemsByPeriod(period))
        }
      period -> systemGeometries
    }.toMap

    val periodBuffers = periods.map { period =>
      println(s"Creating stop buffer geometries for period ${period.periodType}...")
      val systemBuffers : Projected[MultiPolygon] =
        timedTask(s"Calculated system geometries for period ${period.periodType}.") {
          params.bufferForPeriod(period)
        }
      period -> SystemBufferGeometries(systemsByPeriod(period), systemBuffers)
    }.toMap

    // This is lazy so it isn't generated if not needed (i.e. in the case of no alltime period)
    lazy val overallLineGeometries = {
      println("Calculating overall system geometries...")
      timedTask("Calculated overall system geometries.") {
        SystemLineGeometries.merge(periodGeometries.values.toSeq)
      }
    }

    lazy val overallBufferGeometries = {
      println("Calculating overall buffer geometries...")
      timedTask("Calculated overall system buffer geometries") {
        SystemBufferGeometries.merge(periodBuffers.values.toSeq)
      }
    }

    // This indicator only needs to be calculated when there's a full set of sample periods
    if (calculateAllTime) {
      println("Done processing periodic indicators; going to calculate weekly service hours...")
      timedTask("Processed indicator: hours_service") {
        WeeklyServiceHours(periods, builder, overallLineGeometries, statusManager, trackStatus) }
      println("Done processing indicators in CalculateIndicators")
    }

    for(indicator <- Indicators.list(params)) {
      try {
        timedTask(s"Processed indicator: ${indicator.name}") {
          println(s"Calculating indicator: ${indicator.name}")
          trackStatus(indicator.name, JobStatus.Processing)

          val periodResults =
            periods
              .map { period =>
                val calculation = indicator.calculation(period)
                val transitSystem = systemsByPeriod(period)
                val results = calculation(transitSystem)
                (period, results)
              }
              .toMap

          val periodIndicatorResults: Seq[ContainerGenerator] =
            periods
              .map { period =>
                val (results, geometries) = (periodResults(period), indicator match {
                  case (_: CoverageRatioStopsBuffer | _: WeightedServiceFrequency | _: Accessibility) => periodBuffers(period)
                  case _ => periodGeometries(period)
                })
                PeriodIndicatorResult.createContainerGenerators(indicator.name,
                                                                period,
                                                                results,
                                                                geometries)
            }
            .toSeq
            .flatten

          if (!calculateAllTime) {
            statusManager.indicatorFinished(periodIndicatorResults)
          } else {
            val overallResults: AggregatedResults = PeriodResultAggregator(periodResults)
            val overallIndicatorResults: Seq[ContainerGenerator] =
              OverallIndicatorResult.createContainerGenerators(indicator.name,
                                                               overallResults,
                                                               overallLineGeometries)

            statusManager.indicatorFinished(periodIndicatorResults ++ overallIndicatorResults)
          }

          trackStatus(indicator.name, JobStatus.Complete)
        }
      } catch {
        case e: Exception => {
          println(e.getMessage)
          println(e.getStackTrace.mkString("\n"))
          trackStatus(indicator.name, JobStatusWithMessage(JobStatusType.Failed, e.getMessage))
        }
      }
    }
    // This indicator only needs to be calculated when there's a full set of sample periods
    if (calculateAllTime) {
      println("Done processing periodic indicators; going to calculate weekly service hours...")
      timedTask("Processed indicator: hours_service") {
        WeeklyServiceHours(periods, builder, overallLineGeometries, statusManager, trackStatus) }
      println("Done processing indicators in CalculateIndicators")
    }

    // Run travelshed indicators
    for(indicator <- travelshedIndicators) {
      val name = indicator.name
      trackStatus(name, JobStatus.Processing)
      try {
        indicator(Main.rasterCache)
        trackStatus(name, JobStatus.Complete)
      } catch {
        case e: Exception =>
          println(e.getMessage)
          println(e.getStackTrace.mkString("\n"))

          trackStatus(name, JobStatus.Failed)
      }
    }

    println("Done processing indicators in CalculateIndicators")
  }
}
