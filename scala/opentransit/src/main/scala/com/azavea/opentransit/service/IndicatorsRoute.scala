package com.azavea.opentransit.service

import com.azavea.opentransit._
import com.azavea.opentransit.JobStatus
import com.azavea.opentransit.JobStatus._
import com.azavea.opentransit.json._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.database.{IndicatorsTable, IndicatorJobsTable}

import com.azavea.gtfs._

import com.github.nscala_time.time.Imports._
import com.github.tototoshi.slick.PostgresJodaSupport

import geotrellis.proj4._
import geotrellis.slick._

import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.{Database, Session, DatabaseDef}
import scala.slick.jdbc.meta.MTable

import spray.http.MediaTypes
import spray.http.StatusCodes.{Accepted, InternalServerError}
import spray.routing.{ExceptionHandler, HttpService}
import spray.util.LoggingContext
import scala.concurrent._

// JSON support
import spray.json._
import spray.httpx.SprayJsonSupport
import SprayJsonSupport._
import DefaultJsonProtocol._

import scala.util.{Success, Failure}

import com.typesafe.config.{ConfigFactory, Config}

case class IndicatorJob(
  id: Int,
  status: Map[String, Map[String, JobStatus]]
)

trait IndicatorsRoute extends Route { self: DatabaseInstance with DjangoClientComponent =>
  val config = ConfigFactory.load
  val mainDbName = config.getString("database.name")
  val indicatorJobs = new IndicatorJobsTable {}

  def handleIndicatorsRequest(
    request: IndicatorCalculationRequest,
    dbByName: String => Database
  ): Unit = {
    CalculateIndicators(request, dbByName, new CalculationStatusManager
      with IndicatorsTable {

      def indicatorFinished(containerGenerators: Seq[ContainerGenerator]) = {
        try {
          val indicatorResultContainers = containerGenerators.map(_.toContainer(request.id))
          dbByName(mainDbName) withTransaction { implicit session =>
            import PostgresDriver.simple._
              indicatorsTable.forceInsertAll(indicatorResultContainers:_*)
            }
        } catch {
          case e: java.sql.SQLException => {
            println(e.getNextException())
          }
        }
      }
      def statusChanged(status: Map[String, Map[String, JobStatus]]) = {
        dbByName(mainDbName) withTransaction { implicit session =>
          indicatorJobs.updateCalcStatus(IndicatorJob(request.id, status))
        }
      }
    })
  }

  // Endpoint for triggering indicator calculations
  //
  // TODO: Add queue management. Calculation request jobs will be stored
  //   in a table, and calculations will be run one (or more) at a time
  //   in the background via an Actor.
  def indicatorsRoute = {
    pathEnd {
      post {
        entity(as[IndicatorCalculationRequest]) { request =>
          complete {
            TaskQueue.execute { // async
              handleIndicatorsRequest(request, dbByName)
            }.onComplete { // TaskQueue callback for result handling
              case Success(_) =>
                println(s"TaskQueue successfully completed - indicator finished")
              case Failure(e) =>
                dbByName(mainDbName) withTransaction { implicit session =>
                  indicatorJobs.failJob(request.id, "calculation_error")
                }
                println("Error calculating indicators!")
                println(e.getMessage)
                println(e.getStackTrace.mkString("\n"))
            }
            Accepted -> JsObject(
              "success" -> JsBoolean(true),
              "message" -> JsString(s"Calculations started (id: ${request.id})")
            )
          }
        }
      }
    }
  }
}
