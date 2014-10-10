package com.azavea.opentransit.indicators.calculators

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._

/** Indicator for average distance between stops */
object DistanceStops extends Indicator
                       with AggregatesByAll {
  type Intermediate = Double

  val name = "distance_stops"

  // for each route, get tuple of:
  // (sum of trip shape lengths, maximum number of stops in any trip)

  def calculation(period: SamplePeriod) = {
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
      if (count > 0) total / count else 0.0
    }
    perRouteCalculation(map, reduce)
  }
}
