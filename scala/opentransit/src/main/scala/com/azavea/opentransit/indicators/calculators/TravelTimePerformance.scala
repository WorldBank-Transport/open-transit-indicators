package com.azavea.opentransit.indicators.calculators

import org.joda.time.Seconds

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.parameters._

import com.github.nscala_time.time.Imports._
import org.joda.time._

/**
* This indicator calculates the average deviation between the anticipated travel
* time (difference between some stop's departure and the next stop's arrival) as
* scheduled and actually observed (in minutes)
* Heavily commented and type signed because it is a fairly involved calculation
**/
class TravelTimePerformance(params: ObservedStopTimes)
    extends Indicator
      with AggregatesByAll {
  type Intermediate = Option[Double]

  val name = "travel_time"

  def calculation(period: SamplePeriod) = {
    // Grab a map from trip ID to the tuple of scheduled stop values and observed stop values
    val observedTrips: Map[String, Seq[(ScheduledStop, ScheduledStop)]] =
      params.observedStopsByTrip(period)

    def map(trip: Trip): Option[Seq[Double]] = {
      // Sort the tuples out (so that they might be accurately compared, one to the next)
      val (schedTimes, obsTimes): (Seq[ScheduledStop], Seq[ScheduledStop]) =
        observedTrips(trip.id).sortBy { case (sched, obsvd) =>
          sched.arrivalTime // Order stops by scheduled arrival time
        }.unzip // unzip so that we are left with two ordered lists - scheduled and observed stops

      if (!obsTimes.isEmpty && !schedTimes.isEmpty) {
        val schedTravelTimes: Seq[Double] = // Moving window with a width of 2 for scheduled times
          schedTimes.zip(schedTimes.tail).map { case (t1, t2) =>
            Seconds.secondsBetween(t1.departureTime, t2.arrivalTime).getSeconds.toDouble
          }

        val obsTravelTimes: Seq[Double] = // Moving window with a widht of 2 for observed times
          obsTimes.zip(obsTimes.tail).map { case (t1, t2) =>
            Seconds.secondsBetween(t1.departureTime, t2.arrivalTime).getSeconds.toDouble
          }

        // Zip the data up and map distance aggregation over that zipped data
        Some(schedTravelTimes.zip(obsTravelTimes).map { case (schedSeconds, obsSeconds) =>
          (schedSeconds - obsSeconds).abs
        }.toSeq)
      } else None
    }

    def reduce(timeDeltas: Seq[Option[Seq[Double]]]): Double = {
      val (total, count): (Double, Int) =
        timeDeltas
          .flatten // flatten out list
          .flatten // remove any None values from the flattened list
          .foldLeft((0.0, 0)) { case ((total, count), diff) =>
            (total + diff, count + 1) // fold for the following average operation
          }
      if (count > 0) (total / count) / 60 else 0.0
    }
    perTripCalculation(map, reduce)
  }

  def travelTimeDeviation(s1: ScheduledStop, s2: ScheduledStop): Double = {
    (Seconds.secondsBetween(s1.arrivalTime, s1.departureTime).getSeconds -
     Seconds.secondsBetween(s2.arrivalTime, s2.departureTime).getSeconds).abs.toDouble
  }

}
