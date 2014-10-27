package com.azavea.opentransit.testkit

import com.typesafe.config.{ConfigFactory,Config}
import org.scalatest._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.{Database, Session}
import scala.sys.process._

// trait that sets up a fresh database for each spec, and drops it afterwards
trait DatabaseTestFixture extends TestDatabase with BeforeAndAfterAll { self: Suite =>
  // database initialization
  postgres withSession { implicit session: Session =>
    // drop the test database if it exists -- we want a fresh one for each spec
    Q.updateNA(s"DROP DATABASE IF EXISTS $dbName").execute

    // Base is different depending on whether or not the process is forked in SBT
    val base = if(new java.io.File("testkit").exists) ".." else "../.."
    val tkBase = if(new java.io.File("testkit").exists) "testkit" else "../testkit"

    // initialize the test database via the setup_db script
    s"sudo -u postgres $base/deployment/setup_db.sh $dbName $dbUser $dbPassword $base".!!
    s"sudo -u postgres $tkBase/data/populate_db.sh $dbName $dbUser $dbPassword $base".!!
    s"sudo -u postgres psql -d $dbName -f $tkBase/data/philly_demographics/demographics.sql".!!
  }

  // after all tests have been run in the spec, drop the test database
  override def afterAll() {
    postgres withSession { implicit session: Session =>
      Q.updateNA(s"""DROP DATABASE IF EXISTS "$dbName";""").execute
    }
  }
}
