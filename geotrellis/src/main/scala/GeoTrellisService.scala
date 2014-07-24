package opentransitgt

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.azavea.gtfs.slick._
import com.github.nscala_time.time.Imports._
import com.github.tototoshi.slick.PostgresJodaSupport
import com.typesafe.config.{ConfigFactory,Config}
import geotrellis.process.{Error, Complete}
import geotrellis.proj4._
import geotrellis.render.ColorRamps
import geotrellis.slick._
import geotrellis.source.{ValueSource, RasterSource}
import geotrellis.statistics.Histogram
import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.{Database, Session}
import spray.http.MediaTypes
import spray.http.StatusCodes.InternalServerError
import spray.routing.{ExceptionHandler, HttpService}
import spray.util.LoggingContext

// JSON support
import spray.json._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._

trait GeoTrellisService extends HttpService {
  val config = ConfigFactory.load
  val dbName = config.getString("database.name")
  val dbUser = config.getString("database.user")
  val dbPassword = config.getString("database.password")

  // For performing extent queries
  case class Extent(xmin: Double, xmax: Double, ymin: Double, ymax: Double)
  implicit val getExtentResult = GetResult(r => Extent(r.<<, r.<<, r.<<, r.<<))

  // All the routes that are defined by our API
  def rootRoute = pingRoute ~ gtfsRoute ~ indicatorsRoute ~ mapInfoRoute

  // Database setup
  val dao = new DAO()
  val db = Database.forURL(s"jdbc:postgresql:$dbName", driver = "org.postgresql.Driver",
    user = dbUser, password = dbPassword)

  // Object for calculating indicators
  var indicatorsCalculator: Option[IndicatorsCalculator] = None

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

                // parse the GTFS gtfsData and create the indicators calculator
                var gtfsData = GtfsData.fromFile(gtfsDir)
                indicatorsCalculator = Some(new IndicatorsCalculator(gtfsData))

                // insert GTFS data into the database
                gtfsData.routes.foreach { route => dao.routes.insert(route) }
                gtfsData.service.foreach { service => dao.service.insert(service) }
                gtfsData.agencies.foreach { agency => dao.agencies.insert(agency) }
                gtfsData.trips.foreach { trip => dao.trips.insert(trip) }
                gtfsData.shapes.foreach { shape => dao.shapes.insert(shape) }
                gtfsData.stops.foreach { stop => dao.stops.insert(stop) }
                println("finished parsing GTFS data")

                JsObject(
                  "success" -> JsBoolean(true),
                  "message" -> JsString(s"Imported ${gtfsData.routes.size} routes")
                )
              }
            }
          }
        }
      }
    }

  // Endpoint for obtaining indicators
  def indicatorsRoute =
    pathPrefix("gt") {
      path("indicators") {
        get {
          complete {
            indicatorsCalculator match {
              case None => {
                JsObject(
                  "success" -> JsBoolean(false),
                  "message" -> JsString("No indicators calculator")
                )
              }
              case Some(calc) => {
                JsObject(
                  "success" -> JsBoolean(true),
                  "indicators" -> JsObject(
                    "numRoutesPerMode" -> calc.numRoutesPerMode.toJson,
                    "maxStopsPerRoute" -> calc.maxStopsPerRoute.toJson,
                    "numStopsPerMode" -> calc.numStopsPerMode.toJson,
                    "avgTransitLengthPerMode" -> calc.avgTransitLengthPerMode.toJson
                  )
                )
              }
            }
          }
        }
      }
    }

  // Endpoint for obtaining map info (just extent for now)
  def mapInfoRoute =
    pathPrefix("gt") {
      path("map-info") {
        get {
          complete {
            db withSession { implicit session: Session =>
              // use the stops to find the extent, since they are required
              val q = Q.queryNA[Extent]("""
                SELECT ST_XMIN(ST_Extent(the_geom)) as xmin, ST_XMAX(ST_Extent(the_geom)) as xmax,
                       ST_YMIN(ST_Extent(the_geom)) as ymin, ST_YMAX(ST_Extent(the_geom)) as ymax
                FROM gtfs_stops;
              """)
              val extent = q.list.head

              // construct the extent json, using null if no data is available
              val extentJson = extent match {
                case Extent(0, 0, 0, 0) =>
                  JsNull
                case _ =>
                  JsObject(
                    "southWest" -> JsObject(
                      "lat" -> JsNumber(extent.ymin),
                      "lng" -> JsNumber(extent.xmin)
                    ),
                    "northEast" -> JsObject(
                      "lat" -> JsNumber(extent.ymax),
                      "lng" -> JsNumber(extent.xmax)
                    )
                  )
              }

              // return the map info json
              JsObject("extent" -> extentJson)
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
