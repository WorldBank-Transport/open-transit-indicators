package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import geotrellis.vector._

import com.github.nscala_time.time.Imports._
import org.joda.time.Seconds

object CalculateIndicators {
  /** Computes all indicators and shovels the IndicatorContainerGenerators to a function.
    * The sink funciton should be thread safe!
    */
  def apply(periods: Seq[SamplePeriod], params: IndicatorCalculationParams, gtfsRecords: GtfsRecords)(sink: Seq[ContainerGenerator] => Unit): Unit = {
    val builder = TransitSystemBuilder(gtfsRecords)
    val systemsByPeriod =
      periods.map { period =>
        (period, builder.systemBetween(period.start, period.end))
      }.toMap

    for(indicator <- Indicators.list(params)) {
      val (periodResults, periodGeometries) =
        periods
          .map { period =>
            val transitSystem = systemsByPeriod(period)
            val results = indicator(transitSystem)
            val geometries = SystemGeometries(transitSystem)
            (period, (results, geometries))
           }
          .toMap
          .separateMaps

      val overallResults: AggregatedResults =
        PeriodResultAggregator(periodResults)

      val overallGeometries: SystemGeometries =
        SystemGeometries.merge(periodGeometries.values.toSeq)

      val periodIndicatorResults: Seq[ContainerGenerator] =
        periods
          .map { period =>
            val (results, geometries) = (periodResults(period), periodGeometries(period))
            PeriodIndicatorResult.createContainerGenerators(indicator.name, period, results, geometries)
           }
          .toSeq
          .flatten

      val overallIndicatorResults: Seq[ContainerGenerator] =
        OverallIndicatorResult.createContainerGenerators(indicator.name, overallResults, overallGeometries)

      sink(periodIndicatorResults ++ overallIndicatorResults)
    }
  }
}
