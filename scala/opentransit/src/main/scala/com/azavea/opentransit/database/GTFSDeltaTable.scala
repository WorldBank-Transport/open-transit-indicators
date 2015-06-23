<<<<<<< Updated upstream
package com.azavea.opentransit.database

import com.azavea.opentransit._
import com.azavea.gtfs._

import geotrellis.slick._
import geotrellis.vector._

import scala.slick.driver.{JdbcDriver, JdbcProfile, PostgresDriver}
import scala.slick.jdbc.JdbcBackend.DatabaseDef


// The performance characteristics of postgres enums should be the same as integers.
// Instead of introducing a dependency for PG enums (something we should do from the start),
// I'll use scala typesafety and the `DeltaType` sum type.
sealed trait DeltaType { val intRep: Int }
case object GTFSAddition extends DeltaType { val intRep = 1 }
case object GTFSRemoval extends DeltaType { val intRep = -1 }

object DeltaType {
  def apply(intRep: Int) = intRep match {
    case GTFSAddition.intRep => GTFSAddition
    case GTFSRemoval.intRep => GTFSRemoval
    case _ => throw new Exception("GTFSDelta must be 1 or -1")
  }
}

case class GTFSDelta(deltaType: DeltaType, tripShape: TripShape)

/**
 *
**/
object GTFSDeltaStore {
  import PostgresDriver.simple._
  private val gisSupport = new PostGisProjectionSupport(PostgresDriver)
  import gisSupport._

  private val dbi = new ProductionDatabaseInstance {}

  def serialize(gtfsDelta: GTFSDelta): Option[(String, Int, Projected[Line])] =
    Some((gtfsDelta.tripShape.id, gtfsDelta.deltaType.intRep, gtfsDelta.tripShape.line))
  def deserialize(gtfsDeltaTuple: (String, Int, Projected[Line])): GTFSDelta =
    GTFSDelta(
      DeltaType(gtfsDeltaTuple._2),
      TripShape(gtfsDeltaTuple._1, gtfsDeltaTuple._3)
    )

  class GTFSDeltaTable(tag: Tag) extends Table[GTFSDelta](tag, "gtfs_delta") {
    def id = column[String]("id", O.PrimaryKey)
    def deltaType = column[Int]("deltaType")
    def geom = column[Projected[Line]]("geom")

    def * = (id, deltaType, geom) <> (deserialize, serialize)
  }
  val gtfsDeltas = TableQuery[GTFSDeltaTable]

  def addTripShape(tripShape: TripShape)(implicit sess: Session): Unit = gtfsDeltas.insert(GTFSDelta(GTFSAddition, tripShape))

  def removeTripShape(tripShape: TripShape)(implicit sess: Session): Unit = gtfsDeltas.insert(GTFSDelta(GTFSRemoval, tripShape))

  def gtfsHighlights(deltaType: DeltaType)(implicit sess: Session): MultiLine = {
    val query =
      for { d <- gtfsDeltas if d.deltaType === deltaType.intRep } yield d.geom

    query.run.map(_.geom).foldLeft(MultiLine.EMPTY) {
      (union, geom) => union.union(geom) match {
        case LineResult(l) => MultiLine(l)
        case MultiLineResult(ml) => ml
      }
    }
  }
}
||||||| merged common ancestors
=======
package com.azavea.opentransit.database

import com.azavea.opentransit._
import com.azavea.gtfs._

import geotrellis.slick._
import geotrellis.vector._

import scala.slick.driver.{JdbcDriver, JdbcProfile, PostgresDriver}
import scala.slick.jdbc.JdbcBackend.DatabaseDef


// The performance characteristics of postgres enums should be the same as integers.
// Instead of introducing a dependency for PG enums (something we should do from the start),
// I'll use typesafety and the `DeltaType` sum type to achieve parity.
sealed trait DeltaType { val intRep: Int }
case object GTFSAddition extends DeltaType { val intRep = 1 }
case object GTFSRemoval extends DeltaType { val intRep = -1 }

object DeltaType {
  def apply(intRep: Int) = intRep match {
    case GTFSAddition.intRep => GTFSAddition
    case GTFSRemoval.intRep => GTFSRemoval
    case _ => throw new Exception("GTFSDelta must be 1 or -1")
  }
}

case class GTFSDelta(deltaType: DeltaType, tripShape: TripShape)

/**
 *
**/
object GTFSDeltaStore {
  import PostgresDriver.simple._
  private val gisSupport = new PostGisProjectionSupport(PostgresDriver)
  import gisSupport._

  def serialize(gtfsDelta: GTFSDelta): Option[(String, Int, Projected[Line])] =
    Some((gtfsDelta.tripShape.id, gtfsDelta.deltaType.intRep, gtfsDelta.tripShape.line))

  def deserialize(gtfsDeltaTuple: (String, Int, Projected[Line])): GTFSDelta =
    GTFSDelta(
      DeltaType(gtfsDeltaTuple._2),
      TripShape(gtfsDeltaTuple._1, gtfsDeltaTuple._3)
    )

  class GTFSDeltaTable(tag: Tag) extends Table[GTFSDelta](tag, "gtfs_delta") {
    def id = column[String]("id", O.PrimaryKey)
    def deltaType = column[Int]("deltaType")
    def geom = column[Projected[Line]]("geom")

    def * = (id, deltaType, geom) <> (deserialize, serialize)
  }
  val gtfsDeltas = TableQuery[GTFSDeltaTable]

  def addTripShape(tripShape: TripShape)(implicit sess: Session): Unit = gtfsDeltas.insert(GTFSDelta(GTFSAddition, tripShape))

  def removeTripShape(tripShape: TripShape)(implicit sess: Session): Unit = gtfsDeltas.insert(GTFSDelta(GTFSRemoval, tripShape))

  def gtfsHighlights(deltaType: DeltaType)(implicit sess: Session): MultiLine = {
    val query =
      for { d <- gtfsDeltas if d.deltaType === deltaType.intRep } yield d.geom

    query.run.map(_.geom).foldLeft(MultiLine.EMPTY) {
      (union, geom) => union.union(geom) match {
        case LineResult(l) => MultiLine(l)
        case MultiLineResult(ml) => ml
      }
    }
  }
}
>>>>>>> Stashed changes
