package com.azavea.opentransit.scenarios

import scala.slick.jdbc.JdbcBackend.{Database, Session, DatabaseDef}
import scala.sys.process._
import com.azavea.gtfs._
import com.azavea.opentransit.JobStatus
import com.azavea.opentransit.JobStatus._
import com.azavea.opentransit._
import com.azavea.opentransit.io._
import com.typesafe.config.{ConfigFactory,Config}

object CreateScenario {
  /** Creates a new scenario and passes status to a sink function.
    */
  def apply(request: ScenarioCreationRequest, dbByName: String => Database)
    (sink: JobStatus => Unit): Unit = {

    // Initialize the new scenario database via the setup_db script
    val config = ConfigFactory.load
    val dbUser = config.getString("database.user")
    val dbPassword = config.getString("database.password")

    println("Creating new scenario database")
    s"sudo -u postgres ../deployment/setup_db.sh ${request.dbName} $dbUser $dbPassword ..".!!

    // Copy GTFS data from the old database to the new database.
    println(s"Obtaining records from base database: ${request.baseDbName}")
    val gtfsRecords = dbByName(request.baseDbName) withSession { implicit session =>
      GtfsRecords.fromDatabase
    }
    // TODO: as an optimization, the sample period may be used here to filter out irrelevant data

    println(s"Pushing to new database: ${request.dbName}")
    dbByName(request.dbName) withSession { implicit session => GtfsIngest(gtfsRecords) }

    println("Scenario creation complete, updating status")
    sink(JobStatus.Complete)
  }
}
