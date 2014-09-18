package com.azavea.opentransit

import com.azavea.opentransit.service.OpenTransitServiceActor

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.typesafe.config.{ConfigFactory,Config}
import spray.can.Http

object Main {
  def main(args: Array[String]) {
    // We need an ActorSystem to host our service
    implicit val system = ActorSystem()

    // Create our service actor
    val service = system.actorOf(Props[OpenTransitServiceActor], "opentransit-service")

    // Bind our actor to HTTP
    IO(Http) ! Http.Bind(service, interface = "0.0.0.0", port = ConfigFactory.load.getInt("opentransit.spray.port"))
  }
}
