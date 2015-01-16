package com.azavea.gtfs

import com.github.nscala_time.time.Imports._
import org.joda.time.Days

import scala.collection.mutable
import com.azavea.gtfs.op._

trait TransitSystemBuilder {
  /** Generates a TransitSystem for the specified period, inclusive */
  def systemBetween(start: LocalDate, end: LocalDate): TransitSystem =
    systemBetween(
      start.toLocalDateTime(LocalTime.Midnight),
      end.toLocalDateTime(LocalTime.Midnight plusHours 24 minusMillis 1))

  /** Generates a TransitSystem for the specified date */
  def systemOn(date: LocalDate): TransitSystem =
    systemBetween(
      date.toLocalDateTime(LocalTime.Midnight),
      date.toLocalDateTime(LocalTime.Midnight plusHours 24 minusMillis 1))

  /** Generates a TransitSystem for the specified period */
  def systemBetween(start: LocalDateTime, end: LocalDateTime, pruneStopsBufferMinutes: Int = 0): TransitSystem

  def filterByRoute(routeType: RouteType): TransitSystemBuilder = {
    val innerSystemBetween: (LocalDateTime, LocalDateTime, Int) => TransitSystem = systemBetween _
    new TransitSystemBuilder {
      def systemBetween(start: LocalDateTime, end: LocalDateTime, pruneStopsBufferMinutes: Int = 0): TransitSystem = {
        val system = innerSystemBetween(start, end, pruneStopsBufferMinutes)
        new TransitSystem {
          def routes: Seq[Route] = system.routes.filter(_.routeType == routeType)
        }
      }
    }
  }
}

object TransitSystemBuilder {
  def apply(records: GtfsRecords): TransitSystemBuilder =
    new BaseTransitSystemBuilder(records)

  type StopScheduler = (Seq[StopTimeRecord], LocalDate, StopId=>Stop) => Iterator[Seq[ScheduledStop]]

  def scheduleStopFromFrequency(frequency: FrequencyRecord)
                               (stopTimeRecords: Seq[StopTimeRecord],
                                date: LocalDate,
                                stopIdToStop: StopId => Stop): Iterator[Seq[ScheduledStop]] = {
    frequency.generateStartTimes(date) map { startTime =>
      val offset = stopTimeRecords.head.arrivalTime

      stopTimeRecords.map { record: StopTimeRecord =>
        ScheduledStop(record, startTime, offset, stopIdToStop)
      }.toSeq.flatten
    }
  }

  def scheduleStops(stopTimeRecords: Seq[StopTimeRecord],
                    date: LocalDate,
                    stopIdToStop: StopId => Stop): Iterator[Seq[ScheduledStop]] = {

    val midnight = date.toLocalDateTime(LocalTime.Midnight)
    Iterator(stopTimeRecords  map { ScheduledStop(_, midnight, stopIdToStop) })
  }
}

class BaseTransitSystemBuilder(records: GtfsRecords) extends TransitSystemBuilder {
  import TransitSystemBuilder._

  private val tripIdToFrequencyRecords: Map[TripId, Seq[FrequencyRecord]] =
    records.frequencyRecords.groupBy(_.tripId)

  private val tripIdToStopTimeRecords: Map[TripId, Seq[StopTimeRecord]] =
    records.stopTimeRecords
      .groupBy(_.tripId)
      .map{ case (key, values) => key -> values.sortBy(_.sequence) }

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
      pruneStopsBufferMinutes: Int = 0): TransitSystem = {
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
        val stopTimeRecords = tripIdToStopTimeRecords.getOrElse(tripRecord.id, Nil)

        val trips: Iterator[Trip] = {
          val schedulers: Seq[StopScheduler] =
            tripIdToFrequencyRecords.get(tripRecord.id) match {
              case Some(frequencies) =>
                frequencies map { freq => scheduleStopFromFrequency(freq) _ }
              case None =>
                scheduleStops _ :: Nil
            }

          val scheduledStops =
            (for(date <- dates) yield {
              val listOfLists =
                schedulers map { f =>
                  f(stopTimeRecords, date, stopIdToStop)
                    .map { stopList =>
                      stopList.filter { stop => // chop off stops that happen past our system bounds
                        (start <= stop.departureTime + pruneStopsBufferMinutes.minute && stop.arrivalTime - pruneStopsBufferMinutes.minute <= end)
                      }
                    }
                    .filter (_.length > 0) // throw out empty stop lists after prune
                }

              listOfLists.foldLeft(List[Seq[ScheduledStop]]())(_ ++ _) // combine iterators from all schedulers
            }).foldLeft(List[Seq[ScheduledStop]]())(_ ++ _)           // combine iterators from all dates

         scheduledStops.toIterator map { stops => Trip(tripRecord, stops.toArray[ScheduledStop], tripShapeIdToTripShape) }
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
              else Some(Route(record, trips.toArray[Trip], agencyIdToAgency))
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
