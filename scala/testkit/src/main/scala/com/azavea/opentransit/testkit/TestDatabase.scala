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

  lazy val (sudoUser, dbUser, dbPassword) = {
    val config = ConfigFactory.load
    val sudoUser = config.getString("opentransit.testkit.sudo")
    val dbUser = config.getString("opentransit.testkit.dbuser")
    val dbPassword = config.getString("opentransit.testkit.dbpassword")
    (sudoUser, dbUser, dbPassword)
  }

  def dbForName(dbName: String): Database = {
    Database.forURL(s"jdbc:postgresql:$dbName", driver = "org.postgresql.Driver",
      user = dbUser, password = dbPassword)
  }

  lazy val db = dbForName(dbName)

  lazy val postgres = dbForName("postgres")
}
