// package com.azavea.opentransit.database

// import geotrellis.vector._
// import geotrellis.slick._

// import scala.slick.driver.{JdbcDriver, JdbcProfile, PostgresDriver}

// /**
//  * A demographic point, has multiple demographic categories
//  */
// case class Demographic(
//   id: Int,
//   geom: Projected[Point], // Location-dependent SRID (UTM zone)
//   populationMetric1: Double,
//   populationMetric2: Double,
//   destinationMetric1: Double
// )

// /**
//  * A trait providing Boundaries to an IndicatorCalculator
//  */
// object DemographicsTable {
//   import PostgresDriver.simple._
//   private val gisSupport = new PostGisProjectionSupport(PostgresDriver)
//   import gisSupport._

//   /**
//    * Table class supporting Slick persistence
//    */
//   class Demographics(tag: Tag) extends Table[Demographic](tag, "demographic_grid") {
//     def id = column[Int]("feature_id")
//     def geom = column[Projected[Point]]("geom")
//     def populationMetric1 = column[Double]("population_metric_1")
//     def populationMetric2 = column[Double]("population_metric_2")
//     def destinationMetric1 = column[Double]("destination_metric_1")

//     def * = (id, geom, populationMetric1, populationMetric2, destinationMetric1) <> (Demographic.tupled, Demographic.unapply)
//   }

//   def demographicsTable = TableQuery[Demographics]

//   def demographic(demographicColumn: String, stopsBuffer: Projected[MultiPolygon])(implicit session: Session): Double = {
//     val demographicQueryString =
//       s"""SELECT
//             SUM($demographicColumn)
//           FROM
//             demographic_grid
//           WHERE ST_Contains(
//             ST_GeometryFromText(
//               ${stopsBuffer.geom.toString},
//               ${stopsBuffer.srid}),
//             geom)"""
//     val demographicQuery = Q.queryNA[Int](demographicQueryString)
//     demographicQuery.firstOption
//   }

//   /**
//    * Returns a Boundary (geometry denoting a city or region boundary)
//   def boundary(boundaryId: Int)(implicit session: Session): Projected[MultiPolygon] = {
//     val bounds = boundariesTable.filter(_.id === boundaryId).firstOption
//     bounds.map(_.geom) match {
//       case Some(geometry: Projected[MultiPolygon]) => geometry
//       case None => Projected(MultiPolygon.EMPTY, 4326)
//     }
//   }   */
// }
