package com.azavea.gtfs.io.database

import com.azavea.gtfs._
import com.azavea.gtfs.Timer.timedTask

import scala.slick.jdbc.StaticQuery
import scala.slick.jdbc.JdbcBackend.Session

object DatabaseRecordImport {
  def apply(records: GtfsRecords, geomColumnName: String = Profile.defaultGeomColumnName, clobber: Boolean = true)(implicit session: Session): Unit =
    new DatabaseRecordImport(geomColumnName).load(records, clobber)
}

class DatabaseRecordImport(override val geomColumnName: String = Profile.defaultGeomColumnName)(implicit session: Session) extends GtfsTables with DefaultProfile {
  import profile.simple._

  private def load[T, U <: Table[T]](records: Seq[T], table: TableQuery[U]): Unit = {
    session.withTransaction {
      records.grouped(1000).foreach(batch => if (batch.size > 0) table.forceInsertAll(batch:_*))
    }
  }

  // Run function to populate routes served by each stop
  private def populateRoutesServed(): Unit = {
    session.withTransaction {
      StaticQuery.updateNA("""SELECT stops_routes();""").execute
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

    timedTask("loaded agencies") { load(records.agencies, agenciesTable) }
    timedTask("loaded calendar dates") {
      load(records.calendarDateRecords, calendarDateRecordsTable)
    }
    timedTask("loaded calendar") { load(records.calendarRecords, calendarRecordsTable) }
    timedTask("loaded routes") { load(records.routeRecords, routeRecordsTable) }
    timedTask("loaded trips") { load(records.tripRecords, tripRecordsTable) }
    timedTask("loaded stop times") {
      load(records.stopTimeRecords.view.map(ensureNoNullPeriods), stopTimeRecordsTable)
    }
    timedTask("loaded frequencies") { load(records.frequencyRecords, frequencyRecordsTable) }
    timedTask("loaded trip shapes") { load(records.tripShapes, tripShapesTable) }
    timedTask("loaded stops") { load(records.stops, stopsTable) }
    timedTask("populated routes served by each stop") { populateRoutesServed }
  }
}
