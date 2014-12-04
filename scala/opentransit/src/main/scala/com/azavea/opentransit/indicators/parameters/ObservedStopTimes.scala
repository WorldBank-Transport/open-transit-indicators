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
  def observedStopsByTrip(tripId: String): Seq[(ScheduledStop, ScheduledStop)]
  def observedTripById(tripId: String):  Trip
  def missingTripData: Int
}

object ObservedStopTimes {
  def apply(
    scheduledSystem: TransitSystem,
    period: SamplePeriod,
    db: DatabaseDef,
    hasObserved: Boolean): ObservedStopTimes = {
    // This is ugly: a thousand sorries. it also is apparently necessary -
    // we have to index on SamplePeriod and again on trip id

    lazy val observedSystem = {
      val observedGtfsRecords =
        db withSession { implicit session =>
          new DatabaseGtfsRecords with DefaultProfile {
            override val stopTimesTableName = "gtfs_stop_times_real"
          }
        }
      val builder = TransitSystemBuilder(observedGtfsRecords)
      builder.systemBetween(period.start, period.end, pruneStops=false)
    }

    lazy val observedTrips: Map[String, Trip] =
      observedSystem.routes.flatMap { route =>
        route.trips.map { trip => (trip.id -> trip) }
    }.toMap

    var missingTrips: Int = 0

    lazy val observedStops: Map[String, Seq[(ScheduledStop, ScheduledStop)]] = {
      val scheduledTrips = scheduledSystem.routes.flatMap(_.trips)
      val observedTripsById = observedTrips
      scheduledTrips.map { trip =>
        (trip.id -> {
          val schedStops: Map[String, ScheduledStop] =
            trip.schedule.map(sst => sst.stop.id -> sst).toMap
          val obsvdStops: Map[String, ScheduledStop] =
            // allow for scheduled trips not in observed data
            observedTripsById.get(trip.id) match {
              case Some(observed) => observed.schedule.map(ost => ost.stop.id -> ost).toMap
              case None => {
                val tripId = trip.id.toString
                missingTrips = missingTrips + 1
                println(s"Missing observed stop times for trip ${tripId}")
                Map()
              }
            }
           // only return stops that are in the observed data
          for {
            s <- trip.schedule
            if !obsvdStops.get(s.stop.id).isEmpty
          } yield (schedStops(s.stop.id), obsvdStops(s.stop.id))
        }) // Seq[(String, Seq[(ScheduledStop, ScheduledStop)])]
      }.toMap
    } // Map[String, Seq[(ScheduledStop, ScheduledStop)]])]


    if (hasObserved) {
      new ObservedStopTimes {
        def observedStopsByTrip(tripId: String): Seq[(ScheduledStop, ScheduledStop)] = 
          observedStops.get(tripId) match {
            case Some(s) => s
            case None => Nil
          }
        def observedTripById(tripId: String): Trip =
          observedTrips.get(tripId) match {
            case Some(t) => t
            case None => new Trip {
              def headsign = None
              def id = tripId
              def schedule = Nil
              def tripShape = None
            }
          }
        def missingTripData: Int = missingTrips
      }
    } else {
      new ObservedStopTimes {
        def observedStopsByTrip(tripId: String): Seq[(ScheduledStop, ScheduledStop)] =
          Nil
        def observedTripById(tripId: String): Trip =
          new Trip {
            def headsign = None
            def id = ""
            def schedule = Nil
            def tripShape = None
          }
        def missingTripData: Int = missingTrips
      }
    }
  }
}

