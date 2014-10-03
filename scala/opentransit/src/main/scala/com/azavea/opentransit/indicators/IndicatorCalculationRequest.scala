package com.azavea.opentransit.indicators

import geotrellis.vector._

import com.azavea.opentransit.database.{ BoundariesTable, RoadsTable }
import scala.slick.jdbc.JdbcBackend.Session

import grizzled.slf4j.Logging

// Calculation request parameters
case class IndicatorCalculationRequest(
  token: String,
  version: String,
  povertyLine: Double,
  nearbyBufferDistance: Double,
  maxCommuteTime: Int,
  maxWalkTime: Int,
  cityBoundaryId: Int,
  regionBoundaryId: Int,
  averageFare: Double,
  samplePeriods: List[SamplePeriod]
) extends Logging {
  // There's probably a better place for these database fetches. Especially if
  // the info is used between various requests and some indicators that could
  // start calculation will have to wait on this to happen. But we can do them here
  // and that keeps the database from having to be injected into the Indicators,
  // which is a big win for modularity. Global state objects like this are icky.
  // There's surely a better way to get the information that any one Indicator needs
  // to it without having to pass everything to everyone.
  def toParams(implicit session: Session) = {
    // Get boundary
    val cityBoundary = BoundariesTable.boundary(cityBoundaryId)
    val regionBoundary = BoundariesTable.boundary(regionBoundaryId)
    val totalRoadLength = {
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

    IndicatorCalculationParams(
      povertyLine,
      nearbyBufferDistance,
      maxCommuteTime,
      maxWalkTime,
      cityBoundary,
      regionBoundary,
      averageFare,
      totalRoadLength
    )
  }
}
