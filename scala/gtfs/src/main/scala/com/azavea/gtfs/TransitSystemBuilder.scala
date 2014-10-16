package com.azavea.gtfs

import com.github.nscala_time.time.Imports._
import org.joda.time.Days

import scala.collection.mutable

object TransitSystemBuilder {
  def apply(records: GtfsRecords): TransitSystemBuilder =
    new TransitSystemBuilder(records)
}

class TransitSystemBuilder(records: GtfsRecords) {
  private val tripIdToFrequencyRecords: Map[TripId, Seq[FrequencyRecord]] = 
    records.frequencyRecords.groupBy(_.tripId)

  private val tripIdToStopTimeRecords: Map[TripId, Seq[StopTimeRecord]] =
    records.stopTimeRecords.groupBy(_.tripId)

  private val stopIdToStop: Map[String, Stop] =
    records.stops.map { stop => (stop.id, stop) }.toMap

  private val tripShapeIdToTripShape: Map[String, TripShape] =
    records.tripShapes.map { tripShape => (tripShape.id, tripShape) }.toMap

  private val agencyIdToAgency: Map[String, Agency] =
    records.agencies.map { agency => (agency.id, agency) }.toMap

  /** Generates a TransitSystem for the specified period */
  def systemBetween(
      start: LocalDateTime,
      end: LocalDateTime,
      pruneStops: Boolean = true): TransitSystem = {
    val dates = {
      val startDate = start.toLocalDate
      val endDate = end.toLocalDate
      val daysBetween = Days.daysBetween(startDate, endDate).getDays
      (0 until (daysBetween + 1)).map(startDate.plusDays(_))
    }

    val calendar = ServiceCalendar(dates, records.calendarRecords, records.calendarDateRecords)

    val routeIdToTrips = mutable.Map[String, mutable.ListBuffer[Trip]]()

    for(tripRecord <- records.tripRecords) {
      val isActiveDuringDates =
        dates
          .map(calendar(_)(tripRecord.serviceId))
          .foldLeft(false)(_ | _)

      if(isActiveDuringDates) {
        val stopTimeRecords = tripIdToStopTimeRecords(tripRecord.id)

        val trips: Seq[Trip] =
          tripIdToFrequencyRecords.get(tripRecord.id) match {
            case Some(frequencyRecords) =>
              // Since this trip is scheduled per frequency, we need to go over each frequency schedule
              // for each of the dates in the range, and generate all applicable scheduled trips.
              (for(date <- dates) yield {
                val midnight = date.toLocalDateTime(LocalTime.Midnight)

                frequencyRecords.map { frequencyRecord =>
                  frequencyRecord.generateStartTimes(date)
                    .map { startTime =>
                      val offset = stopTimeRecords.head.arrivalTime
                      val scheduledStops = 
                        stopTimeRecords
                          .map { record =>
                            ScheduledStop(record, startTime, offset, stopIdToStop)
                           }
                          .filter { scheduledStop =>
                            !pruneStops || (start <= scheduledStop.departureTime && scheduledStop.arrivalTime <= end)
                          }

                      if(!scheduledStops.isEmpty)
                        Some(Trip(tripRecord, scheduledStops, tripShapeIdToTripShape))
                      else
                        None
                     }
                    .flatten //Remove trips with no stops
                }.flatten
              }).flatten
            case None =>
              (for(date <- dates) yield {
                val midnight = date.toLocalDateTime(LocalTime.Midnight)
                val scheduledStops =
                  stopTimeRecords
                    .sortBy(_.sequence)
                    .map(ScheduledStop(_, midnight, stopIdToStop))
                    .filter { scheduledStop =>
                      !pruneStops || (start <= scheduledStop.departureTime && scheduledStop.arrivalTime <= end)
                     }

                if(!scheduledStops.isEmpty){
                  Some(Trip(tripRecord, scheduledStops, tripShapeIdToTripShape))
                }
                else
                  None
                Seq(Trip(tripRecord, scheduledStops, tripShapeIdToTripShape))
              }).flatten.toSeq
          }

        if(! routeIdToTrips.contains(tripRecord.routeId)) {
          routeIdToTrips(tripRecord.routeId) = new mutable.ListBuffer[Trip]()
        }
        routeIdToTrips(tripRecord.routeId) ++= trips
      }
    }

    val constructedRoutes =
      records.routeRecords
        .map { record =>
          routeIdToTrips
            .get(record.id)
            .flatMap { trips =>
              if(trips.isEmpty) None
              else Some(Route(record, trips, agencyIdToAgency))
             }
         }
        .flatten

    val routesByType = 
      constructedRoutes.groupBy(_.routeType)

    new TransitSystem {
      val routes = constructedRoutes
      def routes(routeType: RouteType): Seq[Route] =
        routesByType.get(routeType) match {
          case Some(rs) => rs
          case None => Seq()
        }
    }
  }
}
