package com.azavea.opentransit

import com.typesafe.config.{ConfigFactory,Config}
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.{Database, Session}

trait TestDatabase extends GtfsDatabase {
  val (dbName, dbUser, dbPassword) = {
    val config = ConfigFactory.load
    val dbUser = config.getString("database.user")
    val dbPassword = config.getString("database.password")

    // set the dbname to the name of the testing spec, so we can run multiple test
    // specs in parallel without any interference
    val dbName = getClass.getSimpleName.toLowerCase
    println(s"             DBNAME: $dbName")

    (dbName, dbUser, dbPassword)
  }

  lazy val (db, postgres) = {
    val db = Database.forURL(s"jdbc:postgresql:$dbName", driver = "org.postgresql.Driver",
      user = dbUser, password = dbPassword)

    // connection to the postgres database -- used for dropping the test database
    val postgres = 
      Database.forURL("jdbc:postgresql:postgres", driver = "org.postgresql.Driver",
        user = dbUser, password = dbPassword)

    (db, postgres)
  }
}
