package com.azavea.opentransit.indicators

import com.azavea.opentransit._
import com.azavea.gtfs._

object NumRoutes {
  val name = "num_routes"

  val calc =
    new PerRouteIndicatorCalculation {
      type Intermediate = Int

      def map(trips: Seq[Trip]) =
        1

      def reduce(routes: Seq[Int]) =
        routes.foldLeft(0)(_ + _)
    }
}

// Number of routes
// class NumRoutes(val gtfsData: GtfsData, val calcParams: CalcParams, val db: DatabaseDef) extends IndicatorCalculator {
//   val name = "num_routes"

//   def calcByRoute(period: SamplePeriod): Map[String, Double] = {
//     println("in calcByRoute for NumRoutes")
//     // this calculation isn't very interesting by itself, but when aggregated,
//     // it shows the average amount of time where the route is available at all.
//     routesInPeriod(period).map(route =>
//       (route.id.toString -> 1.0)
//     ).toMap
//   }

//   def calcByMode(period: SamplePeriod): Map[Int, Double] = {
//     println("in calcByMode for NumRoutes")
//     // get all routes, group by route type, and count the size of each group
//     routesInPeriod(period)
//       .groupBy(_.route_type.id)
//       .map { case (key, value) => key -> value.size.toDouble }
//   }

//   def calcBySystem(period: SamplePeriod): Double = simpleSumBySystem(period)
// }
