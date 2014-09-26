package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

/** Indicator for average distance between stops */
object DistanceStops extends Indicator
                       with AggregatesByAll {
  type Intermediate = Double

  val name = "distance_stops"

  val calculation =
    new PerRouteIndicatorCalculation[Double] {
     // for each route, get tuple of:
     // (sum of trip shape lengths, maximum number of stops in any trip)
      def map(trips: Seq[Trip]) = {
        val (total, maxStops, count) = 
          trips
            .map { trip =>
              (trip.schedule.size, trip.tripShape.map(_.line.length))
             }
            .foldLeft((0.0, 0, 0)) { case ((total, maxStops, count), (stopCount, length)) =>
              length match {
                case Some(l) => (total + l, math.max(maxStops, stopCount), count + 1)
                case None => (total, maxStops, count)
              }
            }

        ((total / 1000) / count) / (maxStops - 1)
      }

      def reduce(routeAverages: Seq[Double]) = {
        val (total, count) =
          routeAverages
            .foldLeft((0.0, 0)) { case ((total, count), value) =>
              (total + value, count + 1)
             }
        total / count
      }
    }
}
