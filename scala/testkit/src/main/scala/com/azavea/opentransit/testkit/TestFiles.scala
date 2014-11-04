package com.azavea.opentransit.testkit

object TestFiles {
  // Base is different depending on whether or not the process is forked in SBT
  lazy val base =
    if(new java.io.File("testkit").exists) "testkit" else "../testkit"

  def ashevillePath = s"$base/data/asheville_data"
  def singleFilesPath = s"$base/data/single-files"
  def rtSeptaPath = s"$base/data/septa_realtime_data"
  def septaPath: String = s"$base/data/septa_data"
}
