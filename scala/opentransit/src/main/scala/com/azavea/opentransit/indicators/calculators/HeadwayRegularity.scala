package com.azavea.opentransit.indicators.calculators

import org.joda.time.Seconds

import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.parameters._

/**
* This indicator calculates the average deviation between
* frequency predicted and actually observed (in minutes)
**/
class HeadwayRegularity(params: ObservedStopTimes)
    extends Indicator
      with AggregatesByAll {
  type Intermediate = Double

  val name = "regularity_headways"

  def calculation(period: SamplePeriod) = {
    val observedTrips: Map[String, Trip] = params.observedTripById(period)

    def map(trips: Seq[Trip]): Double = {
      val scheduledHeadway = tripsToHeadway(trips)
      val observedHeadway = tripsToHeadway(trips.map { trip => observedTrips(trip.id) }.toSeq)
      (scheduledHeadway - observedHeadway).abs.toDouble / 60 // div60 for minutes
    }

    def reduce(hwDeviations: Seq[Double]): Double = {
      val (total, count) =
        hwDeviations.foldLeft((0.0, 0)) { case ((total, count), diff) =>
          (total + diff, count + 1)
        }
      if (count > 0) (total /count) else 0.0
    }
    perRouteCalculation(map, reduce)
  }

  def tripsToHeadway(trips: Seq[Trip]): Double = {
    val headways: Seq[Double] =
      trips.map { trip =>
        trip.schedule
          .groupBy(_.stop.id).toMap
      }.combineMaps
        .flatMap { case (k, groupedStops) =>
          groupedStops.zip(groupedStops.tail).map { case (a, b) =>
            Seconds.secondsBetween(a.arrivalTime, b.arrivalTime).getSeconds.toDouble
          }
        }.toSeq

    val (total, count) =
      headways.foldLeft((0.0, 0)) { case ((total, count), diff) =>
        (total + diff, count + 1)
      }
    if (count > 0) total / count else 0
  }
}
