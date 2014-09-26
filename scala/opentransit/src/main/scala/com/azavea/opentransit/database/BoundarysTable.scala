package com.azavea.opentransit.database

import geotrellis.vector._
import geotrellis.slick._

import scala.slick.driver.{JdbcDriver, JdbcProfile, PostgresDriver}

/**
 * A boundary, of either a city or a region.
 */
case class Boundary(
  id: Int,
  geom: Projected[MultiPolygon] // Location-dependent SRID (UTM zone)
)

/**
 * A trait providing Boundaries to an IndicatorCalculator
 */
object BoundariesTable { 
  import PostgresDriver.simple._
  private val gisSupport = new PostGisProjectionSupport(PostgresDriver)
  import gisSupport._

  /**
   * Table class supporting Slick persistence
   */
  class Boundaries(tag: Tag) extends Table[Boundary](tag, "utm_datasources_boundary") {
    def id = column[Int]("boundary_id")
    def geom = column[Projected[MultiPolygon]]("geom")

    def * = (id, geom) <> (Boundary.tupled, Boundary.unapply)
  }

  def boundariesTable = TableQuery[Boundaries]

  /**
   * Returns a Boundary (geometry denoting a city or region boundary)
   */
  def boundary(boundaryId: Int)(implicit session: Session): Projected[MultiPolygon] = 
    boundariesTable.filter(_.id === boundaryId).first.geom
}
