package com.azavea.gtfs.op

import com.github.nscala_time.time.Imports._
import org.scalatest._


class InterpolatorSpec extends FlatSpec with Matchers {
  implicit val OverkillInterp = new Interpolatable[(Int, Int)] {
    override def update(t: (Int, Int), x: Double): (Int, Int) = t._1 -> x.toInt

    override def missing(t: (Int, Int)): Boolean = t._2 == 0

    override def x(t1: (Int, Int)): Double = t1._1.toDouble

    override def y(t1: (Int, Int)): Double = t1._2.toDouble
  }

  "Interpolator" should "interpolate!" in {
    val x = Array(1,2,3,4,5,6)
    val y = Array(1,2,3,0,5,6)


    val a = Interpolator.interpolate(x.zip(y))

    a should be (x.zip(Array(1,2,3,4,5,6)))
  }

  it should "interpolate twice" in {
    val x = Array(1,2,3,4,5,6)
    val y = Array(1,0,7,0,0,10)

    val a = Interpolator.interpolate(x.zip(y))

    a should be (x.zip(Array(1,4,7,8,9,10)))
  }
}
