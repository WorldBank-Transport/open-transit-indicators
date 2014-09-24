package com.azavea.opentransit.service

import com.azavea.opentransit._
import com.azavea.opentransit.indicators.{ CalculateIndicators, IndicatorCalculationRequest }
import com.azavea.opentransit.DjangoAdapter._

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
import spray.http.StatusCodes.{Created, InternalServerError}
import spray.routing.{ExceptionHandler, HttpService}
import spray.util.LoggingContext
import scala.concurrent._

// JSON support
import spray.json._
import spray.httpx.SprayJsonSupport
import SprayJsonSupport._
import DefaultJsonProtocol._

import org.joda.time.format.ISODateTimeFormat
import com.typesafe.config.{ConfigFactory, Config}

trait GtfsDatabase {
  val db: DatabaseDef
}

trait ProductionGtfsDatabase extends GtfsDatabase {
  val db = {
    val config = ConfigFactory.load
    val dbName = config.getString("database.name")
    val dbUser = config.getString("database.user")
    val dbPassword = config.getString("database.password")
    Database.forURL(s"jdbc:postgresql:$dbName", driver = "org.postgresql.Driver",
      user = dbUser, password = dbPassword)
  }
}

trait IndicatorsRoute extends Route { self: GtfsDatabase =>
  val config = ConfigFactory.load
  val dbGeomNameUtm = config.getString("database.geom-name-utm")

  // Endpoint for triggering indicator calculations
  //
  // TODO: Add queue management. Calculation request jobs will be stored
  //   in a table, and calculations will be run one (or more) at a time
  //   in the background via an Actor.
  def indicatorsRoute = {
    // need the implicits we defined for custom JSON parsing
    import JsonImplicits._

    pathPrefix("gt") {
      path("indicators") {
        post {
          entity(as[IndicatorCalculationRequest]) { request =>
            complete {
              try {

                // def gtfsData(): GtfsData = {
                //   val config = ConfigFactory.load
                //   val dbGeomNameUtm = config.getString("database.geom-name-utm")

                //   db withSession { implicit session =>
                //     val dao = new DAO()
                //     // data is read from the db as the reprojected UTM projection
                //     dao.geomColumnName = dbGeomNameUtm
                //     dao.toGtfsData
                //   }
                // }

                TaskQueue.execute {
//                  println("Going to start calculating things now...")
//                  println("Loading GTFS data...")
//                  val data = gtfsData()
//                  println("Calculating indicators...")

                  // Load Gtfs records from the database. Load it with UTM projection (column 'geom' in the database)
                  val gtfsRecords =
                    db withSession { implicit session =>
                      GtfsRecords.fromDatabase(dbGeomNameUtm)
                    }

                  CalculateIndicators(request.sample_periods, gtfsRecords) { containerGenerators =>
                    val indicatorResultContainers = containerGenerators.map(_.toContainer(request.version))
                    djangoClient.postIndicators(request.token, indicatorResultContainers)
                  }
//                  println("Indicators calcuated; going to call storeIndicators...")
//                  calc.storeIndicators
                  // Update indicator-job status

                  djangoClient.updateIndicatorJob(request.token, IndicatorJob(version=request.version, job_status="complete"))
                }

                println("Have started calculating things!")

                // return a 201 created
                Created -> JsObject(
                  "success" -> JsBoolean(true),
                  "message" -> JsString(s"Calculations started (version ${request.version})")
                )
              } catch {
                case _: Exception =>
                  JsObject(
                    "success" -> JsBoolean(false),
                    "message" -> JsString("No GTFS data")
                  )
              }
            }
          }
        }
      }
    }
  }
}
