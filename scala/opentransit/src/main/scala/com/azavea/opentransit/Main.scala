package com.azavea.opentransit

import com.azavea.opentransit.service.OpenTransitServiceActor
import com.azavea.opentransit.database._

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.typesafe.config.{ConfigFactory,Config}
import spray.can.Http

import scala.slick.jdbc.JdbcBackend._
import scala.slick.jdbc.{StaticQuery => Q}

object Main {

  val actorSystem = ActorSystem("opentransit")

  val rasterCache = RasterCache(actorSystem)

  // This is the simplest possible cache - it represents some data or none and has no namespacing
  var csvCache = CSVCache

  // This call sets all processing jobs to an failed state
  def failLeftoverJobs(): Unit = {
    val jobsTable = new IndicatorJobsTable {}
    val dbi = new ProductionDatabaseInstance {}
    dbi.db withSession { implicit session: Session =>
      jobsTable.failOOMError
    }
  }

  def main(args: Array[String]) {
    // Set any incomplete jobs to errored out
    failLeftoverJobs
    // We need an ActorSystem to host our service
    implicit val system = actorSystem

    // Create our service actor
    val service = system.actorOf(Props[OpenTransitServiceActor], "opentransit-service")

    // Bind our actor to HTTP
    IO(Http) ! Http.Bind(service, interface = "0.0.0.0", port = ConfigFactory.load.getInt("opentransit.spray.port"))
  }
}
