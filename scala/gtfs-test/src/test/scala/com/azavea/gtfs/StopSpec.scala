package com.azavea.gtfs

import org.scalatest._
import geotrellis.slick._
import geotrellis.vector._

class StopSpec extends FlatSpec with Matchers {
  val s1 = Stop("S1", "Stop 1", None, Point(0,0).withSRID(0))
  val s2 = Stop("S2", "Stop 2", None, Point(10,10).withSRID(0))
  val s3 = Stop("S3", "Stop 3", None, Point(10,20).withSRID(0))

  "Stop" should "know distance to another stop" in {
    (s2 - s1) should equal (math.sqrt(10*10 + 10*10))
    (s3 - s2) should equal (10.0)
  }

  it should "be commutative in distance calculation" in {
    (s2 - s1) should equal (s1 - s2)
    (s3 - s2) should equal (s2 - s3)
    (s3 - s1) should equal (s1 - s3)
  }
}
