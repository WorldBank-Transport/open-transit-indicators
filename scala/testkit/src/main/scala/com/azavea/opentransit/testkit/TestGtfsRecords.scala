package com.azavea.opentransit.testkit

import com.azavea.gtfs._

import com.azavea.gtfs.io.csv._

import com.github.nscala_time.time.Imports._
import geotrellis.vector._
import geotrellis.slick._

object TestGtfsRecords {
  def apply(): TestGtfsRecords =
    new TestGtfsRecords

  object times {
    val subTravelTime = 5.minute
    val subWaitTime = 1.minute
    val busTravelTime = 20.minute
    val busWaitTime = 2.minute
    val railTravelTime = 45.minute
    val railWaitTime = 5.minute
  }

  object stopLocations {
    val stopCenter = Point(0,0)
    val stopWest = Point(-10,0)
    val stopEast = Point(10,0)
    val stopNorth = Point(0,10)
    val stopSouth = Point(0,-10)
  }
}

class TestGtfsRecords extends GtfsRecords {
  import TestGtfsRecords.times._
  import TestGtfsRecords.stopLocations._

  val serviceIds = 
    Seq(
      "SUB_WEEKDAYS",
      "SUB_SATURDAY",
      "SUB_SUNDAY",
      "BUS_WEEKDAYS",
      "BUS_WEEKENDS",
      "RAIL_WEEKDAYS",
      "RAIL_WEEKENDS"
    )

  val agencies: Seq[Agency] = List(
    Agency("AGENCY1", "SUBBERS", "http://subbers.com", "EST", Some("eng"), Some("555-5555"), Some("http://subbers.com/fare")),
    Agency("AGENCY2", "BUSSERS", "http://bussers.com", "EST", Some("eng"), Some("555-5555"), Some("http://bussers.com/fare")),
    Agency("AGENCY3", "RAILERS", "http://railers.com", "EST", Some("eng"), Some("555-5555"), Some("http://railers.com/fare"))
  )

  val stops = List(
    Stop("ST1", "CENTER STATION", Some("Center Station, has bus, and subway"), stopCenter.withSRID(4326)),

    Stop("ST2", "WEST STATION", Some("West Station, has subway and rail"), stopWest.withSRID(4326)),
    Stop("ST3", "EAST STATION", Some("East Station, has subway and rail"), stopEast.withSRID(4326)),
    Stop("ST4", "NORTH STATION", Some("North Station, has subway"), stopNorth.withSRID(4326)),
    Stop("ST5", "SOUTH STATION", Some("South Station, has subway"), stopSouth.withSRID(4326)),

    Stop("ST6", "NW STREET", Some("NorthWest Street, has bus and rail"), Point(-5,5).withSRID(4326)),
    Stop("ST7", "SW STREET", Some("SoutWest Street, has bus"), Point(-5,-5).withSRID(4326)),
    Stop("ST8", "NE STREET", Some("NorthEast Street, has bus and rail"), Point(5, 5).withSRID(4326)),
    Stop("ST9", "SE STREET", Some("SouthEast Street, has bus"), Point(5, -5).withSRID(4326)),

    Stop("ST10", "NORTH COUNTY", Some("North County, has rail"), Point(0,20).withSRID(4326)),
    Stop("ST11", "WEST COUNTY", Some("West County, has rail"), Point(-20,0).withSRID(4326)),
    Stop("ST12", "SOUTH COUNTY", Some("South County, has rail"), Point(0, -20).withSRID(4326)),
    Stop("ST13", "EAST COUNTY", Some("East County, has rail"), Point(20, 0).withSRID(4326))
  )

  val stopsByName = stops.map { stop => (stop.name, stop) }.toMap

  def routeRecords = List(
    // Subway routes
    RouteRecord("EastWest", "East West Subway", "Subway from WEST STATION to EAST STATION", Subway, Some("SUBBERS")),
    RouteRecord("NorthSouth", "North South Subway", "Subway from NORTH STATION to SOUTH STATION", Subway, Some("SUBBERS")),

    // Bus routes
    RouteRecord("WestBus", "West Bus", "Bus from NW STREET to SW STREET", Bus, Some("BUSSERS")),
    RouteRecord("EastBus", "East Bus", "Bus from NE STREET to NW STREET", Bus, Some("BUSSERS")),

    // Rail routes
    RouteRecord("WestRail", "West Rail", "Rail from NORTH COUNTY to SOUTH COUNTY via EAST COUNTY", Rail, Some("RAILERS")),
    RouteRecord("EastRail", "East Rail", "Rail from NORTH COUNTY to SOUTH COUNTY via EAST COUNTY", Rail, Some("RAILERS"))
  )

  val routeToStops = {
    val s = stopsByName
    Map(
      ("EastWest", Seq(s("EAST STATION"), s("CENTER STATION"), s("WEST STATION"))),
      ("NorthSouth", Seq(s("NORTH STATION"), s("CENTER STATION"), s("SOUTH STATION"))),
      ("WestBus", Seq(s("NW STREET"), s("CENTER STATION"), s("SW STREET"))),
      ("EastBus", Seq(s("NE STREET"), s("CENTER STATION"), s("SE STREET"))),
      ("WestRail", Seq(s("NORTH COUNTY"), s("NW STREET"), s("WEST STATION"), s("SOUTH STATION"))),
      ("EastRail", Seq(s("NORTH COUNTY"), s("NE STREET"), s("EAST STATION"), s("SOUTH STATION")))
    )
  }


  def tripRecords = List(
    // Subway weekday trips in the morning, afternoon and evening (which run on frequencies).
    // Morning = 5 AM - 10 AM  every 10 minutes
    // Afternoon = 10 AM - 4 PM  every 20 minutes
    // Evening = 4 PM - 11 PM  every 10 minutes
    TripRecord("SUB_WEEKDAY_MORNING_EastWest", "SUB_WEEKDAYS", "EastWest", None, Some("SUB_EastWest_SHAPE")),
    TripRecord("SUB_WEEKDAY_AFTERNOON_EastWest", "SUB_WEEKDAYS", "EastWest", None, Some("SUB_EastWest_SHAPE")),
    TripRecord("SUB_WEEKDAY_EVENING_EastWest", "SUB_WEEKDAYS", "EastWest", None, Some("SUB_EastWest_SHAPE")),
    TripRecord("SUB_WEEKDAY_MORNING_NorthSouth", "SUB_WEEKDAYS", "NorthSouth", None, Some("SUB_NorthSouth_SHAPE")),
    TripRecord("SUB_WEEKDAY_AFTERNOON_NorthSouth", "SUB_WEEKDAYS", "NorthSouth", None, Some("SUB_NorthSouth_SHAPE")),
    TripRecord("SUB_WEEKDAY_EVENING_NorthSouth", "SUB_WEEKDAYS", "NorthSouth", None, Some("SUB_NorthSouth_SHAPE")),

    // Subway weekends schedules (on frequencies)
    // Saturday = 5 AM - 11 PM  Every 20 minutes
    // Sunday =  5 AM - 11 PM  Every 30 minutes
    TripRecord("SUB_SATURDAY_EastWest", "SUB_SATURDAY", "EastWest", None, Some("SUB_EastWest_SHAPE")),
    TripRecord("SUB_SATURDAY_NorthSouth", "SUB_SATURDAY", "NorthSouth", None, Some("SUB_NorthSouth_SHAPE")),
    TripRecord("SUB_SUNDAY_EastWest", "SUB_SUNDAY", "EastWest", None, Some("SUB_EastWest_SHAPE")),
    TripRecord("SUB_SUNDAY_NorthSouth", "SUB_SUNDAY", "NorthSouth", None, Some("SUB_NorthSouth_SHAPE")),

    // Bus Weekday, 4 trips: 6 AM, 10 AM, 4 PM, 8 PM
    TripRecord("BUS_WEEKDAY_WestBus_1", "BUS_WEEKDAYS", "WestBus", None, None),
    TripRecord("BUS_WEEKDAY_WestBus_2", "BUS_WEEKDAYS", "WestBus", None, None),
    TripRecord("BUS_WEEKDAY_WestBus_3", "BUS_WEEKDAYS", "WestBus", None, None),
    TripRecord("BUS_WEEKDAY_WestBus_4", "BUS_WEEKDAYS", "WestBus", None, None),
    TripRecord("BUS_WEEKDAY_EastBus_1", "BUS_WEEKDAYS", "EastBus", None, None),
    TripRecord("BUS_WEEKDAY_EastBus_2", "BUS_WEEKDAYS", "EastBus", None, None),
    TripRecord("BUS_WEEKDAY_EastBus_3", "BUS_WEEKDAYS", "EastBus", None, None),
    TripRecord("BUS_WEEKDAY_EastBus_4", "BUS_WEEKDAYS", "EastBus", None, None),

    // Bus Weekend, frequency, 5AM - 11 PM, every hour
    TripRecord("BUS_WEEKEND_WestBus", "BUS_WEEKDAYS", "WestBus", None, None),
    TripRecord("BUS_WEEKEND_EastBus", "BUS_WEEKDAYS", "WestBus", None, None),

    // Rail Weekday, 4 trips: 8 AM, 12 PM, 6 PM, 10 PM
    TripRecord("RAIL_WEEKDAY_WestRail_1", "RAIL_WEEKDAYS", "WestRail", None, None),
    TripRecord("RAIL_WEEKDAY_WestRail_2", "RAIL_WEEKDAYS", "WestRail", None, None),
    TripRecord("RAIL_WEEKDAY_WestRail_3", "RAIL_WEEKDAYS", "WestRail", None, None),
    TripRecord("RAIL_WEEKDAY_WestRail_4", "RAIL_WEEKDAYS", "WestRail", None, None),
    TripRecord("RAIL_WEEKDAY_EastRail_1", "RAIL_WEEKDAYS", "EastRail", None, None),
    TripRecord("RAIL_WEEKDAY_EastRail_2", "RAIL_WEEKDAYS", "EastRail", None, None),
    TripRecord("RAIL_WEEKDAY_EastRail_3", "RAIL_WEEKDAYS", "EastRail", None, None),
    TripRecord("RAIL_WEEKDAY_EastRail_4", "RAIL_WEEKDAYS", "EastRail", None, None),

    // Rail Weekend, frequency, 5AM - 11 PM, every hour
    TripRecord("RAIL_WEEKEND_WestRail", "RAIL_WEEKDAYS", "WestRail", None, None),
    TripRecord("RAIL_WEEKEND_EastRail", "RAIL_WEEKDAYS", "WestRail", None, None)
  )

  val tripsById = tripRecords.map { tr => (tr.id, tr) }.toMap

  def stopTimeRecords = {
    def st(trip: TripRecord, starts: Period, every: Period, wait: Period): Seq[StopTimeRecord] = {
      routeToStops(trip.routeId)
        .zipWithIndex
        .map { case (stop, i) =>
          val arrive = 
            starts + 
            Array.fill(i)(every).foldLeft(0.minute: Period)(_ + _) + 
            Array.fill(i)(wait).foldLeft(0.minute: Period)(_ + _)

          StopTimeRecord(stop.id, trip.id, i + 1, arrive.normalizedStandard(), (arrive + wait).normalizedStandard())
        }
    }

    Seq(
      // Subway weekday trips in the morning, afternoon and evening (which run on frequencies).
      // Morning = 5 AM - 10 AM  every 10 minutes
      // Afternoon = 10 AM - 4 PM  every 20 minutes
      // Evening = 4 PM - 11 PM  every 10 minutes

      // It takes 5 minutes to go between subway stations, wait 1 minute at stop
      // These are on frequency
      st(tripsById("SUB_WEEKDAY_MORNING_EastWest"), 0.hour, subTravelTime, subWaitTime),
      st(tripsById("SUB_WEEKDAY_AFTERNOON_EastWest"), 0.hour, subTravelTime, subWaitTime),
      st(tripsById("SUB_WEEKDAY_EVENING_EastWest"), 0.hour, subTravelTime, subWaitTime),
      st(tripsById("SUB_WEEKDAY_MORNING_NorthSouth"), 0.hour, subTravelTime, subWaitTime),
      st(tripsById("SUB_WEEKDAY_AFTERNOON_NorthSouth"), 0.hour, subTravelTime, subWaitTime),
      st(tripsById("SUB_WEEKDAY_EVENING_NorthSouth"), 0.hour, subTravelTime, subWaitTime),

      // Subway weekends schedules (on frequencies)
      // Saturday = 5 AM - 11 PM  Every 20 minutes
      // Sunday =  5 AM - 11 PM  Every 30 minutes
      st(tripsById("SUB_SATURDAY_EastWest"), 0.hour, subTravelTime, subWaitTime),
      st(tripsById("SUB_SATURDAY_NorthSouth"), 0.hour, subTravelTime, subWaitTime),
      st(tripsById("SUB_SUNDAY_EastWest"), 0.hour, subTravelTime, subWaitTime),
      st(tripsById("SUB_SUNDAY_NorthSouth"), 0.hour, subTravelTime, subWaitTime),

      // Bus Weekday, 4 trips: 6 AM, 10 AM, 4 PM, 8 PM
      // Takes 20 minutes to go between stations, wait 2 minutes at stop
      st(tripsById("BUS_WEEKDAY_WestBus_1"), 6.hour, busTravelTime, busWaitTime),
      st(tripsById("BUS_WEEKDAY_WestBus_2"), 10.hour, busTravelTime, busWaitTime),
      st(tripsById("BUS_WEEKDAY_WestBus_3"), 16.hour, busTravelTime, busWaitTime),
      st(tripsById("BUS_WEEKDAY_WestBus_4"), 20.hour, busTravelTime, busWaitTime),
      st(tripsById("BUS_WEEKDAY_EastBus_1"), 6.hour, busTravelTime, busWaitTime),
      st(tripsById("BUS_WEEKDAY_EastBus_2"), 10.hour, busTravelTime, busWaitTime),
      st(tripsById("BUS_WEEKDAY_EastBus_3"), 16.hour, busTravelTime, busWaitTime),
      st(tripsById("BUS_WEEKDAY_EastBus_4"), 20.hour, busTravelTime, busWaitTime),

      // Bus Weekend, frequency, 5AM - 11 PM, every hour
      st(tripsById("BUS_WEEKEND_WestBus"), 0.hour, busTravelTime, busWaitTime),
      st(tripsById("BUS_WEEKEND_EastBus"), 0.hour, busTravelTime, busWaitTime),

      // Rail Weekday, 4 trips: 8 AM, 12 PM, 6 PM, 10 PM
      // Takes 45 minutes to go between stations, 5 minutes at stop
      st(tripsById("RAIL_WEEKDAY_WestRail_1"), 8.hour, railTravelTime, railWaitTime),
      st(tripsById("RAIL_WEEKDAY_WestRail_2"), 12.hour, railTravelTime, railWaitTime),
      st(tripsById("RAIL_WEEKDAY_WestRail_3"), 18.hour, railTravelTime, railWaitTime),
      st(tripsById("RAIL_WEEKDAY_WestRail_4"), 22.hour, railTravelTime, railWaitTime),
      st(tripsById("RAIL_WEEKDAY_EastRail_1"), 8.hour, railTravelTime, railWaitTime),
      st(tripsById("RAIL_WEEKDAY_EastRail_2"), 12.hour, railTravelTime, railWaitTime),
      st(tripsById("RAIL_WEEKDAY_EastRail_3"), 18.hour, railTravelTime, railWaitTime),
      st(tripsById("RAIL_WEEKDAY_EastRail_4"), 22.hour, railTravelTime, railWaitTime),

      // Rail Weekend, frequency, 5AM - 11 PM, every hour
      st(tripsById("RAIL_WEEKEND_WestRail"), 0.hour, railTravelTime, railWaitTime),
      st(tripsById("RAIL_WEEKEND_EastRail"), 0.hour, railTravelTime, railWaitTime)
    ).flatten
  }

  // 2014 - 2015
  def calendarRecords = {
    val weekdays = Array(true, true, true, true, true, false, false)
    val weekends = Array(false ,false, false, false, false, true, true)
    val saturday = Array(false ,false, false, false, false, true, false)
    val sunday = Array(false ,false, false, false, false, false, true)

    List(
      CalendarRecord("SUB_WEEKDAYS", "20140101", "20150101", weekdays),
      CalendarRecord("SUB_SATURDAY", "20140101", "20150101", saturday),
      CalendarRecord("SUB_SUNDAY", "20140101", "20150101", sunday),
      CalendarRecord("BUS_WEEKDAYS", "20140101", "20150101", weekdays),
      CalendarRecord("BUS_WEEKENDS", "20140101", "20150101", weekends),
      CalendarRecord("RAIL_WEEKDAYS", "20140101", "20150101", weekdays),
      CalendarRecord("RAIL_WEEKENDS", "20140101", "20150101", weekends)
    )
  }

  // Exceptions happening in May 2014
  def calendarDateRecords = 
    // May 17 is a holiday, the whole system shuts down.
    serviceIds.map { CalendarDateRecord(_, new LocalDate(2014,5,17), false) } ++
    // No Sunday service on May 3rd
    List(
      CalendarDateRecord("SUB_SUNDAY", new LocalDate(2014,5,3), false), //Saturday
      CalendarDateRecord("BUS_WEEKENDS", new LocalDate(2014,5,3), false), //Saturday
      CalendarDateRecord("RAIL_WEEKENDS", new LocalDate(2014,5,3), false) //Saturday
    )

  def frequencyRecords = List(
      // Subway weekday trips in the morning, afternoon and evening (which run on frequencies).
      // Morning = 5 AM - 10 AM  every 10 minutes
      // Afternoon = 10 AM - 4 PM  every 20 minutes
      // Evening = 4 PM - 11 PM  every 10 minutes

      // It takes 5 minutes to go between subway stations, wait 1 minute at stop
      // These are on frequency
      FrequencyRecord("SUB_WEEKDAY_MORNING_EastWest", 5.hour, 10.hour, 10.minute),
      FrequencyRecord("SUB_WEEKDAY_AFTERNOON_EastWest", 10.hour, 16.hour, 20.minute),
      FrequencyRecord("SUB_WEEKDAY_EVENING_EastWest", 16.hour, 23.hour, 10.minute),
      FrequencyRecord("SUB_WEEKDAY_MORNING_NorthSouth", 5.hour, 10.hour, 10.minute),
      FrequencyRecord("SUB_WEEKDAY_AFTERNOON_NorthSouth", 10.hour, 16.hour, 20.minute),
      FrequencyRecord("SUB_WEEKDAY_EVENING_NorthSouth", 16.hour, 23.hour, 10.minute),

      // Subway weekends schedules (on frequencies)
      // Saturday = 5 AM - 11 PM  Every 20 minutes
      // Sunday =  5 AM - 11 PM  Every 30 minutes
      FrequencyRecord("SUB_SATURDAY_EastWest", 5.hour, 23.hour, 20.minute),
      FrequencyRecord("SUB_SATURDAY_NorthSouth", 5.hour, 23.hour, 20.minute),
      FrequencyRecord("SUB_SUNDAY_EastWest", 5.hour, 23.hour, 30.minute),
      FrequencyRecord("SUB_SUNDAY_NorthSouth", 5.hour, 23.hour, 30.minute),

      // Bus Weekend, frequency, 5AM - 11 PM, every hour
      FrequencyRecord("BUS_WEEKEND_WestBus", 5.hour, 23.hour, 1.hour),
      FrequencyRecord("BUS_WEEKEND_EastBus", 5.hour, 23.hour, 1.hour),

      // Rail Weekend, frequency, 5AM - 11 PM, every hour
      FrequencyRecord("RAIL_WEEKEND_WestRail", 5.hour, 23.hour, 1.hour),
      FrequencyRecord("RAIL_WEEKEND_EastRail", 5.hour, 23.hour, 1.hour)
  )

  def tripShapes: Seq[TripShape] = List(
    TripShape("SUB_EastWest_SHAPE", Line(List(stopWest, stopCenter, stopEast)).withSRID(4326)),
    TripShape("SUB_NorthSouth_SHAPE", Line(List(stopNorth, stopCenter, stopSouth)).withSRID(4326))
  )
}
