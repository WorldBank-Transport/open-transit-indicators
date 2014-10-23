package com.azavea.gtfs.op

import com.azavea.gtfs._
import org.joda.time.{Days, LocalDateTime, LocalDate}

object TimeFilter {
  def apply(records: GtfsRecords, start: LocalDateTime, end: LocalDateTime): GtfsRecords = {
    val dates = {
      val startDate = start.toLocalDate
      val endDate = end.toLocalDate
      val daysBetween = Days.daysBetween(startDate, endDate).getDays
      (0 until (daysBetween + 1)).map(d => startDate.plusDays(d))
    }

    val calendar = ServiceCalendar(dates, records.calendarRecords, records.calendarDateRecords)

    var activeTrips: Set[TripId] = Set.empty
    var activeRoutes: Set[RouteId] = Set.empty
    var activeService: Set[ServiceId] = Set.empty
    var activeShape:Set[String] = Set.empty

    for(tripRecord <- records.tripRecords) {
      val tripActive = dates
        .map(date => calendar(date)(tripRecord.serviceId))
        .foldLeft(false)(_ | _)

      if (tripActive) {
        activeTrips += tripRecord.id
        activeRoutes += tripRecord.routeId
        activeService += tripRecord.serviceId
        tripRecord.tripShapeId.map {id => activeShape += id}
      }
    }
    var activeStops: Set[StopId] = Set.empty
    val stopTimes = records.stopTimeRecords.filter(r => activeTrips.contains(r.tripId))
    stopTimes.foreach(activeStops += _.stopId)

    new GtfsRecords {
      val agencies =
        records.agencies
      val calendarDateRecords =
        records.calendarDateRecords.filter(r => activeService.contains(r.serviceId))
      val tripRecords: Seq[TripRecord] =
        records.tripRecords.filter(r => activeTrips.contains(r.id))
      val calendarRecords =
        records.calendarRecords.filter(r => activeService.contains(r.serviceId))
      val routeRecords =
        records.routeRecords.filter(r => activeRoutes.contains(r.id))
      val tripShapes =
        records.tripShapes.filter(r => activeShape.contains(r.id))
      val stopTimeRecords: Seq[StopTimeRecord] =
        stopTimes
      val stops: Seq[Stop] =
        records.stops.filter(r => activeStops.contains(r.id))
      val frequencyRecords: Seq[FrequencyRecord] =
        records.frequencyRecords.filter(r => activeTrips.contains(r.tripId))
    }
  }
}
