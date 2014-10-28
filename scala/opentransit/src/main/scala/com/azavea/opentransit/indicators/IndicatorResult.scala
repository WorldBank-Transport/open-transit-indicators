package com.azavea.opentransit.indicators

import com.azavea.gtfs._

import geotrellis.vector._

import spray.json._

object IndicatorResultContainer {
  final val OVERALL_KEY = "alltime"
}

/** This class is a container which will
    ship the indicator result to the Django service
    that saves off indicators. */
case class IndicatorResultContainer(
  indicatorId: String,
  samplePeriodType: String,
  aggregation: Aggregate,
  value: Double,
  geom: JsValue,
  calculationJob: Int,
  routeId: String = "",
  routeType: Option[RouteType] = None,
  cityBounded: Boolean = false
)

trait ContainerGenerator {
  def toContainer(id: Int): IndicatorResultContainer
}
