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
      assert(stopTime.arrivalTime != null)
      assert(stopTime.departureTime != null)
      stopTime
    }

    load(records.agencies, agenciesTable)
    load(records.stops, stopsTable)
    load(records.calendarDateRecords, calendarDateRecordsTable)
    load(records.calendarRecords, calendarRecordsTable)
    load(records.routeRecords, routeRecordsTable)
    load(records.tripRecords, tripRecordsTable)
    load(records.stopTimeRecords.view.map(ensureNoNullPeriods), stopTimeRecordsTable)
    load(records.frequencyRecords, frequencyRecordsTable)
    load(records.tripShapes, tripShapesTable)
  }
}
