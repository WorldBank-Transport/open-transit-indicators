package com.azavea.opentransit.indicators.calculators

import scala.util.{Try, Success, Failure}
import org.joda.time.Seconds

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.parameters._

import com.github.nscala_time.time.Imports._
import org.joda.time._

/**
* This indicator calculates the average deviation between
* arrival times predicted and actually observed (in minutes)
**/
class TravelTimePerformance(params: ObservedStopTimes)
    extends Indicator
      with AggregatesByAll {
  type Intermediate = Option[Double]

  val name = "travel_time"

  def calculation(period: SamplePeriod) = {
    val observedTrips: Map[String, Seq[(ScheduledStop, ScheduledStop)]] =
      params.observedStopsByTrip(period)

    def map(trip: Trip): Option[Seq[Double]] = {
      val (schedTimes, obsTimes) = observedTrips(trip.id).sortBy { case (sched, obsvd) =>
        sched.arrivalTime // Order stops by scheduled arrival time
      }.unzip

      Try { // Try monad here for zipping over the empty list throwing an error
        val schedTravelTimes =
          schedTimes.zip(schedTimes.tail).map { case (t1, t2) =>
            Seconds.secondsBetween(t1.arrivalTime, t2.arrivalTime).getSeconds.toDouble
          }

        val obsTravelTimes =
          obsTimes.zip(obsTimes.tail).map { case (t1, t2) =>
            Seconds.secondsBetween(t1.arrivalTime, t2.arrivalTime).getSeconds.toDouble
          }

        schedTravelTimes.zip(obsTravelTimes).map { case (schedSeconds, obsSeconds) =>
          (schedSeconds - obsSeconds).abs
        }.toSeq
      } match {
        case Success(avgTravelTimeDifference: Seq[Double]) => Some(avgTravelTimeDifference)
        case Failure(_) => None
      }
    }

    def reduce(timeDeltas: Seq[Option[Seq[Double]]]): Double = {
      val (total, count) =
        timeDeltas.flatten.flatten.foldLeft((0.0, 0)){ case ((total, count), diff) =>
            (total + diff, count + 1)
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
