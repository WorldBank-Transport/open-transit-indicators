package com.azavea.opentransit

import com.azavea.opentransit.io.GtfsIngest
import com.azavea.opentransit.indicators._
import com.azavea.opentransit.scenarios._
import com.azavea.opentransit.service._

import com.azavea.gtfs._
import com.azavea.opentransit.testkit._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ ConfigFactory, Config }

import java.util.UUID.randomUUID

import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.{ Database, DatabaseDef, Session }
import scala.sys.process._

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

  test("Create new scenarios") {
    val config = ConfigFactory.load
    val dbUser = config.getString("database.user")
    val dbPassword = config.getString("database.password")
    val baseDbName = randomUUID.toString
    val scenarioDbName = randomUUID.toString

    // Helper for obtaining a database connection
    def dbByName(dbName: String): Database = {
      Database.forURL(s"jdbc:postgresql:$dbName", driver = "org.postgresql.Driver",
        user = dbUser, password = dbPassword)
    }

    // Create the base db
    dbByName("postgres") withSession { implicit session: Session =>
      s"sudo -u postgres ../deployment/setup_db.sh $baseDbName $dbUser $dbPassword ..".!!
      s"sudo -u postgres testkit/data/populate_db.sh $baseDbName $dbUser $dbPassword ..".!!
    }

    // Load GTFS data into the base db
    dbByName(baseDbName) withSession { implicit session: Session =>
      GtfsIngest(GtfsRecords.fromFiles(TestFiles.septaPath))
    }

    // Assemble the scenario request
    var request = ScenarioCreationRequest("token", scenarioDbName, baseDbName,
      SamplePeriod(1, "night",
        new LocalDateTime("2014-05-01T00:00:00.000"),
        new LocalDateTime("2014-05-01T08:00:00.000")
      )
    )

    // Create the scenario
    CreateScenario(request, dbByName) { status =>
      status should be (JobStatus.Complete)
    }

    // Load the scenario and verify data is present
    dbByName(scenarioDbName) withSession { implicit session: Session =>
      val records = GtfsRecords.fromDatabase.force
      val builder = TransitSystemBuilder(records)
      val start = new LocalDateTime("2014-06-01T00:00:00.000")
      val end = new LocalDateTime("2014-06-01T08:00:00.000")
      builder.systemBetween(start, end).routes.find(_.id == "GLN") should be (None)
      builder.systemBetween(start, end).routes.find(_.id == "AIR") should not be (None)
    }

    // Clean up dbs
    dbByName("postgres") withSession { implicit session: Session =>
      try {
        Q.updateNA(s"""DROP DATABASE IF EXISTS "$baseDbName";""").execute
        Q.updateNA(s"""DROP DATABASE IF EXISTS "$scenarioDbName";""").execute
      } catch {
        case _: Throwable =>
          // TODO: figure out why sometimes these temp dbs have trouble being deleted
      }
    }
  }
}
