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
  
  // time how long each step takes.  from here:
  // http://stackoverflow.com/questions/9160001/how-to-profile-methods-in-scala
  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + ((t1 - t0) / 1000000000.0) + " s")
    result
  }

  def load(records: GtfsRecords, clobber: Boolean = true): Unit = {
    if(clobber) deleteAll

    def ensureNoNullPeriods(stopTime: StopTimeRecord) = {
      assert(stopTime.arrivalTime != null)
      assert(stopTime.departureTime != null)
      stopTime
    }

    println("going to load agencies")
    time { load(records.agencies, agenciesTable) }
    println("going to load stops")
    time { load(records.stops, stopsTable) }
    println("going to load calendar dates")
    time { load(records.calendarDateRecords, calendarDateRecordsTable) }
    println("going to load calendar")
    time { load(records.calendarRecords, calendarRecordsTable) }
    println("going to load routes")
    time { load(records.routeRecords, routeRecordsTable) }
    println("going to load trips")
    time { load(records.tripRecords, tripRecordsTable) }
    println("going to load stop times")
    time { load(records.stopTimeRecords.view.map(ensureNoNullPeriods), stopTimeRecordsTable) }
    println("going to load frequencies")
    time { load(records.frequencyRecords, frequencyRecordsTable) }
    println("going to load trip shapes")
    time { load(records.tripShapes, tripShapesTable) }
  }
}
