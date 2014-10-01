package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import org.joda.time.Seconds

object PeriodResultAggregator {
  private def calculateOverall(values: Seq[Double]) =
    values.foldLeft(0.0)(_ + _) / (24 * 7)

  private def calculateOverall[T](maps: Seq[Map[T, Double]]): Map[T, Double] =
    maps.combineMaps.map { case (k, v) => (k, calculateOverall(v)) }.toMap

  def apply(periodResults: Map[SamplePeriod, AggregatedResults]): AggregatedResults = {
    val periods = periodResults.keys

    // Set the period multipliers, which weights the results
    // based on the length of the period and whether the period
    // is on a weekday or weekend.
    val periodMultipliers: Map[SamplePeriod, Double] =
      periods.map { period =>
        val hours =
          Seconds.secondsBetween(period.start, period.end).getSeconds / 60 / 60

        // The multiplier is the number of hours * (2 if weekend, 5 otherwise).
        val dayMultiplier = if (period.periodType == "weekend") 2 else 5
        (period, (dayMultiplier * hours).toDouble)
      }.toMap

    // Weight each of the period result sets based on the periodMultiplier.
    // At this point we can lose the period information.
    val resultsWeightedByPeriod: Seq[(Map[Route, Double], Map[RouteType, Double], Option[Double])] =
      periodResults
        .map { case (period, (AggregatedResults(byRoute, byRouteType, bySystem))) =>
          val m = periodMultipliers(period)
          (byRoute.map { case (route, value) => (route, value * m) }.toMap,
           byRouteType.map { case (routeType, value) => (routeType, value * m) }.toMap,
           bySystem.map(_ * m)
          )
         }
        .toSeq

    // Transpose the data from a sequence of result sets per period
    // to a sequence of result sets per result type.
    val (byRoutes, byRouteTypes, bySystems) =
      resultsWeightedByPeriod.transposeTuples

    val overallByRoute: Map[Route, Double] =
      calculateOverall(byRoutes)

    val overallByRouteType: Map[RouteType, Double] =
      calculateOverall(byRouteTypes)

    val overallBySystem: Option[Double] =
      bySystems.flatten.toList match {
        case Nil => None
        case results =>
          Some(calculateOverall(results))
      }

    AggregatedResults(overallByRoute, overallByRouteType, overallBySystem)
  }
}
