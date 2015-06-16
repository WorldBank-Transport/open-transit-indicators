package com.azavea.opentransit.database

import com.azavea.opentransit._

import scala.slick.driver.{JdbcDriver, JdbcProfile, PostgresDriver}
import scala.slick.jdbc.JdbcBackend.DatabaseDef

import com.typesafe.config.{ConfigFactory, Config}

sealed trait CSVStatus { val stringRep: String }
case object Success extends CSVStatus { val stringRep="success" }
case object Processing extends CSVStatus { val stringRep="processing" }
case object Failure extends CSVStatus { val stringRep="failure" }
case object Empty extends CSVStatus { val stringRep="empty" }

object CSVStatus {
  def apply(stringRep: String) = stringRep match {
    case "success" => Success
    case "processing" => Processing
    case "failure" => Failure
    case _ => Empty
  }
}

case class CSVJob(status: CSVStatus, data: Array[Byte] = Array.empty)

object StationCSVStorage {
  import PostgresDriver.simple._
  // Configuration
  private val config = ConfigFactory.load()
  private val dbName = config.getString("database.name")
  private val dbi = new ProductionDatabaseInstance {}
  private val db: DatabaseDef = dbi.dbByName(dbName)

  def serialize(csvJob: CSVJob): Option[(Int, String, Array[Byte])] =
    Some((1, csvJob.status.stringRep, csvJob.data))
  def deserialize(csvJobTuple: (Int, String, Array[Byte])): CSVJob =
    CSVJob(CSVStatus(csvJobTuple._2), csvJobTuple._3)

  class stationCSVTable(tag: Tag) extends Table[CSVJob](tag, "station_csv") {
    def id = column[Int]("feature_id")
    def status = column[String]("status")
    def data = column[Array[Byte]]("data")
    def * = (id, status, data) <> (deserialize, serialize)
  }
  val csvStore = TableQuery[stationCSVTable]


  def get(): Option[CSVJob] = db withSession { implicit session =>
    csvStore.firstOption
  }
  def set(csvJob: CSVJob): Unit = db withSession { implicit session =>
    csvStore += csvJob
  }
}
