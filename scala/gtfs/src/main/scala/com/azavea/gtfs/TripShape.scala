package com.azavea.gtfs

import geotrellis.vector._
import geotrellis.slick.Projected

case class TripShape(id: String, line: Projected[Line])

