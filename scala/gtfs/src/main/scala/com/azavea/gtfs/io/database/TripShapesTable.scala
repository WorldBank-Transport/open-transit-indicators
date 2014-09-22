package com.azavea.gtfs.io.database

import com.azavea.gtfs._
import geotrellis.vector._
import geotrellis.slick.Projected

trait TripShapesTable { this: Profile =>
  import profile.simple._
  import gis._

  class TripShapes(tag: Tag) extends Table[TripShape](tag, "gtfs_shape_geoms") {
    def id = column[String]("shape_id", O.PrimaryKey)
    def geom = column[Projected[Line]](geomColumnName)

    def * = (id, geom)  <> (TripShape.tupled, TripShape.unapply)
  }
  def tripShapesTable = TableQuery[TripShapes]
}
