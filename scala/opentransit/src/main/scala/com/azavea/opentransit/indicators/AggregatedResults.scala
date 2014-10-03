package com.azavea.opentransit.indicators

import com.azavea.gtfs._

case class AggregatedResults(byRoute: Map[Route, Double], byRouteType: Map[RouteType, Double], bySystem: Option[Double])

object AggregatedResults {
  def systemOnly(value: Double): AggregatedResults =
    AggregatedResults(Map(), Map(), Some(value))
}
