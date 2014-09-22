package com.azavea.opentransit.indicators

object IndicatorResultContainer {
  final val OVERALL_KEY = "alltime"
  final val SYSTEM_KEY = "system"
  final val MODE_KEY = "mode"
  final val ROUTE_KEY = "route"
}

/** This class is a container which will
    ship the indicator result to the Django service
    that saves off indicators. */
case class IndicatorResultContainer(
  `type`: String,
  sample_period: String,
  aggregation: String = "system",
  route_id: String = "",
  route_type: Int = 0,
  city_bounded: Boolean = false,
  version: String = "",
  value: Double = 0,
  the_geom: String = ""
) {
  def withGeom(geom: String): IndicatorResultContainer =
    IndicatorResultContainer(
      `type`,
      sample_period,
      aggregation,
      route_id,
      route_type,
      city_bounded,
      version,
      value,
      geom
    )

}

trait IndicatorResult { def toContainer(version: String): IndicatorResultContainer }

class PeriodRouteIndicatorResult(indicatorId: String, period: SamplePeriod, route: Route, value: Double) {
  def toContainer(version: String) = 
    IndicatorResultContainer(
      `type`= indicatorId,
      sample_period = period.`type`,
      aggregation=IndicatorResultContainer.ROUTE_KEY,
      route_id = route,
      value = value,
      version = request.version
    )
}

class PeriodModeIndicatorResult(indicatorId: String, period: SamplePeriod, modeId: Int, value: Double) {
  def toContainer(version: String) = 
    Indicator(
      `type` = name,
      sample_period = period.`type`,
      aggregation = IndicatorCalculator.MODE_KEY,
      route_type = mode,
      version = request.version,
      value = value
    )
}

case class PeriodSystemIndicatorResult(indicatorId: String, period: SampelPeriod, value: Double) {
  def toContainer(version: String) = 
    Indicator(
      `type` = name,
      sample_period = period.`type`,
      aggregation = IndicatorCalculator.SYSTEM_KEY,
      version = request.version,
      value = value
    )
}

case class PeriodIndicatorResults(byRoute: Seq[PeriodRouteIndicatorResult], byMode: Seq[PeriodModeIndicatorResult], bySystem: PeriodSystemIndicatorResult) {
  def toContainers(version: String): Seq[IndicatorContainer] =
    byRoute.map(_.toContainer(version)) ++
    byMode.map(_.toContainer(version)) +:
    bySystem.toContainer

}

class OverallRouteIndicatorResult(indicatorId: String, route: Route, value: Double) {
  def toContainer(version: String) = 
    IndicatorResultContainer(
      `type`= indicatorId,
      sample_overall = overall.`type`,
      aggregation=IndicatorResultContainer.ROUTE_KEY,
      route_id = route,
      value = value,
      version = request.version
    )
}

class OverallModeIndicatorResult(indicatorId: String, modeId: Int, value: Double) {
  def toContainer(version: String) = 
    Indicator(
      `type` = name,
      sample_overall = overall.`type`,
      aggregation = IndicatorCalculator.MODE_KEY,
      route_type = mode,
      version = request.version,
      value = value
    )
}

case class OverallSystemIndicatorResult(indicator: Indicator, value: Double) {
  def toContainer(version: String) = 
    Indicator(
      `type` = name,
      sample_period = IndicatorCalculator.OVERALL_KEY,
      aggregation = IndicatorCalculator.SYSTEM_KEY,
      version = request.version,
      value = value
    )
}

case class OverallIndicatorResults(byRoute: Seq[OverallRouteIndicatorResult], byMode: Seq[OverallModeIndicatorResult], bySystem: OverallSystemIndicatorResult) {
  def toContainers(version: String): Seq[IndicatorContainer] =
    byRoute.map(_.toContainer(version)) ++
    byMode.map(_.toContainer(version)) +:
    bySystem.toContainer
}

