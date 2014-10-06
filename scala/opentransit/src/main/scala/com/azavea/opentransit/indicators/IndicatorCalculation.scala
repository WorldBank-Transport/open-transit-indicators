package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import geotrellis.vector._

import com.github.nscala_time.time.Imports._
import org.joda.time.Seconds

sealed trait IndicatorCalculation {
  def apply(transitSystem: TransitSystem): AggregatedResults
}

/** Indicator calculation that calculates intermediate values of type T */
sealed trait ReducingIndicatorCalculation[T] extends IndicatorCalculation{
  def reduce(results: Seq[T]): Double

  def apply(transitSystem: TransitSystem, aggregatesBy: Aggregate => Boolean): AggregatedResults =
    IndicatorCalculation.resultsForSystem(this, transitSystem, aggregatesBy)
}

trait PerRouteIndicatorCalculation[T] extends ReducingIndicatorCalculation[T] {
  def map(trips: Seq[Trip]): T
}

trait PerTripIndicatorCalculation[T] extends ReducingIndicatorCalculation[T] {
  def map(trip: Trip): T
}

object IndicatorCalculation {
  def resultsForSystem[T](calc: IndicatorCalculation[T], transitSystem: TransitSystem, aggregatesBy: Aggregate => Boolean): AggregatedResults = {
    // Indicators can either map a single trip or a set of trips
    // to an intermediate value. Create a mapping from each route
    // to a set of intermediate results based on the Indicator's
    // calculation behavior.
    val mapRouteToIntermediateResults: Route => Seq[T] =
      calc match {
        case perRoute: PerRouteIndicatorCalculation[T] =>
          { route: Route => Seq(perRoute.map(route.trips)) }
        case perTrip: PerTripIndicatorCalculation[T] =>
          { route: Route => route.trips.map(perTrip.map) }
      }

    // Get the intermediate results from the indicator calculation.
    val intermediateResults: Map[Route, Seq[T]] =
      transitSystem.routes
        .map { route =>
          (route, mapRouteToIntermediateResults(route))
         }
        .toMap
    // Aggregate by route by reducing the set of results for each route.
    val byRoute: Map[Route, Double] =
      if (aggregatesBy(RouteAggregate)) {
        intermediateResults.map { case (route, result) =>
          (route, calc.reduce(result))
        }.toMap
      } else {
        Map()
      }

    // Aggregate by RouteType by combining all the intermediate
    // results of routes that belong to the same RouteType, and then
    // reduce those groupings.
    val byRouteType: Map[RouteType, Double] =
      if (aggregatesBy(RouteTypeAggregate)) {
        intermediateResults
          .groupBy { case (route, results) => route.routeType }
          .map { case (route, results) => (route, results.values.flatten.toSeq) }.toMap
          .map { case (route, results) => (route, calc.reduce(results)) }.toMap
      } else {
        Map()
      }

    // Aggregate by the whole system by combining all the intermediate
    // results of all routes, and reducing that to a single result.
    val bySystem: Option[Double] =
      if (aggregatesBy(SystemAggregate)) {
        Some(calc.reduce(intermediateResults.values.flatten.toSeq))
      } else {
        None
      }

    AggregatedResults(byRoute, byRouteType, bySystem)
  }
}
