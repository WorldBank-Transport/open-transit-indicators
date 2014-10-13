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
  def observedForTrip(period: SamplePeriod, scheduledTripId: String): Trip // real-time data corresponding to scheduled trip
}

object ObservedStopTimes {
  def apply(scheduledSystems: Map[SamplePeriod, TransitSystem])(implicit session: Session): ObservedStopTimes = {
    // This is ugly: a thousand sorries. it also is apparently necessary -
    // we have to index on SamplePeriod and again on trip id
    val observedTrips: Map[SamplePeriod, Map[String, Trip]] = {
      val periods = scheduledSystems.keys
      val observedGtfsRecords =
        new DatabaseGtfsRecords with DefaultProfile {
          override val stopTimesTableName = "gtfs_stop_times_real"
        }
      val builder = TransitSystemBuilder(observedGtfsRecords)
      val observedSystemsMap = periods.map { period =>
        (period -> builder.systemBetween(period.start, period.end))
      }.toMap
      observedSystemsMap.map { case (period, system) =>
        period -> system.routes.map { route =>
          route.trips.map { trip =>
            (trip.id -> trip)
          }
        }
        .flatten
        .toMap
      }
      .toMap
    }
    new ObservedStopTimes {
      def observedForTrip(period: SamplePeriod, scheduledTripId: String): Trip =
        observedTrips(period)(scheduledTripId)
    }
  }
}

