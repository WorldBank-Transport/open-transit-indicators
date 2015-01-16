package com.azavea.gtfs.io.database

import scala.slick.driver.{JdbcDriver, JdbcProfile, PostgresDriver}
import com.github.tototoshi.slick.GenericJodaSupport

import com.github.nscala_time.time.Imports._
import org.joda.time.format.PeriodFormatterBuilder
import com.azavea.gtfs.RouteType
import geotrellis.slick.PostGisProjectionSupport

object Profile {
  val defaultGeomColumnName = "the_geom"
  val defaultAgencyTableName = "gtfs_agency"
  val defaultDatesTableName = "gtfs_calendar_dates"
  val defaultCalendarTableName = "gtfs_calendar"
  val defaultFrequencyTableName = "gtfs_frequencies"
  val defaultRoutesTableName = "gtfs_routes"
  val defaultStopsTableName = "gtfs_stops"
  val defaultStopTimesTableName = "gtfs_stop_times"
  val defaultTripRecordsTableName = "gtfs_trips"
  val defaultTripShapesTableName = "gtfs_shape_geoms"
}

trait Profile {
  val profile = PostgresDriver
  val joda = new GenericJodaSupport(profile)
  val gis = new PostGisProjectionSupport(profile)

  val geomColumnName: String
  val agencyTableName: String
  val datesTableName: String
  val calendarTableName: String
  val frequencyTableName: String
  val routesTableName: String
  val stopsTableName: String
  val stopTimesTableName: String
  val tripRecordsTableName: String
  val tripShapesTableName: String

  import profile.simple._

  /** Periods are expressed in HH:MM:SS */
  implicit val periodColumnType = {
    val formatter = new PeriodFormatterBuilder()
      .minimumPrintedDigits(2)
      .printZeroAlways()
      .appendHours().appendSuffix(":")
      .appendMinutes().appendSuffix(":")
      .appendSeconds()
      .toFormatter

    MappedColumnType.base[Period, String](
      { period => period.toString(formatter) },
      { text => formatter.parsePeriod(text) }
    )
  }

  implicit val durationColumnType =
    MappedColumnType.base[Duration, Int](
      { duration => duration.getStandardSeconds.toInt },
      { int => int.seconds }
    )

  implicit val routeTypeColumnType =
    MappedColumnType.base[RouteType, Int](
      { rt => rt.id },
      { int => RouteType(int) }
    )
}

trait DefaultProfile extends Profile {
  val geomColumnName = Profile.defaultGeomColumnName
  val agencyTableName = Profile.defaultAgencyTableName
  val datesTableName = Profile.defaultDatesTableName
  val calendarTableName = Profile.defaultCalendarTableName
  val frequencyTableName = Profile.defaultFrequencyTableName
  val routesTableName = Profile.defaultRoutesTableName
  val stopsTableName = Profile.defaultStopsTableName
  val stopTimesTableName = Profile.defaultStopTimesTableName
  val tripRecordsTableName = Profile.defaultTripRecordsTableName
  val tripShapesTableName = Profile.defaultTripShapesTableName
}
