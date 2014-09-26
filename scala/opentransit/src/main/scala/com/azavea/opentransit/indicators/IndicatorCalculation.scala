package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import geotrellis.vector._

import com.github.nscala_time.time.Imports._
import org.joda.time.Seconds

/** Indicator calculation that calculates intermediate values of type T */
trait IndicatorCalculation[T] {
  def reduce(results: Seq[T]): Double

  def apply(transitSystem: TransitSystem, aggregatesBy: Aggregate => Boolean): AggregatedResults =
    IndicatorCalculation.resultsForSystem(this, transitSystem, aggregatesBy)
}

trait PerRouteIndicatorCalculation[T] extends IndicatorCalculation[T] {
  def map(trips: Seq[Trip]): T
}

trait PerTripIndicatorCalculation[T] extends IndicatorCalculation[T] {
  def map(trip: Trip): T
}

case class AggregatedResults(byRoute: Map[Route, Double], byRouteType: Map[RouteType, Double], bySystem: Option[Double])

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
      if(aggregatesBy(RouteAggregate)) {
        intermediateResults.mapValues(calc.reduce(_))
      } else {
        Map()
      }

    // Aggregate by RouteType by combining all the intermediate
    // results of routes that belong to the same RouteType, and then
    // reduce those groupings.
    val byRouteType: Map[RouteType, Double] =
      if(aggregatesBy(RouteTypeAggregate)) {
        intermediateResults
          .groupBy { case (route, results) => route.routeType }
          .mapValues(_.values.flatten.toSeq)
          .mapValues(calc.reduce(_))
      } else {
        Map()
      }

    // Aggregate by the whole system by combining all the intermediate
    // results of all routes, and reducing that to a single result.
    val bySystem: Option[Double] =
      if(aggregatesBy(SystemAggregate)) {
        Some(calc.reduce(intermediateResults.values.flatten.toSeq))
      } else {
        None
      }

    AggregatedResults(byRoute, byRouteType, bySystem)
  }
}
