package opentransitgt

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.azavea.gtfs.slick._
import com.github.nscala_time.time.Imports._
import com.github.tototoshi.slick.PostgresJodaSupport
import com.typesafe.config.{ConfigFactory,Config}
import geotrellis.feature.reproject._
import geotrellis.process.{Error, Complete}
import geotrellis.proj4._
import geotrellis.render.ColorRamps
import geotrellis.slick._
import geotrellis.source.{ValueSource, RasterSource}
import geotrellis.statistics.Histogram
import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.JdbcBackend.{Database, Session}
import spray.http.MediaTypes
import spray.http.StatusCodes.InternalServerError
import spray.routing.{ExceptionHandler, HttpService}
import spray.util.LoggingContext

trait GeoTrellisService extends HttpService {
  val config = ConfigFactory.load
  val dbName = config.getString("database.name")
  val dbUser = config.getString("database.user")
  val dbPassword = config.getString("database.password")

  // All the routes that are defined by our API
  def rootRoute = pingRoute ~ gtfsRoute

  // Database setup
  val dao = new DAO()
  val db = Database.forURL(s"jdbc:postgresql:$dbName", driver = "org.postgresql.Driver",
    user = dbUser, password = dbPassword)

  // Endpoint for testing: browsing to /ping should return the text
  def pingRoute =
    pathPrefix("gt") {
      path("ping") {
        get {
          complete("pong!")
        }
      }
    }

  // Endpoint for uploading a GTFS file
  def gtfsRoute =
    pathPrefix("gt") {
      path("gtfs") {
        post {
          parameter('gtfsDir.as[String]) { gtfsDir =>
            complete {
              db withSession { implicit session: Session =>
                println(s"parsing GTFS data from: $gtfsDir")

                // parse the GTFS gtfsData
                val gtfsData = GtfsData.fromFile(gtfsDir)

                // insert routes into the gtfsDatabase
                gtfsData.routes.foreach { route => dao.routes.insert(route) }
                gtfsData.service.foreach { service => dao.service.insert(service) }
                gtfsData.agencies.foreach { agency => dao.agencies.insert(agency) }
                gtfsData.trips.foreach { trip => dao.trips.insert(trip) }
                gtfsData.shapes.foreach { shape => dao.shapes.insert(shape) }
                gtfsData.stops.foreach { stop => dao.stops.insert(stop) }

                println("finished parsing GTFS data")
                s"""{ "success": true, "message": "Imported ${gtfsData.routes.size} routes" }"""
              }
            }
          }
        }
      }
    }

  // This will be picked up by the runRoute(_) and used to intercept Exceptions
  implicit def OpenTransitGeoTrellisExceptionHandler(implicit log: LoggingContext) =
    ExceptionHandler {
      case e: Exception =>
        requestUri { uri =>
          // print error message and stack trace to console so we dont have to go a-hunting
          // in the celery job logs
          println(e.getMessage)
          println(e.getStackTrace.mkString("\n"))
          // replace double quotes with single so our message is more json safe
          val jsonMessage = e.getMessage.replace("\"", "'")
          complete(InternalServerError, s"""{ "success": false, "message": "${jsonMessage}" }""" )
        }
    }
}
