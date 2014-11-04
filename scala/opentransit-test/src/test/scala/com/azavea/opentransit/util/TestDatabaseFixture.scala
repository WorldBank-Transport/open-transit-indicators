package com.azavea.opentransit.util

import grizzled.slf4j.Logging
import com.azavea.opentransit.DatabaseInstance
import com.azavea.opentransit.io.GtfsIngest
import com.azavea.opentransit.testkit.TestGtfsRecords
import com.typesafe.config.ConfigFactory
import org.scalatest._

import scala.slick.jdbc.JdbcBackend.{Database, Session}
import scala.slick.jdbc.{StaticQuery => Q}
import scala.sys.process._

/**
 * This fixture will setup a default database with name of the test class and pre-populate it.
 * Any database asked by name will be created with the test class name as prefix.
 * All databases created during the run-time of this test will be dropped at the end.
 */
trait TestDatabaseFixture extends DatabaseInstance with BeforeAndAfterAll with Logging { self: Suite =>
  var live: Set[String] = Set.empty // set of databases that we have created for this test

  def mainDbName: String = getClass.getSimpleName.toLowerCase

  lazy val (dbSudo, dbUser, dbPassword) = {
    val config = ConfigFactory.load

    val dbSudo = config.getString("opentransit.testkit.sudo")
    val dbUser = config.getString("opentransit.testkit.dbuser")
    val dbPassword = config.getString("opentransit.testkit.dbpassword")

    (dbSudo, dbUser, dbPassword)
  }

  val db = dbByName(mainDbName)


  def dbByName(name: String): Database = {
    val exempt = List("postgres", mainDbName)
    val dbName = if (exempt contains name) name else s"$mainDbName-$name"

    Database.forURL(s"jdbc:postgresql:$dbName", driver = "org.postgresql.Driver",
      user = dbUser, password = dbPassword)
  }


  def createDatabase(name: String) = {
    val dbName = if (name == mainDbName) name else s"$mainDbName-$name"

    val processLogger = ProcessLogger(println, println);
    logger.info(s"TEST STRING")
    s"""sudo -u $dbSudo ../../deployment/setup_db.sh $dbName $dbUser "$dbPassword" ../..""".!!(processLogger)
    s"""sudo -u $dbSudo psql -d $dbName -f ../../deployment/stops_routes_function.sql""".!!(processLogger)
    live += dbName
  }

  def createFunctions(name: String) = {
    val dbName = if (name == mainDbName) name else s"$mainDbName-$name"

    val logger = ProcessLogger(println, println);
  }

  def deleteDatabase(name: String) = {
    dbByName("postgres") withSession { implicit session: Session =>
      Q.updateNA( s"""DROP DATABASE IF EXISTS "$name";""").execute
    }
  }

  abstract override protected def beforeAll(): Unit = {
    createDatabase(mainDbName)
    //createFunctions(mainDbName)
    live += mainDbName
    dbByName(mainDbName) withSession { implicit session: Session =>
      GtfsIngest(TestGtfsRecords())
    }
  }

  abstract override protected def afterAll(): Unit = {
    super.afterAll()
    live.foreach(deleteDatabase)
  }
}
