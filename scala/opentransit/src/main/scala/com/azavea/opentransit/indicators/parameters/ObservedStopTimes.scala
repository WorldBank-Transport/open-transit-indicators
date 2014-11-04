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
  // Map of Trip IDs to Sequence of tuples of (scheduled, observed)
  def observedStopsByTrip(period: SamplePeriod): Map[String, Seq[(ScheduledStop, ScheduledStop)]]
  def observedTripById(period: SamplePeriod):  Map[String, Trip]
}

object ObservedStopTimes {
  def apply(
    scheduledSystems: Map[SamplePeriod, TransitSystem],
    db: DatabaseDef,
    hasObserved: Boolean): ObservedStopTimes = {
    // This is ugly: a thousand sorries. it also is apparently necessary -
    // we have to index on SamplePeriod and again on trip id
    lazy val periods = scheduledSystems.keys

    lazy val observedSystemsMap = {
      val observedGtfsRecords =
        db withSession { implicit session =>
          new DatabaseGtfsRecords with DefaultProfile {
            override val stopTimesTableName = "gtfs_stop_times_real"
          }
        }
      val builder = TransitSystemBuilder(observedGtfsRecords)
      periods.map { period =>
        (period -> builder.systemBetween(period.start, period.end, pruneStops=false))
      }.toMap
    }

    lazy val observedTrips: Map[SamplePeriod, Map[String, Trip]] =
      observedSystemsMap.map { case (period, system) =>
        period -> system.routes.flatMap { route =>
          route.trips.map { trip => (trip.id -> trip) }
        }.toMap
      }.toMap


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
              for (s <- trip.schedule)
                yield (schedStops(s.stop.id), obsvdStops(s.stop.id))
            }) // Seq[(String, Seq[(ScheduledStop, ScheduledStop)])]
          }.toMap
        }) // Seq[(Period, Map[String, Seq[(ScheduledStop, ScheduledStop)]])]
      }.toMap


    if (hasObserved) {
      new ObservedStopTimes {
        def observedStopsByTrip(period: SamplePeriod): Map[String, Seq[(ScheduledStop, ScheduledStop)]] =
          observedPeriodTrips(period)
        def observedTripById(period: SamplePeriod): Map[String, Trip] =
          observedTrips(period)
      }
    } else {
      new ObservedStopTimes {
        def observedStopsByTrip(period: SamplePeriod): Map[String, Seq[(ScheduledStop, ScheduledStop)]] =
          Map()
        def observedTripById(period: SamplePeriod): Map[String, Trip] =
          Map()
      }
    }
  }
}

