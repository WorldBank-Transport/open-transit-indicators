package com.azavea.opentransit.database

import geotrellis.vector._
import geotrellis.slick._

import grizzled.slf4j.Logging

import scala.slick.driver.{JdbcDriver, JdbcProfile, PostgresDriver}

/**
  * A multiline of all roads pulled from OSM.
  */
case class Road(
  osm_id: String,
  geom: Projected[Line] // Location-dependent SRID (UTM zone)
)

/**
 * A trait providing Boundaries to an IndicatorCalculator
 */
object RoadsTable extends Logging { 
  import PostgresDriver.simple._
  private val gisSupport = new PostGisProjectionSupport(PostgresDriver)
  import gisSupport._

  /**
  * Table class supporting Slick persistence
  */
  class Roads(tag: Tag) extends Table[Road](tag, "planet_osm_roads") {
    def osm_id = column[String]("osm_id")
    def geom = column[Projected[Line]]("way")

    def * = (osm_id,geom) <> (Road.tupled, Road.unapply)
  }

  def roadTable = TableQuery[Roads]
  /**
  * Returns roadlines (geometry denoting a set of roads)
  */
  def allRoads(implicit session: Session): List[Line] = {
    debug("Inside OSMCalculatorComponent, gathering all roads")
    val roads = roadTable.list
    roads.map(_.geom.geom)
  }
}
