initialCommands in opentransit in console := """
import spray.json._
import DefaultJsonProtocol._
import org.apache.commons.csv._
import com.azavea.opentransit._
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.indicators.stations._
import com.azavea.opentransit.database._
import com.azavea.opentransit.indicators.parameters._
import geotrellis.slick._
import geotrellis.vector._
import scala.slick.driver._
import PostgresDriver.simple._
val gisSupport = new PostGisProjectionSupport(PostgresDriver)
import gisSupport._
implicit val db = new ProductionDatabaseInstance {}.db
implicit val sess = db.createSession()
"""
