package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.github.nscala_time.time.Imports._

import geotrellis.vector._
import geotrellis.slick._

import spray.json._

object IndicatorResultContainer {
  final val OVERALL_KEY = SamplePeriod(1, "alltime",
    new LocalDateTime("01-01-01T00:00:00.000"),
    new LocalDateTime("2014-05-01T08:00:00.000")
  )
}

/** This class is a container which will
    ship the indicator result to the Django service
    that saves off indicators. */
case class IndicatorResultContainer(
  indicatorId: String,
  samplePeriod: SamplePeriod,
  aggregation: Aggregate,
  value: Double,
  geom: Geometry,
  calculationJob: Int,
  routeId: String = "",
  routeType: Option[RouteType] = None,
  cityBounded: Boolean = false
)

trait ContainerGenerator {
  def toContainer(id: Int): IndicatorResultContainer
}
