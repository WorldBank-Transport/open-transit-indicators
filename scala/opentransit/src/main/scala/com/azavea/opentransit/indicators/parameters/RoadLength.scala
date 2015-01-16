package com.azavea.opentransit.indicators.parameters


import com.azavea.opentransit.database.RoadsTable

import geotrellis.vector._

import grizzled.slf4j.Logging

import scala.slick.jdbc.JdbcBackend.{Database, DatabaseDef, Session}

/**
 * Trait used to populate indicator parameters with Road Length
 */
trait RoadLength {
  def totalRoadLength: Double
}

object RoadLength extends Logging {
  def totalRoadLength(implicit session: Session): Double = {
    debug("Fetching Roads")
    val roadLines: List[Line] = RoadsTable.allRoads
    val distinctRoadLines: Array[Line] =
      (MultiLine(roadLines: _*).union match {
        case MultiLineResult(ml) => ml
        case LineResult(l) => MultiLine(l)
        case NoResult => MultiLine.EMPTY
      }).lines
    val len = distinctRoadLines.map(x => x.length).sum / 1000
    debug(s"Length of roadlines: $len")
    len
  }
}
