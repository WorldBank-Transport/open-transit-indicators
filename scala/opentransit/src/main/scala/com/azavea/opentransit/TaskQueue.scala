package com.azavea.opentransit

import scala.concurrent._

object TaskQueue {
  def execute[T](f : => T): Future[T] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    future {
      try {
        f
      } catch {
        case e: Exception =>
          println(e)
          e.printStackTrace
          throw(e)
      }
    }
    System.gc()
  }
}
