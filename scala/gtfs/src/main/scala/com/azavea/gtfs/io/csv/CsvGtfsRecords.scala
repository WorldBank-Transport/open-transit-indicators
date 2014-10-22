package com.azavea.gtfs.io.csv

import com.azavea.gtfs._

import com.github.nscala_time.time.Imports._
import geotrellis.vector._
import geotrellis.slick._

import scala.collection.mutable
import java.io.{ File, IOException }

class GtfsFormatException(msg: String) extends Exception(msg)

object CsvGtfsRecords {
  def apply(directory: String): CsvGtfsRecords = 
    new CsvGtfsRecords(directory)
}

/**
 * Reads GTFS data from .txt files
 * @param dir directory containing the files
 */
class CsvGtfsRecords(dir: String) extends GtfsRecords {
  private def assertExists() = {
    val f = new File(dir)
    if(!f.exists)
      throw new IOException(s"Directory ${f.getAbsolutePath} does not exist.")
    if(!f.isDirectory)
      throw new IOException(s"${f.getAbsolutePath} is not a directory.")
  }
  assertExists()


  def parse[T](gtfsFile: GtfsFile[T]): Seq[T] = {
    val path = s"$dir/${gtfsFile.fileName}"
    if(! new File(path).exists) {
      if(gtfsFile.isRequired) {
        throw new GtfsFormatException(
          s"${new File(path).getAbsolutePath} is a required file that is not found."
        )
      } else {
        Seq[T]()
      }
    } else {
      gtfsFile.parse(path)
    }
  }

  def agencies: Seq[Agency] =
    parse(AgencyFile)

  def routeRecords: Seq[RouteRecord] = 
    parse(RoutesFile)

  def tripRecords: Seq[TripRecord] =
    parse(TripsFile)

  def stopTimeRecords: Seq[StopTimeRecord] = 
    parse(StopTimesFile)

  def calendarRecords: Seq[CalendarRecord] = 
    parse(CalendarFile)

  def calendarDateRecords: Seq[CalendarDateRecord] = 
    parse(CalendarDatesFile)

  def tripShapes: Seq[TripShape] =
    parse(ShapesFile)

  def frequencyRecords: Seq[FrequencyRecord] = 
    parse(FrequenciesFile)

  def stops: Seq[Stop] =
    parse(StopsFile)
}
