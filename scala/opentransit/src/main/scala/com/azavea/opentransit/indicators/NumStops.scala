package com.azavea.opentransit.indicators

import com.azavea.gtfs._
import com.azavea.opentransit._

object NumStops extends Indicator
                   with AggregatesByAll {
  type Intermediate = Seq[Stop]

  val name = "num_stops"

  def calculation(period: SamplePeriod): IndicatorCalculation = {
    def map(trip: Trip) =
      trip.schedule.map(_.stop)

    def reduce(stops: Seq[Seq[Stop]]) =
      stops.flatten.distinct.size

    perTripCalculation(map, reduce)
  }

  // NOTE: This is different than before the Indicator refactor. The previous
  // version didn't count distinct stops across trips, but counted the stops
  // in the trip of a route with the maximum number of stops. If the latter
  // is the correct way to do it (which I don't belive it is), this code will
  // do:
    // new PerRouteIndicatorCalculation[Seq[Stop]] {
    //   def map(trips: Seq[Trip]) = {
    //     val trip = trips.reduce { (trip1, trip2) => if(trip1.schedule.size > trip2.schedule.size) trip1 else trip2 }
    //     trip.schedule.map(_.stop)
    //   }

    //   def reduce(stops: Seq[Seq[Stop]]) =
    //     stops.flatten.distinct.size
    // }
}
