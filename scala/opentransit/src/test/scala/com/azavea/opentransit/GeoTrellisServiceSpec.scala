package com.azavea.opentransit

import com.azavea.gtfs._
import com.azavea.gtfs.data._
import com.azavea.gtfs.slick._
import com.typesafe.config.{ConfigFactory,Config}

import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.{Database, Session}

import com.github.nscala_time.time.Imports._
import scala.slick.driver.PostgresDriver
import com.github.tototoshi.slick.PostgresJodaSupport
import geotrellis.slick._

import org.scalatest._

class GeoTrellisServiceSpec extends FlatSpec with PostgresSpec with Matchers {
  val dao = new DAO

  it should "start with no trips" in {
    db withSession { implicit session: Session =>
      dao.toGtfsData.trips.size should be (0)
    }
  }

  it should "be able to insert a trip" in {
    val trip1 = Trip("T3","SR1","1",None,
      List(
        StopTime("1","T3", 1, 0.seconds, 1.minute),
        StopTime("10","T3", 2, 10.minutes, 11.minutes),
        StopTime("100","T3", 3, 15.minutes, 16.minutes)
      )
    )
    db withSession {
      implicit session: Session =>
      dao.trips.insert(trip1)
    }
  }

  it should "be able to read a trip" in {
    db withSession { implicit session: Session =>
      dao.toGtfsData.trips.size should be (1)
    }
  }

  it should "Calculate UTM Zones SRIDs properly" in {
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
