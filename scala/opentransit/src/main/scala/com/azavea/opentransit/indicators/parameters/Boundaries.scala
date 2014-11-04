package com.azavea.opentransit.indicators.parameters

import com.azavea.opentransit.database.BoundariesTable

import geotrellis.raster._
import geotrellis.slick._
import geotrellis.vector._

import scala.slick.jdbc.JdbcBackend.{Database, DatabaseDef, Session}

/**
 * Trait used to populate indicator parameters with boundaried
 */
trait Boundaries {
  def cityBoundary: MultiPolygon
  def regionBoundary: MultiPolygon
}

object Boundaries {
  def cityBoundary(id: Int)(implicit session: Session): MultiPolygon =
    BoundariesTable.boundary(id)

  def regionBoundary(id: Int)(implicit session: Session): MultiPolygon =
    BoundariesTable.boundary(id)
}
