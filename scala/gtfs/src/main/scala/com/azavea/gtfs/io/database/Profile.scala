package com.azavea.gtfs.io.database

import scala.slick.driver.{JdbcDriver, JdbcProfile, PostgresDriver}
import com.github.tototoshi.slick.GenericJodaSupport

import com.github.nscala_time.time.Imports._
import org.joda.time.format.PeriodFormatterBuilder
import com.azavea.gtfs.RouteType
import geotrellis.slick.PostGisProjectionSupport

trait Profile {
  val profile = PostgresDriver
  val joda = new GenericJodaSupport(profile)
  val gis = new PostGisProjectionSupport(profile)
  var geomColumnName = "the_geom"

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
