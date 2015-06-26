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
  jobId: Int,
  data: Array[Byte] = Array.empty)

trait StationCSVStore {
  def get(jobId: Int): Option[CSVJob]
  def set(csvJob: CSVJob): Unit
}

/**
 * This object provides very simple storage for CSV data and CSV creation status
**/
object StationCSVDatabase extends StationCSVStore {
  import PostgresDriver.simple._
  // Configuration
  val config = ConfigFactory.load()
  val dbName = config.getString("database.name")
  private val dbi = new ProductionDatabaseInstance {}
  val db = dbi.dbByName(dbName)

  def serialize(csvJob: CSVJob): Option[(Int, String, Double, Int, Array[Byte])] =
    Some((csvJob.jobId, csvJob.status.stringRep, csvJob.bufferDistance, csvJob.commuteTime, csvJob.data))
  def deserialize(csvJobTuple: (Int, String, Double, Int, Array[Byte])): CSVJob =
    CSVJob(CSVStatus(csvJobTuple._2), csvJobTuple._3, csvJobTuple._4, csvJobTuple._1, csvJobTuple._5)

  class stationCSVTable(tag: Tag) extends Table[CSVJob](tag, "station_csv") {
    def id = column[Int]("id", O.PrimaryKey)
    def status = column[String]("status")
    def bufferDistance = column[Double]("buffer_distance")
    def commuteTime = column[Int]("commute_time")
    def data = column[Array[Byte]]("data")
    def * = (id, status, bufferDistance, commuteTime, data) <> (deserialize, serialize)
  }
  val csvStore = TableQuery[stationCSVTable]


  def get(jobId: Int): Option[CSVJob] = db withSession { implicit session =>
    csvStore.filter(_.id === jobId).firstOption
  }

  def set(csvJob: CSVJob): Unit = db withSession { implicit session =>
    val query = csvStore.filter(_.id === csvJob.jobId)
    query.firstOption match {
      case Some(_) => query.update(csvJob)
      case None => csvStore.insert(csvJob)
    }
  }
}
