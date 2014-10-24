package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import scala.collection.mutable
import com.azavea.opentransit.indicators.calculators._
import com.azavea.opentransit.indicators.parameters._

object Indicators {
  // The case class in the second position of each tuple specifies requirements
  private def filteredIndicators(params: IndicatorParams): List[Indicator] = {
    case class Requires(requirements: Boolean*)
    val settings = params.settings

    List( // Tuples of requirements and params-requiring indicators
      (
        AverageServiceFrequency,
        Requires()
      ),
      (
        Length,
        Requires()
      ),
      (
        NumRoutes,
        Requires()
      ),
      (
        NumStops,
        Requires()
      ),
      (
        TimeTraveledStops,
        Requires()
      ),
      (
        InterstopDistance,
        Requires()
      ),
      (
        StopsToLength,
        Requires()
      ),
      (
        new RatioLinesRoads(params),
        Requires(settings.hasOsm)
      ),
      (
        new CoverageRatioStopsBuffer(params),
        Requires(settings.hasCityBounds)
      ),
      (
        new TransitNetworkDensity(params),
        Requires(settings.hasRegionBounds)
      ),
      (
        new HeadwayRegularity(params),
        Requires(settings.hasObserved)
      ),
      (
        new TravelTimePerformance(params),
        Requires(settings.hasObserved)
      ),
      (
        new OnTimePerformance(params),
        Requires(settings.hasObserved)
      ),
      (
        new DwellTimePerformance(params),
        Requires(settings.hasObserved)
      ),
      (
        new RatioSuburbanLines(params),
        Requires(settings.hasCityBounds)
      ),
      (
        new AllWeightedServiceFrequency(params),
        Requires(settings.hasDemographics)
      ),
      (
        new LowIncomeWeightedServiceFrequency(params),
        Requires(settings.hasDemographics)
      ),
      (
        new AllAccessibility(params),
        Requires(settings.hasDemographics)
      ),
      (
        new LowIncomeAccessibility(params),
        Requires(settings.hasDemographics)
      ),
      (
        new Affordability(params),
        Requires()
      )
    ).map { case (indicator: Indicator, reqs: Requires) =>
      if (reqs.requirements.foldLeft(true)(_ && _)) Some(indicator) else None
    }.flatten
  }


  def list(params: IndicatorParams): List[Indicator] =
    filteredIndicators(params)
}

trait Indicator { self: AggregatesBy =>
  val name: String
  def calculation(period: SamplePeriod): IndicatorCalculation

  def aggregatesBy(aggregate: Aggregate) =
    self.aggregates.contains(aggregate)

  def perTripCalculation[T](mapFunc: Trip => T, reduceFunc: Seq[T] => Double): IndicatorCalculation =
    new PerTripIndicatorCalculation[T] {
      def apply(system: TransitSystem): AggregatedResults =
        apply(system, Indicator.this.aggregatesBy _)

      def map(trip: Trip): T = mapFunc(trip)
      def reduce(results: Seq[T]): Double = reduceFunc(results)
    }

  def perRouteCalculation[T](mapFunc: Seq[Trip] => T, reduceFunc: Seq[T] => Double): IndicatorCalculation =
    new PerRouteIndicatorCalculation[T] {
      def apply(system: TransitSystem): AggregatedResults =
        apply(system, Indicator.this.aggregatesBy _)

      def map(trips: Seq[Trip]): T = mapFunc(trips)
      def reduce(results: Seq[T]): Double = reduceFunc(results)
    }

  def perSystemCalculation(calculate: TransitSystem => AggregatedResults) =
    new IndicatorCalculation {
      def apply(system: TransitSystem) = calculate(system)
    }
}
