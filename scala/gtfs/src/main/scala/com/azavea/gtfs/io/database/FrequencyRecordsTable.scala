package com.azavea.gtfs.io.database

import com.azavea.gtfs._
import com.github.nscala_time.time.Imports._
import geotrellis.vector._
import geotrellis.slick._

trait FrequencyRecordsTable {this: Profile  =>
  import profile.simple._

  class FrequencyRecords(tag: Tag) extends Table[FrequencyRecord](tag, "gtfs_frequencies") {
    def trip_id = column[String]("trip_id")
    def start_time = column[Period]("start_time")
    def end_time = column[Period]("end_time")
    def headway = column[Duration]("headway_secs")

    def * = (trip_id, start_time, end_time, headway) <> (FrequencyRecord.tupled, FrequencyRecord.unapply)
  }

  val frequencyRecordsTable = TableQuery[FrequencyRecords]
}
