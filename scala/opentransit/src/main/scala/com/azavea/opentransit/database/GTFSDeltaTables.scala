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

case class TripDelta(deltaType: DeltaType, tripShape: TripShape)
case class StopDelta(deltaType: DeltaType, stop: Stop)

object TripDeltaStore {
  import PostgresDriver.simple._
  private val gisSupport = new PostGisProjectionSupport(PostgresDriver)
  import gisSupport._

  def serialize(tripDelta: TripDelta): Option[(Int, String, Projected[Line])] =
    Some((tripDelta.deltaType.intRep, tripDelta.tripShape.id, tripDelta.tripShape.line))

  def deserialize(tripDeltaTuple: (Int, String, Projected[Line])): TripDelta =
    TripDelta(
      DeltaType(tripDeltaTuple._1),
      TripShape(tripDeltaTuple._2, tripDeltaTuple._3)
    )

  class TripDeltaTable(tag: Tag) extends Table[TripDelta](tag, "trip_deltas") {
    def id = column[String]("id", O.PrimaryKey)
    def geom = column[Projected[Line]]("geom")
    def deltaType = column[Int]("delta_type")

    def * = (deltaType, id, geom) <> (deserialize, serialize)
  }
  val tripDeltas = TableQuery[TripDeltaTable]

  def addTripShape(tripShape: TripShape)(implicit sess: Session): Unit = {
    val query = tripDeltas.filter(_.id === tripShape.id).map(_.deltaType)
    query.firstOption match {
      case Some(_) => ()
      case None => tripDeltas.insert(TripDelta(GTFSAddition, tripShape))
    }
  }

  def removeTripShape(tripShape: TripShape)(implicit sess: Session): Unit = {
    val query = tripDeltas.filter(_.id === tripShape.id)
    query.firstOption match {
      case Some(_) => query.delete
      case None => tripDeltas.insert(TripDelta(GTFSRemoval, tripShape))
    }
  }

  def tripHighlights(deltaType: DeltaType)(implicit sess: Session): MultiLine = {
    val query =
      for { d <- tripDeltas if d.deltaType === deltaType.intRep } yield d.geom

    query.run.map(_.geom).foldLeft(MultiLine.EMPTY) {
      (union, geom) => union.union(geom) match {
        case LineResult(l) => MultiLine(l)
        case MultiLineResult(ml) => ml
      }
    }
  }
}

object StopDeltaStore {
  import PostgresDriver.simple._
  private val gisSupport = new PostGisProjectionSupport(PostgresDriver)
  import gisSupport._

  def serialize(stopDelta: StopDelta): Option[(Int, String, String, Option[String], Projected[Point])] =
    Some(Tuple5(
      stopDelta.deltaType.intRep,
      stopDelta.stop.id,
      stopDelta.stop.name,
      stopDelta.stop.description,
      stopDelta.stop.point
    ))

  def deserialize(stopDeltaTuple: (Int, String, String, Option[String], Projected[Point])): StopDelta =
    StopDelta(
      DeltaType(stopDeltaTuple._1),
      Stop(stopDeltaTuple._2, stopDeltaTuple._3, stopDeltaTuple._4, stopDeltaTuple._5)
    )

  class StopDeltaTable(tag: Tag) extends Table[StopDelta](tag, "stop_deltas") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def description = column[Option[String]]("description")
    def geom = column[Projected[Point]]("geom")
    def deltaType = column[Int]("delta_type")

    def * = (deltaType, id, name, description, geom) <> (deserialize, serialize)
  }
  val stopDeltas = TableQuery[StopDeltaTable]

  def addStop(stop: Stop)(implicit sess: Session): Unit = {
    val query = stopDeltas.filter(_.id === stop.id).map(_.deltaType)
    query.firstOption match {
      case Some(_) => ()
      case None => stopDeltas.insert(StopDelta(GTFSAddition, stop))
    }
  }

  def removeStop(stop: Stop)(implicit sess: Session): Unit = {
    val query = stopDeltas.filter(_.id === stop.id)
    query.firstOption match {
      case Some(_) => query.delete
      case None => stopDeltas.insert(StopDelta(GTFSRemoval, stop))
    }
  }

  def stopHighlights(deltaType: DeltaType)(implicit sess: Session): MultiPoint = {
    val query =
      for { d <- stopDeltas if d.deltaType === deltaType.intRep } yield d.geom

    query.run.map(_.geom).foldLeft(MultiPoint.EMPTY) {
      (union, geom) => union.union(geom) match {
        case PointResult(p) => MultiPoint(p)
        case MultiPointResult(mp) => mp
      }
    }
  }
}
