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

case class CSVJob(
  status: CSVStatus,
  bufferDistance: Double,
  commuteTime: Int,
  data: Array[Byte] = Array.empty)

trait StationCSVStore {
  def get(dbName: String): Option[CSVJob]
  def set(csvJob: CSVJob, dbName: String): Unit
}

/**
 * This object provides very simple storage for CSV data and CSV creation status
**/
object StationCSVDatabase extends StationCSVStore {
  import PostgresDriver.simple._
  // Configuration
  private val dbi = new ProductionDatabaseInstance {}

  def serialize(csvJob: CSVJob): Option[(Int, String, Double, Int, Array[Byte])] =
    Some((1, csvJob.status.stringRep, csvJob.bufferDistance, csvJob.commuteTime, csvJob.data))
  def deserialize(csvJobTuple: (Int, String, Double, Int, Array[Byte])): CSVJob =
    CSVJob(CSVStatus(csvJobTuple._2), csvJobTuple._3, csvJobTuple._4, csvJobTuple._5)

  class stationCSVTable(tag: Tag) extends Table[CSVJob](tag, "station_csv") {
    def id = column[Int]("id")
    def status = column[String]("status")
    def bufferDistance = column[Double]("buffer_distance")
    def commuteTime = column[Int]("commute_time")
    def data = column[Array[Byte]]("data")
    def * = (id, status, bufferDistance, commuteTime, data) <> (deserialize, serialize)
  }
  val csvStore = TableQuery[stationCSVTable]


  def get(dbName: String): Option[CSVJob] = dbi.dbByName(dbName) withSession { implicit session =>
    csvStore.firstOption
  }

  def set(csvJob: CSVJob, dbName: String): Unit = dbi.dbByName(dbName) withSession { implicit session =>
    csvStore.firstOption match {
      case Some(_) => csvStore.update(csvJob)
      case None => csvStore.insert(csvJob)
    }
  }
}
