package com.azavea.opentransit.indicators

trait IndicatorCalculation {
  type Intermediate // The indicator intermediate result

  def reduce(results: Seq[Intermediate]): Double
}

trait PerTripIndicatorCalculation extends IndicatorCalculation {
  def map(trips: Seq[Trip]): Intermediate
}

trait PerTripIndicatorCalculation {
  def map(trip: Trip): Intermediate
}

object IndicatorComputer {
  def apply(indicator: Indicator, periods: Seq[SamplePeriod], gtfsData: GtfsData): Seq[IndicatorResult] = {
    val periodsToResults: Map[SamplePeriod, PeriodIndicatorResults] = 
      periods.map { period =>
        val system = PeriodFilter(gtfsData, period.start, period.start)
      }

  }
}

object PeriodFilter {
  def apply(gtfsData: GtfsData, start: DateTime, end: DateTime): SystemForPeriod = {

  }
}

object ComputeIndicators {
  def apply() = {
    val perionResults: Map[Period, Map[Indicator, Map[Aggregation, Double]]] = 
      for(period <- periods) yield {
      val (routeAggregates, modeAggregates, serviceAggregates) =
        Indicators.map { indicator =>
          val mapped: Map[Route, Seq[Double]] =
            indicator.calculation match {
              case perRoute: PerRouteIndicatorCalculation =>
                { route: Route => (route, Seq(calc.map(route.trips))) }
              case perTrip: PerTripIndicatorCalculation =>
                { route: Route => (route, route.trips.map(calc.map)) }
            }

          val aggregates = indicator.aggregates

          val routeAggregates = 
            if(aggregates.contains(RouteAggregate)) {
              intermediateResults
                .mapValues { results =>
                  indicator.calculation.reduce(results)
                 }
                .toMap
            } else {
              Map[Int, Double]()
            }

          val modeAggregates = 
            if(aggregates.contains(SubsystemAggregate)) {

              indicator.calcluation.reduce(

}

object NumStops extends Indicator 
                   with AggregatesByAll {
  val name = "num_stops"

  val calc = new PerTripIndicatorCalculation {
    type Intermediate = Seq[String]

    def map(trip: Trip) = {
      trip.stops.map(_.stop_id)
    }

    def reduce(stops: Seq[Seq[String]]) = 
      stops.flatten.distinct.count
  }
}
