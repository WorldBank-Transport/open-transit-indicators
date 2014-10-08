package com.azavea.gtfs.io.csv

import com.azavea.gtfs.Agency

import scala.collection.mutable

object AgencyFile extends GtfsFile[Agency] {
  val fileName = "agency.txt"
  val isRequired = true

  def parse(path: String): Seq[Agency] = {
    val agencies = mutable.ListBuffer[Agency]()
    for (s <- CsvParser.fromPath(path)) {
      agencies +=
      Agency(
        id = s("agency_id").getOrElse[String]("1"),
        name = s("agency_name").get,
        url = s("agency_url").get,
        timezone = s("agency_timezone").get,
        language = s("agency_lang"),
        phone = s("agency_phone"),
        fareUrl = s("agency_fare_url")
      )
    }

    if(agencies.map(_.id).distinct.size != agencies.size) {
      throw new GtfsFormatException(
        s"$path contains more than one agency with a the same ID."
      )
    }

    agencies.toSeq
  }
}
