package com.azavea.gtfs.io.database

import com.azavea.gtfs._
import geotrellis.vector._
import geotrellis.slick._

trait StopsTable { this: Profile =>
  import profile.simple._
  import gis._

  class Stops(tag: Tag) extends Table[Stop](tag, this.stopsTableName) {
    def id = column[String]("stop_id", O.PrimaryKey)
    def name = column[String]("stop_name")
    def desc = column[Option[String]]("stop_desc")
    def geom = column[Projected[Point]](geomColumnName)

    def * = (id, name, desc, geom) <> (Stop.tupled, Stop.unapply)
  }

  val stopsTable = TableQuery[Stops]

  def getStopBuffers(radius: Float)(implicit session: Session): Map[String, Projected[Polygon]] = {
    val result = for {
      stop <- stopsTable
    } yield (stop.id, stop.geom.buffer(radius))
    result.list.map( tup => tup.asInstanceOf[(String, Projected[Polygon])] ).toMap
  }

}
