package com.azavea.opentransit.scenarios

import grizzled.slf4j.Logging

import scala.slick.jdbc.JdbcBackend.{Database, Session, DatabaseDef}
import scala.sys.process._
import com.azavea.gtfs._
import com.azavea.opentransit.JobStatus
import com.azavea.opentransit.JobStatus._
import com.azavea.opentransit._
import com.azavea.opentransit.io._
import com.typesafe.config.{ConfigFactory,Config}

import scala.util.Try

object CreateScenario extends Logging {
  /** Creates a new scenario and passes status to a sink function. */
  def apply(request: ScenarioCreationRequest, dbByName: String => Database): Unit =  {
    logger.info(s"Creating scenario with database: ${request.dbName}")

    // Initialize the new scenario database via the setup_db script
    val config = ConfigFactory.load
    val dbUser = config.getString("database.user")
    val dbPassword = config.getString("database.password")

    s"sudo -u postgres ../deployment/setup_db.sh ${request.dbName} $dbUser $dbPassword ..".!!

    // Copy GTFS data from the old database to the new database.
    logger.info(s"Obtaining gtfs records from base database: ${request.baseDbName}")
    val gtfsRecords = dbByName(request.baseDbName) withSession { implicit session =>
      GtfsRecords.fromDatabase
    }
    // TODO: as an optimization, the sample period may be used here to filter out irrelevant data
    logger.info(s"Pushing gtfs records to new database: ${request.dbName}")
    dbByName(request.dbName) withSession { implicit session => GtfsIngest(gtfsRecords) }
    logger.info(s"Scenario creation complete: '${request.dbName}'")
  }
}
