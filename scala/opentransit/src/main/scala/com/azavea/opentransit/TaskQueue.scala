package com.azavea.opentransit

import scala.concurrent._

object TaskQueue {
  def execute[T](f : => T): Future[T] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    future { f }
  }
}
