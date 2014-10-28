package com.azavea.opentransit.util

import com.azavea.opentransit.DjangoClient
import spray.http.HttpRequest

import scala.concurrent._

trait MockDjangoClient extends DjangoClient {
  // not private in case you're a lot more inventive than me
  var requestPromise = promise[HttpRequest]

  def nextRequest: Future[HttpRequest] = requestPromise.future

  def processResponse(request: HttpRequest): Unit = {
    requestPromise.success(request)
    requestPromise = promise[HttpRequest] // swap in new promise for next request
  }
}
