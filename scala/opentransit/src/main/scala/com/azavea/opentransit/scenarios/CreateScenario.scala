package com.azavea.opentransit.scenarios

import com.azavea.gtfs.Timer._
import com.azavea.gtfs.io.database.{GtfsTables, DefaultProfile}
import com.azavea.opentransit.DatabaseInstance
import grizzled.slf4j.Logging

import scala.slick.jdbc.JdbcBackend.{Database, Session, DatabaseDef}
import scala.sys.process._
import com.azavea.gtfs._
import com.azavea.gtfs.op._
import com.azavea.opentransit.io._
import com.typesafe.config.{ConfigFactory,Config}

import scala.util.Try

object CreateScenario extends Logging {
  /** Creates a new scenario and passes status to a sink function. */
  def apply(request: ScenarioCreationRequest, dbByName: String=>Database, createDatabase: String=>Unit): Unit =  {
    logger.info(s"Creating scenario with database: ${request.dbName}")

    // Initialize the new scenario database via the setup_db script
    val config = ConfigFactory.load
    val dbUser = config.getString("database.user")
    val dbPassword = config.getString("database.password")

    createDatabase(request.dbName)

    // Copy GTFS data from the old database to the new database.
    logger.info(s"Obtaining gtfs records from base database: ${request.baseDbName}")
    val gtfsRecords = dbByName(request.baseDbName) withSession { implicit session =>
      GtfsRecords.fromDatabase
    }

    logger.info(s"Pushing gtfs records to new database: ${request.dbName}")

    val tables = new GtfsTables with DefaultProfile
    dbByName(request.dbName) withSession { implicit session =>
      import tables.profile.simple._
      val start = request.samplePeriod.start
      val end = request.samplePeriod.end
      val filtered = timedTask("Filtering GTFS records for scenario") {gtfsRecords.filter(start, end)}
      GtfsIngest(filtered)

      tables.calendarRecordsTable insert
        CalendarRecord("ALWAYS",start.toLocalDate, end.toLocalDate, Array.fill(7)(true))
    }
    logger.info(s"Scenario creation complete: '${request.dbName}'")
  }
}
