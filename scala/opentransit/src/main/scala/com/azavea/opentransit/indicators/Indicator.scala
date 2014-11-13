package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import scala.collection.mutable
import com.azavea.opentransit.indicators.calculators._
import com.azavea.opentransit.indicators.parameters._
import com.azavea.opentransit.indicators.travelshed._

object Indicators {
  case class Requirements(requirements: Boolean*) { val fulfilled = requirements.foldLeft(true)(_ && _) }
  implicit class RequiresWrapper[T](val indicator: T) { 
    def requires(requirements: Boolean*): (T, Requirements) = (indicator, Requirements(requirements: _*))
   }
  implicit def indicatorWithRequirements[T](indicator: T): (T, Requirements) = (indicator, Requirements())

  def list(params: IndicatorParams): List[Indicator] = {
    val settings = params.settings

    List[(Indicator, Requirements)]( 
      AverageServiceFrequency,
      Length,
      NumRoutes,
      NumStops,
      TimeTraveledStops,
      InterstopDistance,
      StopsToLength,
      new RatioLinesRoads(params) requires settings.hasOsm,
      new CoverageRatioStopsBuffer(params) requires settings.hasCityBounds,
      new TransitNetworkDensity(params) requires settings.hasRegionBounds,
      new HeadwayRegularity(params) requires settings.hasObserved,
      new TravelTimePerformance(params) requires settings.hasObserved,
      new OnTimePerformance(params) requires settings.hasObserved,
      new DwellTimePerformance(params) requires settings.hasObserved,
      new RatioSuburbanLines(params) requires settings.hasCityBounds,
      new AllWeightedServiceFrequency(params) requires settings.hasDemographics,
      new LowIncomeWeightedServiceFrequency(params) requires settings.hasDemographics,
      new AllAccessibility(params) requires settings.hasDemographics,
      new LowIncomeAccessibility(params) requires settings.hasDemographics,
      new Affordability(params)
    ).flatMap { case (indicator, requirements) => if(requirements.fulfilled) Some(indicator) else None }
  }

  def travelshedIndicators(params: IndicatorParams): List[TravelshedIndicator] = {
    val settings = params.settings

    List[(TravelshedIndicator, Requirements)]( 
      new JobsTravelshedIndicator(params) requires (
        settings.hasJobDemographics, 
        settings.hasOsm, 
        params.hasTravelshedGraph
      )
    ).flatMap { case (indicator, requirements) => if(requirements.fulfilled) Some(indicator) else None }
  }
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
