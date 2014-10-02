package com.azavea.opentransit

import com.azavea.gtfs._
import com.azavea.opentransit.testkit._

import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Session

import org.scalatest._

class DatabaseTests extends FunSuite with DatabaseTestFixture with Matchers {
  test("Calculate UTM Zones SRIDs properly") {
    db withSession { implicit session: Session =>
      val sridQ = Q.query[(Double, Double), Int]("""SELECT srid FROM utm_zone_boundaries
                                          WHERE ST_Contains(utm_zone_boundaries.geom,
                                          (SELECT ST_SetSRID(ST_MakePoint(?,?),4326)));""")

      val coordTests = 
        Seq (
          ((-75.1667, 39.9500), 32618), // Philly, zone 18N
          ((-157.8167, 21.3000), 32604), // Honolulu, zone 4N
          ((144.9631, -37.8136), 32755), // Melbourne, zone 55S
          ((116.3917, 39.9139), 32650), // Beijing, zone 50N
          ((18.4239, -33.9253), 32734), // Cape Town, zone 34S
          ((166.6667, -77.8500), 32758), // McMurdo Station, zone 58S
          ((-149.900, 61.2167), 32606) // Anchorage, zone 6N
        )

      for((coords, expected) <- coordTests) { 
        sridQ(coords).list.head should be (expected) 
      }
    }
  }
}
