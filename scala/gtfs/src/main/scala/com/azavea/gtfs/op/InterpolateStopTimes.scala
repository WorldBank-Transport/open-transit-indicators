package com.azavea.gtfs.op

import com.azavea.gtfs._

import com.github.nscala_time.time.Imports._

/** Interpolates the arrival and depature times
  * for StopTimeRecords that have no entires.
  * This happen for some GTFS data sets that are off-spec,
  * as Spec (Revised June 20, 2012) says these fields are
  * required.
  */
object InterpolateStopTimes {
  def apply(records: GtfsRecords): GtfsRecords = {
    val interpolatedStopTimes = 
      records.stopTimeRecords
        .groupBy(_.tripId)
        .values
        .map { tripStopTimes =>
          val sorted =
            tripStopTimes.sortBy(_.sequence)

          Interpolator.interpolate(tripStopTimes.toArray)(new Interpolatable[StopTimeRecord] {
            override def x(t1: StopTimeRecord): Double =
              t1.distanceTraveled match {
                case Some (x) => x
                case None => Double.NaN
              }

            override def y(t1: StopTimeRecord): Double =
              t1.arrivalTime.getMillis.toDouble

            override def update(t: StopTimeRecord, x: Double): StopTimeRecord =
              t.copy(arrivalTime = x.toInt.seconds, departureTime = x.toInt.seconds)

            override def missing(t: StopTimeRecord): Boolean =
              t.arrivalTime == null //nulls are bad, must exterminate nulls
          })
         }
        .flatten
        .toSeq

    new GtfsRecords {
      def agencies = records.agencies
      def stops = records.stops
      def tripRecords = records.tripRecords
      def routeRecords = records.routeRecords
      def frequencyRecords = records.frequencyRecords
      def calendarRecords = records.calendarRecords
      def calendarDateRecords = records.calendarDateRecords
      def tripShapes = records.tripShapes

      def stopTimeRecords = interpolatedStopTimes
    }
  }
}

trait Interpolatable[T] {
  def x(t: T): Double
  def y(t: T): Double
  def missing(t: T): Boolean
  def update(t: T, x: Double): T
  def slope(t1: T, t2: T): Double =
    (y(t2) - y(t1)) / (x(t2) - x(t1))
}

object Interpolator {
  /** Sequence of items that is assumed to be increasing linearly */
  def interpolate[A](arr: Array[A])(implicit s: Interpolatable[A]): Array[A] = {
    def fillForward(i: Int): Int = {
      var j = i
      while (s.missing(arr(j))) j += 1   //find next filled
      val m = s.slope(arr(i-1), arr(j))  //assume a linear relation
      for (k <- i until j) {                //fill forward
        arr(k) = s.update(
          arr(k),
          s.y(arr(k-1)) + (s.x(arr(k)) - s.x(arr(k-1))) * m
        )
      }
      j
    }

    var i = 0
    while (i < arr.length) {
      if (s.missing(arr(i))) {
        i = fillForward(i)
      }else{
        i += 1
      }
    }

    arr
  }
}
