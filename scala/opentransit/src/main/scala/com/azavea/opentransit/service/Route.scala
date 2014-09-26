package com.azavea.opentransit.service

import spray.routing.HttpService
import scala.concurrent._

trait Route extends HttpService {
  // Required for marshalling futures
  implicit val dispatcher: ExecutionContext
}
