package com.azavea.gtfs

object Timer {
  // Time how long a block takes to execute.  From here:
  // http://stackoverflow.com/questions/9160001/how-to-profile-methods-in-scala
  def timedTask[R](msg: String)(block: => R): R = {
    val t0 = System.currentTimeMillis
    val result = block    // call-by-name
    val t1 = System.currentTimeMillis
    println(msg + " in " + ((t1 - t0) / 1000.0) + " s")
    result
  }
}
