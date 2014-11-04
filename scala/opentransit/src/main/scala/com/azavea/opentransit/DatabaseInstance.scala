package com.azavea.opentransit

import scala.slick.jdbc.JdbcBackend._
import com.typesafe.config.{ ConfigFactory, Config }
import scala.slick.jdbc.StaticQuery
import scala.sys.process._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}


trait DatabaseInstance {
  val db: DatabaseDef
  def dbByName(dbName: String): DatabaseDef

  def createDatabase(name: String)
  def createFunctions(name: String)
  def deleteDatabase(name: String)
}

trait ProductionDatabaseInstance extends DatabaseInstance {
  private val (dbSudo, dbUser, dbPassword) = {
    val config = ConfigFactory.load
    val dbSudo = config.getString("database.sudo")
    val dbUser = config.getString("database.user")
    val dbPassword = config.getString("database.password")

    (dbSudo, dbUser, dbPassword)
  }

  val db = dbByName(ConfigFactory.load.getString("database.name"))

  // Allows retrieving a database other than the configured default
  def dbByName(dbName: String): Database = {
    Database.forURL(s"jdbc:postgresql:$dbName", driver = "org.postgresql.Driver",
      user = dbUser, password = dbPassword)
  }

  def createDatabase(name: String) = {
    s"""sudo -u $dbSudo ../deployment/setup_db.sh $name $dbUser "$dbPassword" ..""".!!
  }

  def createFunctions(name: String) = {
    s"""sudo -u $dbSudo psql -d $name -f ../deployment/stops_routes_function.sql""".!!
  }

  def deleteDatabase(name: String) = {
    dbByName("postgres") withSession { implicit session: Session =>
      Q.updateNA( s"""DROP DATABASE IF EXISTS "$name";""").execute
    }
  }

}
