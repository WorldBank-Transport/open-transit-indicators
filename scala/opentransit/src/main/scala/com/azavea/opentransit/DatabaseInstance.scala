package com.azavea.opentransit

import scala.slick.jdbc.JdbcBackend.{ Database, DatabaseDef }
import com.typesafe.config.{ ConfigFactory, Config }

trait DatabaseInstance {
  val db: DatabaseDef
  def dbByName(dbName: String): DatabaseDef
}

trait ProductionDatabaseInstance extends DatabaseInstance {
  val db = dbByName(ConfigFactory.load.getString("database.name"))

  // Allows retrieving a database other than the configured default
  def dbByName(dbName: String): Database = {
    val config = ConfigFactory.load
    val dbUser = config.getString("database.user")
    val dbPassword = config.getString("database.password")
    Database.forURL(s"jdbc:postgresql:$dbName", driver = "org.postgresql.Driver",
      user = dbUser, password = dbPassword)
  }
}
