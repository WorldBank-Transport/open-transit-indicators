package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import geotrellis.vector._

object CalculateIndicators {
  // We always want a MultiLine from a MultiLine.union() call
  implicit def multiLineResultToMultiLine(result: MultiLineMultiLineUnionResult): MultiLine =
    result match {
      case MultiLineResult(ml) => ml
      case LineResult(l) => MultiLine(l)
      case NoResult => MultiLine()
    }

  private def tupleTranspose[T1, T2, T3](tuples: Seq[(T1, T2, T3)]): (Seq[T1], Seq[T2], Seq[T3]) = {
    import scala.collection.mutable
    val l1 = mutable.ListBuffer[T1]()
    val l2 = mutable.ListBuffer[T2]()
    val l3 = mutable.ListBuffer[T3]()

    for((e1, e2, e3) <- tuples) {
      l1 += e1
      l2 += e2
      l3 += e3
    }

    (l1.toSeq, l2.toSeq, l3.toSeq)
  }

  private def combineMaps[TKey, TValue](m: Seq[Map[TKey, TValue]]): Map[TKey, Seq[TValue]] = {
    m.map(_.toSeq).flatten.groupBy(_._1).mapValues(_.map(_._2))
  }

  private def calculateOverall(values: Seq[Double]) =
    values.foldLeft(0.0)(_ + _) / (24 * 7)

  private def calculateOverall[T](maps: Seq[Map[T, Double]]): Map[T, Double] =
    combineMaps(maps).mapValues(calculateOverall(_))

  /** Computes all indicators and shovels the IndicatorContainerGenerators to a function.
    * The sink funciton should be thread safe! 
    */
  def apply(periods: Seq[SamplePeriods], gtfsRecords: GtfsRecords)(sink: Seq[IndicatorContainerGenerator] => Unit): Unit = {
    val builder = TransitSystemBuilder(gtfsRecords)
    val systemsByPeriod = periods.map { period => builder.systemBetween(period.start_time, period.end_time) }
    val indicators = Indicators.list

    for(indicator <- indicators) {
      computeIndicator(indicator, periods, systemsByPeriod)(sink)
    }
  }

  private def computeIndicator(indicator: Indicator, periods: Seq[SamplePeriods], systemsByPeriod: Map[Period, TransitSystem])
                              (sink: Seq[IndicatorContainerGenerator] => Unit)(implicit session: Session): Unit = {
    val calc = indicator.calc
    val aggregates = indicator.aggregates

    case class AggregatedResults(byRoute: Map[Route, Double], byRouteType: Map[RouteType, Double], bySystem: Option[Double])
    case class SystemGeometries(byRoute: Map[Route, Line], byRouteType: Map[RouteType, MultiLine], bySystem: MultiLine)

    val periodResults: Map[Period, (AggregatedResults, SystemGeometries)] =
      for(period <- periods) yield {
        val system = systemsByPeriod(period)

        // Indicators can either map a single trip or a set of trips
        // to an intermediate value. Create a mapping from each route
        // to a set of intermediate results based on the Indicator's 
        // calculation behavior.
        val mapRouteToIntermediateResults: Route => Seq[Double] =
          calc match {
            case perRoute: PerRouteIndicatorCalculation =>
              { route: Route => (route, Seq(calc.map(route.trips))) }
            case perTrip: PerTripIndicatorCalculation =>
              { route: Route => (route, route.trips.map(calc.map)) }
          }

        // Get the intermediate results from the indicator calculation.
        val intermediateResults: Map[Route, Seq[Double]] =
          system.routes
            .map { route => 
              (route, mapRouteToIntermediateResults(route))
            }
            .toMap

        // Aggregate by route by reducing the set of results for each route.
        val byRoute: Map[Route, Double] =
          if(aggregates.contains(RouteAggregate)) {
            intermediateResults.mapValues(calc.reduce(_))
          } else {
            Map()
          }

        // Aggregate by RouteType by combining all the intermediate
        // results of routes that belong to the same RouteType, and then
        // reduce those groupings.
        val byRouteType: Map[RouteType, Double] =
          if(aggregates.contains(ModeAggregate)) {
            intermediateResults
              .groupBy { case (route, results) => route.routeType }
              .mapValues(_.values.flatten)
              .mapValues(calc.reduce(_))
          } else {
            Map()
          }

        // Aggregate by the whole system by combining all the intermediate
        // results of all routes, and reducing that to a single result.
        val bySystem: Option[Double] =
          if(aggregates.contains(SystemAggregate)) {
            Some(calc.reduce(intermediateResults.values))
          } else {
            None
          }

        // Get all the geometries. TODO: Move this out of the indicator calcaultion logic.
        // Get the aggregate line for the route
        val routeToGeom =
          system.routes.map { route =>
            val line = MultiLine(route.trips.map(_.tripShape.line.geom)).union
            (route, line)
          }

        val routeTypeToGeom =
          routeToGeom
            .groupBy { case (route, line) => route.routeType }
            .mapValues(_.values.flatten)
            .mapValues { MutliLine(_).union }

        val systemGeom =
          MultiLine(routeTypeToGeom.values).union

        (period, (AggregateResults(byRoute, byRouteType, bySystem), SystemGeometries(routeToGeom, routeTypeToGeom, systemGeom)))
      }.toMap

    // Now that we have the result for each period,
    // calculate the results across the periods.

    // Set the period multipliers, which weights the results
    // based on the length of the period and whether the period
    // is on a weekday or weekend.
    val periodMultipliers: Map[Period, Double] =
      periods.map { period =>
        val hours =
          Hours.hoursBetween(period.period_start.toLocalDateTime, period.period_end.toLocalDateTime)

        // The multiplier is the number of hours * (2 if weekend, 5 otherwise).
        val dayMultiplier = if (period.`type` == "weekend") 2 else 5
        (period, dayMultiplier * hours)
      }.toMap

    // Weight each of the period result sets based on the periodMultiplier.
    // At this point we can lose the period information.
    val resultsWeightedByPeriod: Seq[(Map[Route, Double], Map[RouteType, Double], Option[Double])] =
      periodResults
        .map { case (period, (AggregatedResults(byRoute, byRouteType, bySystem), _)) =>
          val m = periodMultipliers(period)
          (byRoute.mapValues(_ * m),
           byRouteType.mapValues(_ * m),
           bySustem.map(_ * m)
          )
      }

    // Transpose the data from a sequence of result sets per period
    // to a sequence of result sets per result type.
    val (byRoutes, byRouteTypes, bySystems) =
      tupleTranspose(resultsWeightedByPeriod)

    val overallByRoute: Map[Route, Double] =
      calculateOverall(byRoutes)

    val overallByRouteType: Map[RouteType, Double] =
      calculateOverall(byRouteType)

    val overallBySystem: Double =
      calcaulteOverall(bySystems)

    // Calculate overall geometries

    val (overallRouteGeom, overallRouteTypeGeom, overallSystemGeom) = {
      val (byRoutes, byRouteTypes, bySystems) =
        tupleTranspose(
          periodResults
            .values
            .map(_._2)
        )

      val overallRouteGeom =
        combineMaps(byRoutes).mapValues { lines =>
          MultiLine(lines).union: MultiLine
        }

      val overallRouteTypeGeom =
        combineMaps(byRouteTypes).mapValues { ml =>
          ml.union: MultiLine
        }

      val overallSystemGeom =
        combineMaps(bySystems).mapValues { ml =>
          ml.union: MultiLine
        }
    }

    // Turn all the results into IndicatorResults by packing the appropriate Map key information
    // into an IndicatorResult instance.

    val periodIndicatorResults =
      periodResults.map { case (period, (byRoute, byRouteType, bySystem), geometries) =>
        val agg =
          byRoute.map { case (route, value) =>
            val line = geometries.byRoute(route)
            PeriodIndicatorResult(indicator.name, period, value).forRoute(route, line)
          } ++
          byRouteType.map { case (routeType, value) =>
            val multiLine = geometries.byRouteType(routeType)
            PeriodIndicatorResult(indicator.name, period, value).forRouteType(routeType, multiLine)
          }

        bySystem match {
          case Some(v) =>
            agg +: PeriodIndicatorResult(indicator.name, period, v).forSystem(geometries.bySystem)
          case None =>
            agg
        }
      } 

    val overallIndicatorResults =
      (overallByRoute.map { case (route, value) =>
        OverallIndicatorResult(indicator.name, value).forRoute(route, overallRouteGeom)
      } ++
      overallByRouteType.map { case (routeType, value) =>
        val multiLine = geometries.byRouteType(routeType)
        OverallIndicatorResult(indicator.name, value).forRouteType(routeType, overallRouteTypeGeom)
      }) +:
      OverallIndicatorResult(indicator.name, overallBySystem).forSystem(overallSystemGeom)

    sink(periodIndicatorRestuls ++ overallIndicatorResults)
  }
}
