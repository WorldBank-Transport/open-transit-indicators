package opentransitgt

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.azavea.gtfs.slick._
import com.typesafe.config.{ConfigFactory,Config}
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.{Database, Session}

import org.scalatest._

class GeoTrellisServiceSpec extends FlatSpec with Matchers {
  // Connect to database
  // #TODO: Set up a proper test database.
  val config = ConfigFactory.load
  val dbName = config.getString("database.name")
  val dbUser = config.getString("database.user")
  val dbPassword = config.getString("database.password")

  val db = Database.forURL(s"jdbc:postgresql:$dbName", driver = "org.postgresql.Driver",
    user = dbUser, password = dbPassword)

  it should "Calculate UTM Zones SRIDs properly" in {
    db withSession { implicit session: Session =>
      val sridQ = Q.query[(Double, Double), Int]("""SELECT srid FROM utm_zone_boundaries
                                          WHERE ST_Contains(utm_zone_boundaries.geom,
                                          (SELECT ST_SetSRID(ST_MakePoint(?,?),4326)));""")

      Seq (
        ((-75.1667, 39.9500), 32618), // Philly, zone 18N
        ((-157.8167, 21.3000), 32604), // Honolulu, zone 4N
        ((144.9631, -37.8136), 32755), // Melbourne, zone 55S
        ((116.3917, 39.9139), 32650), // Beijing, zone 50N
        ((18.4239, -33.9253), 32734), // Cape Town, zone 34S
        ((166.6667, -77.8500), 32758), // McMurdo Station, zone 58S
        ((-149.900, 61.2167), 32606) // Anchorage, zone 6N
      ).foreach(coordTest => { sridQ.list(coordTest._1).head should be (coordTest._2) })
    }
  }
}
