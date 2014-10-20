package com.azavea.opentransit.indicators.parameters

import com.azavea.opentransit.database.{ BoundariesTable, RoadsTable }
import scala.slick.jdbc.JdbcBackend.{Database, DatabaseDef, Session}

import com.azavea.gtfs.io.database._
import com.azavea.gtfs._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._
import com.azavea.gtfs.{TransitSystem, Stop}

import scala.collection.mutable

import grizzled.slf4j.Logging

/**
 * Trait used to populate parameters with data from 'real time' GTFS
  */
trait ObservedStopTimes {
  // real-time data corresponding to scheduled trip
  def observedForTrip(period: SamplePeriod, scheduledTripId: String): Trip
  // Map of Trip IDs to Sequence of tuples of (scheduled, observed)
  def observedStopsByTrip(period: SamplePeriod): Map[String, Seq[(ScheduledStop, ScheduledStop)]]
}

object ObservedStopTimes {
  def apply(scheduledSystems: Map[SamplePeriod, TransitSystem], hasObserved: Boolean)(implicit session: Session): ObservedStopTimes = {
    // This is ugly: a thousand sorries. it also is apparently necessary -
    // we have to index on SamplePeriod and again on trip id
    val periods = scheduledSystems.keys
    lazy val observedTrips: Map[SamplePeriod, Map[String, Trip]] = {
      val observedGtfsRecords =
        new DatabaseGtfsRecords with DefaultProfile {
          override val stopTimesTableName = "gtfs_stop_times_real"
        }
      val builder = TransitSystemBuilder(observedGtfsRecords)
      val observedSystemsMap = periods.map { period =>
        (period -> builder.systemBetween(period.start, period.end, pruneStops=false))
      }.toMap
      observedSystemsMap.map { case (period, system) =>
        period -> system.routes.flatMap { route =>
          route.trips.map { trip =>
            (trip.id -> trip)
          }
        }
        .toMap
      }
      .toMap
    }

    lazy val observedPeriodTrips: Map[SamplePeriod, Map[String, Seq[(ScheduledStop, ScheduledStop)]]] =
      periods.map { period =>
        (period -> {
          val scheduledTrips = scheduledSystems(period).routes.flatMap(_.trips)
          val observedTripsById = observedTrips(period)
          scheduledTrips.map { trip =>
            (trip.id -> {
              val schedStops: Map[String, ScheduledStop] =
                trip.schedule.map(sst => sst.stop.id -> sst).toMap
              val obsvdStops: Map[String, ScheduledStop] =
                observedTripsById(trip.id).schedule.map(ost => ost.stop.id -> ost).toMap
              for {
                s <- trip.schedule
                // TODO: This will throw an exception if the stop id isn't found.
                // This can happen if a stop is so shifted in the observed data that it
                // falls outside the sample period. The systems are constructed with a 2-hour
                // pad that will protect against most cases, but it's still possible that certain
                // observed data could break this indicator.
              } yield (schedStops(s.stop.id), obsvdStops(s.stop.id))
            }) // Seq[(String, Seq[(ScheduledStop, ScheduledStop)])]
          }.toMap
        }) // Seq[(Period, Map[String, Seq[(ScheduledStop, ScheduledStop)]])]
      }.toMap

    if (hasObserved) {
      new ObservedStopTimes {
        def observedForTrip(period: SamplePeriod, scheduledTripId: String): Trip =
          observedTrips(period)(scheduledTripId)

        def observedStopsByTrip(period: SamplePeriod): Map[String, Seq[(ScheduledStop, ScheduledStop)]] =
          observedPeriodTrips(period)
      }
    } else {
      new ObservedStopTimes {
        def observedForTrip(period: SamplePeriod, scheduledTripId: String): Trip =
            scheduledSystems(period)
              .routes
              .flatMap(_.trips)
              .head

        def observedStopsByTrip(period: SamplePeriod): Map[String, Seq[(ScheduledStop, ScheduledStop)]] =
          Map()
      }
    }
  }
}

