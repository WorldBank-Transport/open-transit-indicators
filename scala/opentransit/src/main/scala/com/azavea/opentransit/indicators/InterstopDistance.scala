package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

// This indicator calculates the average length of transit
// line between two stops
object InterstopDistance extends Indicator
                   with AggregatesByAll {
  type Intermediate = Seq[Double]

  val name = "distance_stops"

  def calculation(period: SamplePeriod) = {
    def map(trip: Trip): Seq[Double] = {
      val stops: Seq[Stop] = trip.schedule.map(_.stop)

      trip.schedule map {
        _.distanceTraveled match {
          // if distance traveled data exists, use it
          case Some(dt) =>
            dt
          // otherwise, use the trip shape length / # of stops - 1
          // This is a best-effort estimation, and should be improved
          // by modifying the GTFS parser to fill in missing
          // distanceTraveled values.
          case None =>
            trip.tripShape match {
              case Some(shape) =>
                if (stops.size > 1) (shape.line.length / (stops.size-1)) else 0.0
              case None =>
                0.0
            }
          }
        }
      }

    def reduce(stopRates: Seq[Seq[Double]]): Double = {
      val (total, count) =
        stopRates.flatten
      .foldLeft((0.0, 0)) {
        case ((headVal, count), tailVal) =>
          if (tailVal > 0.0)
            ((headVal + tailVal), (count + 1))
          else // no sense in counting zero distance traveled as another stop
            (headVal, count)
      }
      if (count > 0) total / count / 1000 else 0.0
    }
    perTripCalculation(map, reduce)
  }

}
