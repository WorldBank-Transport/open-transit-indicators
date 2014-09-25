package com.azavea.opentransit.indicators

import com.azavea.opentransit._
import com.azavea.gtfs._

object NumRoutes extends Indicator 
                    with AggregatesByAll {
  type Intermediate = Int

  val name = "num_routes"

  val calculation =
    new PerRouteIndicatorCalculation[Int] {
      def map(trips: Seq[Trip]) =
        1

      def reduce(routes: Seq[Int]) =
        routes.foldLeft(0)(_ + _)
    }
}
