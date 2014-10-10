package com.azavea.opentransit.database

import geotrellis.vector._
import geotrellis.slick._

import scala.slick.driver.{JdbcDriver, JdbcProfile, PostgresDriver}
import scala.slick.jdbc.{StaticQuery => Q}



/**
 * A demographic point, has multiple demographic categories
 */
case class Demographic(
  id: Int,
  geom: Projected[Point], // Location-dependent SRID (UTM zone)
   populationMetric1: Double,
   populationMetric2: Double,
   destinationMetric1: Double
)

object DemographicsTable {
  import PostgresDriver.simple._
  private val gisSupport = new PostGisProjectionSupport(PostgresDriver)
  import gisSupport._

  /**
   * Table class supporting Slick persistence
   */
  class Demographics(tag: Tag) extends Table[Demographic](tag, "demographic_grid") {
    def id = column[Int]("feature_id")
    def geom = column[Projected[Point]]("geom")
    def populationMetric1 = column[Double]("population_metric_1")
    def populationMetric2 = column[Double]("population_metric_2")
    def destinationMetric1 = column[Double]("destination_metric_1")

    def * = (id, geom, populationMetric1, populationMetric2, destinationMetric1) <> (Demographic.tupled, Demographic.unapply)
  }

  def demographicsTable = TableQuery[Demographics]

  /**
    * Given a multipolygon and string for colum, returns population metric
    */
  def getPopMetric(multipolygon: Projected[MultiPolygon], column: String)(implicit session: Session): Double = {
    val metric = for {
      demographic <- demographicsTable if demographic.geom.within(multipolygon)
    } yield column match {
      case "populationMetric2" => demographic.populationMetric2
      case "destinationMetric1" => demographic.destinationMetric1
      case _ => demographic.populationMetric1
    }
    metric.sum.run.getOrElse(0.0)
  }
}
