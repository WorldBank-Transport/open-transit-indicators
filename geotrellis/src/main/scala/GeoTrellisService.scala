package opentransitgt

import opentransitgt.DjangoAdapter._

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.azavea.gtfs.slick._

import com.github.nscala_time.time.Imports._
import com.github.tototoshi.slick.PostgresJodaSupport

import geotrellis.engine.{Error, Complete}
import geotrellis.proj4._
import geotrellis.raster.render.ColorRamps
import geotrellis.slick._
import geotrellis.engine.{ValueSource, RasterSource}
import geotrellis.raster.stats.Histogram

import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.{Database, Session, DatabaseDef}
import scala.slick.jdbc.meta.MTable

import spray.http.MediaTypes
import spray.http.StatusCodes.{Created, InternalServerError}
import spray.routing.{ExceptionHandler, HttpService}
import spray.util.LoggingContext
import scala.concurrent._
import ExecutionContext.Implicits.global

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

trait LoadedGtfsData { self: GtfsDatabase =>
  // In-memory GTFS data storage
  lazy val gtfsData: GtfsData = {
    val config = ConfigFactory.load
    val dbGeomNameUtm = config.getString("database.geom-name-utm")

    db withSession { implicit session =>
      val dao = new DAO()
      // data is read from the db as the reprojected UTM projection
      dao.geomColumnName = dbGeomNameUtm
      dao.toGtfsData
    }
  }
}

trait GeoTrellisServiceRoute extends HttpService with GeoTrellisService { self: GtfsDatabase =>
  // For performing extent queries
  case class Extent(xmin: Double, xmax: Double, ymin: Double, ymax: Double)
  implicit val getExtentResult = GetResult(r => Extent(r.<<, r.<<, r.<<, r.<<))

 // All the routes that are defined by our API
  def rootRoute = pingRoute ~ gtfsRoute ~ indicatorsRoute ~ mapInfoRoute

  // Endpoint for testing: browsing to /ping should return the text
  def pingRoute =
    pathPrefix("gt") {
      path("ping") {
        get {
          complete(future("pong!"))
        }
      }
    }

  // Endpoint for uploading a GTFS file
  def gtfsRoute =
    pathPrefix("gt") {
      path("gtfs") {
        post {
          parameter('gtfsDir.as[String]) { gtfsDir =>
            complete(future {
              println(s"parsing GTFS data from: $gtfsDir")
              val data = parseAndStore(gtfsDir)

              JsObject(
                "success" -> JsBoolean(true),
                "message" -> JsString(s"Imported ${data.routes.size} routes")
              )
            })
          }
        }
      }
    }

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
          entity(as[CalcParams]) { calcParams =>
            complete {
              try {
                calculateIndicators(calcParams)

                // return a 201 created
                Created -> JsObject(
                  "success" -> JsBoolean(true),
                  "message" -> JsString(s"Calculations started (version ${calcParams.version})")
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

  // Endpoint for obtaining map info (just extent for now)
  def mapInfoRoute =
    pathPrefix("gt") {
      path("map-info") {
        get {
          complete(future {
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
          })
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
          complete(future(InternalServerError, s"""{ "success": false, "message": "${jsonMessage}" }""" ))
        }
    }
}

trait GeoTrellisService extends LoadedGtfsData { self: GtfsDatabase =>
  val config = ConfigFactory.load
  val dbGeomNameLatLng = config.getString("database.geom-name-lat-lng")

  // Triggers an indicator calculation with the specified calculation parameters.
  // Returns true if calculation has begun, false if there is a problem
  def calculateIndicators(calcParams: CalcParams): Boolean = {
    val calc = new IndicatorsCalculator(gtfsData, calcParams, db)
    calc.storeIndicators

    // Update indicator-job status
    djangoClient.updateIndicatorJob(calcParams.token, IndicatorJob(version=calcParams.version, job_status="complete"))
    true
  }

  // Parses a GTFS directory from the specified location, stores it in the db and reprojects
  def parseAndStore(gtfsDir: String): GtfsData = {
    db withSession { implicit session: Session =>
      val data = GtfsData.fromFile(gtfsDir)

      val dao = new DAO
      // data is read from the file and inserted into the db as lat/lng
      dao.geomColumnName = dbGeomNameLatLng

      // insert GTFS data into the database
      data.routes.foreach { route => dao.routes.insert(route) }
      data.service.foreach { service => dao.service.insert(service) }
      data.agencies.foreach { agency => dao.agencies.insert(agency) }
      data.trips.foreach { trip => dao.trips.insert(trip) }
      data.shapes.foreach { shape => dao.shapes.insert(shape) }
      data.stops.foreach { stop => dao.stops.insert(stop) }
      println("finished parsing GTFS data")

      println("Transforming GTFS to local UTM zone.")
      // Get the SRID of the UTM zone which contains the center of the GTFS data.  This is
      // done by finding the centroid of the bounding box of all GTFS stops, and then
      // doing a spatial query on utm_zone_boundaries to figure out which UTM zone
      // contains the centroid point. If a GTFS feed spans the boundary of two (or more)
      // UTM zones, it will be arbitrarily assigned to the zone into which the centroid
      // point falls. This isn't expected to significantly increase error.
      // Note: This assumes that the gtfs_stops table is in the same projection as the
      // utm_zone_boundaries table (currently 4326). If that ever changes then this query
      // will need to be updated.
      val sridQ = Q.queryNA[Int]("""SELECT srid FROM utm_zone_boundaries
        WHERE ST_Contains(utm_zone_boundaries.geom,
            (SELECT ST_SetSRID(ST_Centroid((SELECT ST_Extent(the_geom) from gtfs_stops)),
            Find_SRID('public', 'gtfs_stops', 'the_geom'))));
        """)
      val srid = sridQ.list.head
      // Reproject gtfs_stops.geom and gtfs_shape_geoms.geom to the UTM srid.
      // Directly interpolating into SQL query strings isn't best practice,
      // but since the value is pre-loaded into the database, it's safe and the simplest
      // thing to do in this case.
      def geomTransform(srid: Int, table: String, geomType: String, column: String) =
        // only alter the table if it exists
        if (!MTable.getTables(table).list.isEmpty) {
          (Q.u +
            s"ALTER TABLE ${table} ALTER COLUMN ${column} " +
            s"TYPE Geometry(${geomType},${srid}) " +
            s"USING ST_Transform(${column},${srid});").execute
        }

      def geomCopy(srid: Int, table: String, fromColumn: String, toColumn: String) =
        (Q.u +
          s"UPDATE ${table} SET ${toColumn} = ST_Transform(${fromColumn}, ${srid});").execute

      geomTransform(srid, "gtfs_stops", "Point", "geom")
      geomCopy(srid, "gtfs_stops", "the_geom", "geom")
      geomTransform(srid, "gtfs_shape_geoms", "LineString", "geom")
      geomCopy(srid, "gtfs_shape_geoms", "the_geom", "geom")

      // Now that the SRID of the GTFS data is known, we also need to set the SRID
      // for imported shapefile data (boundaries and demographics).
      geomTransform(srid, "utm_datasources_boundary", "MultiPolygon", "geom")
      geomTransform(srid, "utm_datasources_demographicdatafeature", "MultiPolygon", "geom")

      println("Finished transforming to local UTM zone.")
      data
    }
  }
}
