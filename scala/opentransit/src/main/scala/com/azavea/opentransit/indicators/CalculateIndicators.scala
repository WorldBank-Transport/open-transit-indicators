package com.azavea.opentransit.indicators

import scala.collection.mutable
import scala.slick.jdbc.JdbcBackend.{Database, Session, DatabaseDef}

import com.azavea.gtfs._
import com.azavea.gtfs.Timer.timedTask
import com.azavea.opentransit.JobStatus
import com.azavea.opentransit.JobStatus._
import com.azavea.opentransit.indicators.parameters._
import com.azavea.opentransit.indicators.WeeklyServiceHours._
import geotrellis.vector._
import com.azavea.opentransit.indicators.parameters._

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
  def apply(request: IndicatorCalculationRequest, gtfsRecords: GtfsRecords, db: DatabaseDef,
    statusManager: CalculationStatusManager): Unit = {
    val periods = request.samplePeriods
    val builder = TransitSystemBuilder(gtfsRecords)
    val systemsByPeriod =
      periods.map { period =>
        (period, builder.systemBetween(period.start, period.end))
      }.toMap

    val params = IndicatorParams(request, systemsByPeriod, db)

    // Helper for tracking indicator calculation status
    val trackStatus = {
      val status = mutable.Map[String, JobStatus]() ++
        Indicators.list(params).map(indicator => (indicator.name, JobStatus.Submitted))

      (indicatorName: String, newStatus: JobStatus) => {
        status(indicatorName) = newStatus
        statusManager.statusChanged(status.toMap)
      }
    }

    val periodGeometries = periods.map { period =>
      period -> SystemGeometries(systemsByPeriod(period))
    }.toMap

    val overallGeometries: SystemGeometries = SystemGeometries.merge(periodGeometries.values.toSeq)

    for(indicator <- Indicators.list(params)) {
      try {
        timedTask(s"Processed indicator: ${indicator.name}") {
          println(s"Calculating indicator: ${indicator.name}")
          trackStatus(indicator.name, JobStatus.Processing)

          val periodResults =
            periods
              .map { period =>
              val calculation =
                indicator.calculation(period)
              val transitSystem = systemsByPeriod(period)
              val results = calculation(transitSystem)
              (period, results)
            }
            .toMap

          val overallResults: AggregatedResults = PeriodResultAggregator(periodResults)

          val periodIndicatorResults: Seq[ContainerGenerator] =
            periods
              .map { period =>
              val (results, geometries) = (periodResults(period), periodGeometries(period))
              PeriodIndicatorResult.createContainerGenerators(indicator.name,
                period,
                results,
                geometries)
            }
            .toSeq
            .flatten

          val overallIndicatorResults: Seq[ContainerGenerator] =
            OverallIndicatorResult.createContainerGenerators(indicator.name,
              overallResults,
              overallGeometries)

          statusManager.indicatorFinished(periodIndicatorResults ++ overallIndicatorResults)
          trackStatus(indicator.name, JobStatus.Complete)
        }
      } catch {
        case e: Exception => {
          println(e.getMessage)
          println(e.getStackTrace.mkString("\n"))
          trackStatus(indicator.name, JobStatus.Failed)
        }
      }
    }

    println("Done processing periodic indicators; going to calculate weekly service hours...")
    timedTask("Processed indicator: hours_service") {
      WeeklyServiceHours(periods, builder, overallGeometries, statusManager, trackStatus) }
    println("Done processing indicators in CalculateIndicators")
  }
}
