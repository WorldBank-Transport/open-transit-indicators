package com.azavea.gtfs.io.database

import com.azavea.gtfs._

import scala.slick.jdbc.JdbcBackend.Session

object DatabaseRecordImport {
  def apply(records: GtfsRecords, geomColumnName: String = Profile.defaultGeomColumnName, clobber: Boolean = true)(implicit session: Session): Unit =
    new DatabaseRecordImport(geomColumnName).load(records, clobber)
}

class DatabaseRecordImport(val geomColumnName: String = Profile.defaultGeomColumnName)(implicit session: Session) extends GtfsTables with Profile {
  import profile.simple._

  private def load[T, U <: Table[T]](records: Seq[T], table: TableQuery[U]): Unit = {
    for(record <- records) {
      table.forceInsert(record)
    }
  }

  private def deleteAll(): Unit = {
    println("deleting all existing records...")
    agenciesTable.delete
    stopsTable.delete
    frequencyRecordsTable.delete
    tripShapesTable.delete
    calendarDateRecordsTable.delete
    calendarRecordsTable.delete
    stopTimeRecordsTable.delete
    tripRecordsTable.delete
    routeRecordsTable.delete
  }

  def load(records: GtfsRecords, clobber: Boolean = true): Unit = {
    if(clobber) deleteAll

    def ensureNoNullPeriods(stopTime: StopTimeRecord) = {
      println("checking for null periods")
      assert(stopTime.arrivalTime != null)
      assert(stopTime.departureTime != null)
      stopTime
    }

    println("going to load agencies")
    load(records.agencies, agenciesTable)
    println("going to load stops")
    load(records.stops, stopsTable)
    println("going to load calendar dates")
    load(records.calendarDateRecords, calendarDateRecordsTable)
    println("going to load calendar")
    load(records.calendarRecords, calendarRecordsTable)
    println("going to load routes")
    load(records.routeRecords, routeRecordsTable)
    println("going to load trips")
    load(records.tripRecords, tripRecordsTable)
    println("going to load stop times")
    load(records.stopTimeRecords.view.map(ensureNoNullPeriods), stopTimeRecordsTable)
    println("going to load frequencies")
    load(records.frequencyRecords, frequencyRecordsTable)
    println("going to load trip shapes")
    load(records.tripShapes, tripShapesTable)
  }
}
