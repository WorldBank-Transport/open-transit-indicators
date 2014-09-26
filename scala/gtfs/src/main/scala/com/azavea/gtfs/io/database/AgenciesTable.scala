package com.azavea.gtfs.io.database

import com.azavea.gtfs._

trait AgenciesTable { this: Profile =>
  import profile.simple._

  class Agencies(tag: Tag) extends Table[Agency](tag, "gtfs_agency") {
    def id = column[String]("agency_id", O.PrimaryKey)
    def name = column[String]("agency_name")
    def url = column[String]("agency_url")
    def timezone = column[String]("agency_timezone")
    def lang = column[Option[String]]("agency_lang")
    def phone = column[Option[String]]("agency_phone")
    def fare_url = column[Option[String]]("agency_fare_url")

    def * = (id, name, url, timezone, lang, phone, fare_url)  <>
      (Agency.tupled, Agency.unapply)
  }
 
  val agenciesTable = TableQuery[Agencies]
}
