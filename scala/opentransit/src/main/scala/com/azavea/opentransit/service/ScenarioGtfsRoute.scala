package com.azavea.opentransit.service

import com.azavea.gtfs.io.database.{DatabaseRecordImport, DefaultProfile, GtfsTables}
import com.azavea.gtfs._
import com.azavea.opentransit.service.json.ScenariosGtfsRouteJsonProtocol
import com.azavea.opentransit.{TaskQueue, DatabaseInstance}
import com.sun.xml.internal.ws.encoding.soap.DeserializationException
import geotrellis.slick.Projected
import geotrellis.vector.{Line, Point}
import geotrellis.vector.json.GeoJsonSupport
import org.joda.time._
import org.joda.time.format.PeriodFormatterBuilder
import spray.http._
import spray.httpx.SprayJsonSupport

import spray.json.DefaultJsonProtocol
import spray.json._
import scala.concurrent._
import scala.concurrent.ExecutionContext.global
import scala.util.{Success, Try, Failure}

case class TripTuple(trip: TripRecord,
                     stopTimes: Seq[(StopTimeRecord, Stop)],
                     frequencies: Seq[FrequencyRecord],
                     shape: Option[TripShape])


trait ScenarioGtfsRoute extends Route {
  import ScenarioGtfsRoute._
  import spray.httpx.SprayJsonSupport._
  import ScenariosGtfsRouteJsonProtocol._

  import tables.profile.simple._

  /** This seems weird, but scalac will NOT find this implicit with simple import */

  def scenarioGtfsRoute(scenarioDB: Database) =
    pathPrefix("routes") {
      pathEnd {
        get { /** Get route list */
          complete { future {
            val routes: List[RouteRecord] =
              scenarioDB withSession { implicit s =>
                tables.routeRecordsTable.list
            }
            routes
          }}
        } ~
        post { /** Insert new Route Record */
          entity(as[RouteRecord]) { route =>
            complete {
              future {
                scenarioDB withTransaction { implicit s =>
                  tables.routeRecordsTable.insert(route)
                }
                StatusCodes.Created
              }
            }
          }
        }
      } ~
      pathPrefix(Segment) { routeId =>
        pathEnd {
          get { /** Get single RouteRecord */
            complete { future {
              scenarioDB withSession { implicit s =>
                tables.routeRecordsTable.filter(_.id === routeId).firstOption
              }
            }}
          } ~
          delete { /** Delete RouteRecord and all it's trips */
            complete { future {
              scenarioDB withTransaction { implicit s =>
                deleteRoute(routeId)
              }
              StatusCodes.OK
            }}
          }
        } ~
        pathPrefix("trips") { tripEndpoint(scenarioDB, routeId) }
      }
    }

  def tripEndpoint(db: Database, routeId: RouteId) = 
    pathEnd {
      /** List all trip_ids in the route and bin them by their path */
      get {
        complete {
          future {
            db withSession { implicit s =>
              fetchTripBins(routeId)
            }
          }
        }
      }
    } ~
    pathPrefix(Segment) { tripId =>
      val trip = tables.tripRecordsTable.filter(trip => trip.id === tripId && trip.route_id === routeId)

      get { /** Fetch specific trip by id */
        complete {
          future {
            db withSession { implicit s =>
              trip.firstOption.map(buildTripPattern)
            }
          }
        }
      } ~
      post { /** Accept a trip pattern, use it as a basis for creating new TripRecord, StopTimesRecords and Stops. */
        entity(as[TripTuple]) { pattern =>
          complete {
            TaskQueue.execute {
              db withTransaction { implicit s =>
                val bins = fetchTripBins(routeId)
                for {
                  bin <- bins.find(_.contains(pattern.trip.id))
                  tripId <- bin // delete all trips that are in the same bin as our parameter
                } deleteTrip(tripId)
                saveTripPattern(pattern)
                StatusCodes.Created
              }
            }
          }
        }
      } ~
      delete { /** Delete all traces of the single named trip */
        complete {
          future {
            db withTransaction { implicit s =>
              trip.firstOption
                .map { _ =>
                deleteTrip(tripId)
                StatusCodes.OK
              }
            }
          }
        }
      }
    }

}

object ScenarioGtfsRoute {
  private val tables = new GtfsTables with DefaultProfile
  import tables.profile.simple._

  /** Load all stop_times for route and use them to group trip_ids by trip path */
  private def fetchTripBins(routeId: String)(implicit s: Session): Array[Array[String]] = {
    /** extract part of trip that identifies unique path */
    def tripOffset(stopTimes: Seq[StopTimeRecord]) = {
      import com.github.nscala_time.time.Imports._

      val offset = stopTimes.head.arrivalTime
      stopTimes map { st =>
        (st.stopId, (st.arrivalTime - offset).toStandardSeconds , (st.departureTime - offset).toStandardSeconds)
      }
    }

    val stopTimesAll = tables.tripRecordsTable.filter(_.route_id === routeId)
      .join(tables.stopTimeRecordsTable).on(_.id === _.trip_id)
      .sortBy(_._2.stop_sequence)
      .map(_._2)
      .list

    val tripStopTimes = stopTimesAll
      .groupBy(_.tripId)
      .map{ case (tripId, stops) => tripId -> stops.sortBy(_.sequence) }
      .toArray

    // note: we also have the the trip headway information, if it's ever useful

    tripStopTimes
      .groupBy{ case (tripId, stops) => tripOffset(stops) }
      .values // the keys are trip patterns
      .map(tripStopList => tripStopList.map(_._1)) // discarding the stop-times, wasteful ?
      .toArray
  }

  private def deleteTrip(tripId: String)(implicit s: Session): Unit = {
    //Delete stops created through this service, if they exist for this trip
    ( tables.stopsTable
      filter ( stop => stop.id.like(s"${ScenariosGtfsRouteJsonProtocol.STOP_PREFIX}-${tripId}%"))
      delete
    )
    tables.stopTimeRecordsTable.filter(_.trip_id === tripId).delete
    tables.frequencyRecordsTable.filter(_.trip_id === tripId).delete
    tables.tripRecordsTable.filter(_.id === tripId).delete
    tables.tripShapesTable.filter(_.id === s"${ScenariosGtfsRouteJsonProtocol.STOP_PREFIX}-${tripId}").delete
  }

  private def saveTripPattern(pattern: TripTuple)(implicit s: Session): Unit = {
    tables.tripRecordsTable.insert(pattern.trip)
    tables.frequencyRecordsTable.insertAll(pattern.frequencies:_*)
    tables.stopTimeRecordsTable.insertAll(pattern.stopTimes map {_._1}:_*)
    tables.stopsTable.insertAll(pattern.stopTimes map {_._2}:_*)
    pattern.shape map { tables.tripShapesTable.insert }
  }

  private def buildTripPattern(trip: TripRecord)(implicit s: Session): TripTuple = {
    val stops = (
      tables.stopTimeRecordsTable
        filter (_.trip_id === trip.id)
        join tables.stopsTable on (_.stop_id === _.id)
        sortBy { case (st, _) => st.stop_sequence }
        list
      )
    val frequencies = tables.frequencyRecordsTable.filter(_.trip_id === trip.id).list

    val shape = for {
      shapeId <- trip.tripShapeId
      shape <- tables.tripShapesTable.filter(_.id === shapeId).firstOption
    } yield shape

    TripTuple(trip, stops, frequencies, shape)
  }

  private def deleteRoute(routeId: RouteId)(implicit s: Session): Unit = {
    val tripIds = tables.tripRecordsTable.filter(_.route_id === routeId).map(_.id).list
    tripIds foreach {deleteTrip}
    tables.routeRecordsTable.filter(_.id === routeId).delete
  }
}
