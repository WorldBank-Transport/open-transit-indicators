package com.azavea.gtfs.io.database

import com.azavea.gtfs._
import com.github.nscala_time.time.Imports._
import geotrellis.vector._
import geotrellis.slick._

trait TripRecordsTable {this: Profile  =>
  import profile.simple._

  class TripRecords(tag: Tag)
      extends Table[TripRecord](tag, "gtfs_trips") {
    def id = column[String]("trip_id")
    def service_id = column[String]("service_id")
    def route_id = column[String]("route_id")
    def trip_headsign = column[Option[String]]("trip_headsign")
    def shape_id = column[Option[String]]("shape_id")

    def * = (id, service_id, route_id, trip_headsign, shape_id) <> (TripRecord.tupled, TripRecord.unapply)
  }
  val tripRecordsTable = TableQuery[TripRecords]
}
