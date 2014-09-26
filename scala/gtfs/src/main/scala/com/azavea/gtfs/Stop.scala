package com.azavea.gtfs

import math._
import geotrellis.slick._
import geotrellis.vector._

case class Stop (
  id: String,
  name: String,
  description: Option[String],
  point: Projected[Point]
) {

  /** Euclidean distance between stops
      TODO: This should give meters. */
  def -(that: Stop): Double =
    point.distance(that.point)
}

