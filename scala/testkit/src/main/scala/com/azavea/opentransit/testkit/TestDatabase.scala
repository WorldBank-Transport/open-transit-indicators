package com.azavea.opentransit.testkit

import com.typesafe.config.{ConfigFactory,Config}
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.{Database, Session}
import scala.sys.process._

trait TestDatabase {
  // Override this to manually create the name
  def dbName: String = {
    // set the dbname to the name of the testing spec, so we can run multiple test
    // specs in parallel without any interference
    getClass.getSimpleName.toLowerCase
  }

  lazy val (dbUser, dbPassword) = {
    val config = ConfigFactory.load
    val dbUser = config.getString("opentransit.testkit.dbuser")
    val dbPassword = config.getString("opentransit.testkit.dbpassword")

    (dbUser, dbPassword)
  }

  lazy val db = 
    Database.forURL(s"jdbc:postgresql:$dbName", driver = "org.postgresql.Driver", user = dbUser, password = dbPassword)

  lazy val postgres =
    // connection to the postgres database -- used for dropping the test database
      Database.forURL("jdbc:postgresql:postgres", driver = "org.postgresql.Driver",
        user = dbUser, password = dbPassword)

  def create() = {
    postgres withSession { implicit session: Session =>
      // drop the test database if it exists -- we want a fresh one for each spec
      Q.updateNA(s"""DROP DATABASE IF EXISTS "$dbName";""").execute

      // initialize the test database via the setup_db script
      s"sudo -u postgres ../deployment/setup_db.sh $dbName $dbUser $dbPassword ..".!!
    }
  }
}
