package com.azavea.opentransit

import scala.slick.jdbc.JdbcBackend.{ Database, DatabaseDef }
import com.typesafe.config.{ ConfigFactory, Config }

trait DatabaseInstance {
  val db: DatabaseDef
}

trait ProductionDatabaseInstance extends DatabaseInstance {
  val db = {
    val config = ConfigFactory.load
    val dbName = config.getString("database.name")
    val dbUser = config.getString("database.user")
    val dbPassword = config.getString("database.password")
    Database.forURL(s"jdbc:postgresql:$dbName", driver = "org.postgresql.Driver",
      user = dbUser, password = dbPassword)
  }
}
