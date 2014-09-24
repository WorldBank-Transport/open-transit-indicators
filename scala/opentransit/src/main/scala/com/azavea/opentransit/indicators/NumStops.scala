package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

object NumStops extends Indicator
                   with AggregatesByAll {

  val name = "num_stops"

  val calculation =
    new PerTripIndicatorCalculation[Seq[Stop]] {
      def map(trip: Trip) =
        trip.schedule.map(_.stop)

      def reduce(stops: Seq[Seq[Stop]]) =
        stops.flatten.distinct.size
    }
}
