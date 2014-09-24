package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import geotrellis.vector._

import com.github.nscala_time.time.Imports._
import org.joda.time.Seconds

object CalculateIndicators {
  /** Computes all indicators and shovels the IndicatorContainerGenerators to a function.
    * The sink funciton should be thread safe! 
    */
  def apply(periods: Seq[SamplePeriod], gtfsRecords: GtfsRecords)(sink: Seq[ContainerGenerator] => Unit): Unit = {
    val builder = TransitSystemBuilder(gtfsRecords)
    val systemsByPeriod = 
      periods.map { period => 
        (period, builder.systemBetween(period.start, period.end)) 
      }.toMap

    for(calculate <- Indicators.calculations) {
      calculate(systemsByPeriod)(sink)
    }
  }
}

object CalculateIndicator {
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

  private def calculateOverall(values: Seq[Double]) =
    values.foldLeft(0.0)(_ + _) / (24 * 7)

  private def calculateOverall[T](maps: Seq[Map[T, Double]]): Map[T, Double] =
    maps.combineMaps.mapValues(calculateOverall(_))

  def apply[T](calc: IndicatorCalculation[T], systemsByPeriod: Map[SamplePeriod, TransitSystem], aggregatesBy: Aggregate => Boolean, name: String)
              (sink: Seq[ContainerGenerator] => Unit): Unit = {
    val periods = systemsByPeriod.keys

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

    case class AggregatedResults(byRoute: Map[Route, Double], byRouteType: Map[RouteType, Double], bySystem: Option[Double])
    case class SystemGeometries(byRoute: Map[Route, MultiLine], byRouteType: Map[RouteType, MultiLine], bySystem: MultiLine) {
      def toTuple = (byRoute, byRouteType, bySystem)
    }

    val periodResults: Map[SamplePeriod, (AggregatedResults, SystemGeometries)] =
      (for(period <- periods) yield {
        val system = systemsByPeriod(period)

        // Get the intermediate results from the indicator calculation.
        val intermediateResults: Map[Route, Seq[T]] =
          system.routes
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

        // Get all the geometries. TODO: Move this out of the indicator calcaultion logic.
        // Get the aggregate line for the route
        val routeToGeom: Map[Route, MultiLine] =
          system.routes
            .map { route =>
              val lines = 
                route.trips
                  .map { trip => trip.tripShape.map(_.line.geom) }
                  .flatten
              val multiLine: MultiLine = MultiLine(lines).union 
              (route, multiLine)
             }
            .toMap

        val routeTypeToGeom: Map[RouteType, MultiLine] =
          routeToGeom
            .groupBy { case (route, multiLine) => route.routeType }
            .mapValues { seq: Map[Route, MultiLine] => 
              val lines = seq.values.map(_.lines).flatten.toSeq
              MultiLine(lines).union: MultiLine
             }

        val systemGeom: MultiLine =
          MultiLine(routeTypeToGeom.values.map(_.lines).flatten.toSeq).union

        (period, (AggregatedResults(byRoute, byRouteType, bySystem), SystemGeometries(routeToGeom, routeTypeToGeom, systemGeom)))
      }).toMap

    // Now that we have the result for each period,
    // calculate the results across the periods.

    // Set the period multipliers, which weights the results
    // based on the length of the period and whether the period
    // is on a weekday or weekend.
    val periodMultipliers: Map[SamplePeriod, Double] =
      periods.map { period =>
        val hours =
          Seconds.secondsBetween(period.start, period.end).getSeconds * 60 * 60

        // The multiplier is the number of hours * (2 if weekend, 5 otherwise).
        val dayMultiplier = if (period.periodType == "weekend") 2 else 5
        (period, (dayMultiplier * hours).toDouble)
      }.toMap

    // Weight each of the period result sets based on the periodMultiplier.
    // At this point we can lose the period information.
    val resultsWeightedByPeriod: Seq[(Map[Route, Double], Map[RouteType, Double], Option[Double])] =
      periodResults
        .map { case (period, (AggregatedResults(byRoute, byRouteType, bySystem), _)) =>
          val m = periodMultipliers(period)
          (byRoute.mapValues(_ * m),
           byRouteType.mapValues(_ * m),
           bySystem.map(_ * m)
          )
         }
        .toSeq

    // Transpose the data from a sequence of result sets per period
    // to a sequence of result sets per result type.
    val (byRoutes, byRouteTypes, bySystems) =
      tupleTranspose(resultsWeightedByPeriod)

    val overallByRoute: Map[Route, Double] =
      calculateOverall(byRoutes)

    val overallByRouteType: Map[RouteType, Double] =
      calculateOverall(byRouteTypes)

    val overallBySystem: Double =
      calculateOverall(bySystems.map(_.getOrElse(0.0)))

    // Calculate overall geometries

    val (overallRouteGeom, overallRouteTypeGeom, overallSystemGeom) = {
      val (byRoutes, byRouteTypes, bySystems) =
        tupleTranspose(
          periodResults
            .values
            .map(_._2.toTuple)
            .toSeq
        )

      val overallRouteGeom =
        byRoutes
          .combineMaps
          .mapValues { multiLines =>
            val lines = multiLines.map(_.lines).flatten
            MultiLine(lines.toSeq).union: MultiLine
           }

      val overallRouteTypeGeom =
        byRouteTypes
          .combineMaps
          .mapValues { multiLines =>
            val lines = multiLines.map(_.lines).flatten
            MultiLine(lines.toSeq).union: MultiLine
           }

      val overallSystemGeom: MultiLine =
        MultiLine(
          bySystems
            .map(_.lines)
            .flatten
        ).union 

      (overallRouteGeom, overallRouteTypeGeom, overallSystemGeom)
    }

    // Turn all the results into IndicatorResults by packing the appropriate Map key information
    // into an IndicatorResult instance.

    val periodIndicatorResults: Seq[ContainerGenerator] =
      periodResults
        .map { case (period, (AggregatedResults(byRoute, byRouteType, bySystem), geometries)) =>
          val containersByRoute: Iterable[ContainerGenerator] =
            byRoute.map { case (route, value) =>
              val line = geometries.byRoute(route)
              PeriodIndicatorResult(name, period, value).forRoute(route, line)
            }

          val containersByRouteType: Iterable[ContainerGenerator] =
            byRouteType.map { case (routeType, value) =>
              val multiLine = geometries.byRouteType(routeType)
              PeriodIndicatorResult(name, period, value).forRouteType(routeType, multiLine)
            }

          val containerForSystem: Iterable[ContainerGenerator] =
            bySystem match {
              case Some(v) =>
                Seq(PeriodIndicatorResult(name, period, v).forSystem(geometries.bySystem))
              case None =>
                Seq()
            }
          Seq(containersByRoute, containersByRouteType, containerForSystem).flatten
         }
        .toSeq
        .flatten

    val overallIndicatorResults: Seq[ContainerGenerator] = {
      val containersByRoute: Iterable[ContainerGenerator] =
        overallByRoute.map { case (route, value) =>
          OverallIndicatorResult(name, value).forRoute(route, overallRouteGeom(route))
        }

      val containersByRouteType: Iterable[ContainerGenerator] =
        overallByRouteType.map { case (routeType, value) =>
          OverallIndicatorResult(name, value).forRouteType(routeType, overallRouteTypeGeom(routeType))
        }

      val containerForSystem: ContainerGenerator =
        OverallIndicatorResult(name, overallBySystem).forSystem(overallSystemGeom)

      Seq(containersByRoute, containersByRouteType).flatten :+ containerForSystem
    }

    sink(periodIndicatorResults ++ overallIndicatorResults)
  }
}
