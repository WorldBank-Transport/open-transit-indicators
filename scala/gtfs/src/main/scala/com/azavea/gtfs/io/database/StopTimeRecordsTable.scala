package com.azavea.gtfs.io.database

import com.azavea.gtfs._
import com.github.nscala_time.time.Imports._
import geotrellis.vector._
import geotrellis.slick._

trait StopTimeRecordsTable { this: Profile  =>
  import profile.simple._

  class StopTimeRecords(tag: Tag) extends Table[StopTimeRecord](tag, "gtfs_stop_times") {
    def stop_id = column[String]("stop_id")
    def trip_id = column[String]("trip_id")
    def stop_sequence = column[Int]("stop_sequence")
    def arrival_time = column[Period]("arrival_time")
    def departure_time = column[Period]("departure_time")
    def shape_dist_traveled = column[Option[Double]]("shape_dist_traveled")

    def * = (stop_id, trip_id, stop_sequence, arrival_time, departure_time, shape_dist_traveled) <>
      (StopTimeRecord.tupled, StopTimeRecord.unapply)
  }
  val stopTimeRecordsTable = TableQuery[StopTimeRecords]
}
