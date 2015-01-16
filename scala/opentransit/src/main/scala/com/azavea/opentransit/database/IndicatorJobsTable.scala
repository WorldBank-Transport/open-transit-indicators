package com.azavea.opentransit.database

import com.azavea.gtfs._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.service._
import com.azavea.opentransit.JobStatus
import spray.json.DefaultJsonProtocol._

import com.github.nscala_time.time.Imports._

import scala.slick.driver.{JdbcDriver, JdbcProfile, PostgresDriver}
import scala.slick.jdbc.{StaticQuery => Q}

case class FullIndicatorJob(
  id: Int,
  calcStatus: String,
  jobStatus: String,
  errorType: String,
  creatorId: Int,
  scenarioId: Int,
  cityName: String
)

/**
 *  Trait for providing the indicatorjobs table
 */
trait IndicatorJobsTable {
  import PostgresDriver.simple._

  class IndicatorJobs(tag: Tag) extends Table[FullIndicatorJob](tag, "transit_indicators_indicatorjob") {
    def id = column[Int]("id")
    def calcStatus = column[String]("calculation_status")
    def jobStatus = column[String]("job_status")
    def errorType = column[String]("error_type")
    def creatorId = column[Int]("created_by_id")
    def scenarioId = column[Int]("scenario_id")
    def cityName = column[String]("city_name")

    def * = (id, calcStatus, jobStatus, errorType, creatorId, scenarioId, cityName) <> (FullIndicatorJob.tupled, FullIndicatorJob.unapply)
  }

  def indicatorJobsTable = TableQuery[IndicatorJobs]

  // Fail out all processing jobs
  def failProcessingJobs(reason: String)(implicit session: Session): Unit = {
    indicatorJobsTable.filter(_.jobStatus === "processing").map { fullJob =>
      fullJob.errorType
    }.update(reason)
    indicatorJobsTable.filter(_.jobStatus === "processing").map { fullJob =>
      fullJob.jobStatus
    }.update("error")
  }
  // Partially applied failAllJobs for scala restart
  def failOOMError(implicit session: Session): Unit = failProcessingJobs("scala_unknown_error")

  // Set job to processing
  def updateJobStatus(id: Int, jobStatus: String)(implicit session: Session): Unit =
    indicatorJobsTable.filter(_.id === id).map { fullJob =>
      fullJob.jobStatus
    }.update(jobStatus)

  // Set a job to failure
  def failJob(id: Int, reason: String = "")(implicit session: Session): Unit = {
    indicatorJobsTable.filter(_.jobStatus === "processing").map { fullJob =>
      fullJob.errorType
    }.update(reason)
    indicatorJobsTable.filter(_.jobStatus === "processing").map { fullJob =>
      fullJob.jobStatus
    }.update("error")
  }

  // Function for arbitrarily adding to the error-tracking column
  def updateErrorType(id: Int, errorType: String)(implicit session: Session): Unit =
    indicatorJobsTable.filter(_.id === id).map { fullJob =>
      fullJob.errorType
    }.update(errorType)

  // The indicator job parameter here is NOT the same as 'FullIndicatorJob' above!
  def updateCalcStatus(job: IndicatorJob)(implicit session: Session): Unit = {
    val hasComplete =
      job.status.map { case (period, indicatorResult) =>
        indicatorResult.forall { case (indicatorName, state) =>
          state != JobStatus.Processing && state != JobStatus.Submitted
        }
      }.foldLeft(true)(_ && _)

    val noFailed =
      !job.status.map { case (period, indicatorResult) =>
        indicatorResult.exists {case (indicatorName, state) => state == JobStatus.Failed }
      }.foldLeft(true)(_ && _)

    val jobStatus =
      if (hasComplete) {
        if (noFailed) JobStatus.Complete else JobStatus.Failed
      } else {
        JobStatus.Processing
      }

    val status = job.status.map { case (periodType, indicatorStatus) =>
      (periodType -> indicatorStatus.map { case (indicatorName, status) =>
        (indicatorName -> status.getJsonWithMsg)
      }.toMap)
    }.toMap

    updateJobStatus(job.id, jobStatus.toString)

    indicatorJobsTable.filter(_.id === job.id).map { fullJob =>
      fullJob.calcStatus
    }.update(status.toJson.toString)
  }

}
