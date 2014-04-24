package opentransitgt

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.azavea.gtfs.slick._
import com.github.nscala_time.time.Imports._
import com.github.tototoshi.slick.PostgresJodaSupport
import com.typesafe.config.{ConfigFactory,Config}
import geotrellis._
import geotrellis.process.{Error, Complete}
import geotrellis.render.ColorRamps
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
  val dao = new DAO(PostgresDriver, PostgresJodaSupport)
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

                // TODO: only routes are working/implemented with the gtfs-parser library
                // at the moment. Need to insert all other components when available.
                // E.g. here is what the code for inserting trips will look like:
                //gtfsData.trips.foreach { trip => dao.trips.insert(trip) }

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
          complete(InternalServerError, s"Message: ${e.getMessage}\n Trace: ${e.getStackTrace.mkString("</br>")}" )
        }
    }
}
