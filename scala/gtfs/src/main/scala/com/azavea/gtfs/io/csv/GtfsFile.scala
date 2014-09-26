package com.azavea.gtfs.io.csv

trait GtfsFile[T] {
  val fileName: String
  val isRequired: Boolean

  def parse(path: String): Seq[T]
}
