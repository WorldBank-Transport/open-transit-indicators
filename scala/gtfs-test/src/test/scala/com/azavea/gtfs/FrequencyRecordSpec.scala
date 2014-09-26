package com.azavea.gtfs

import com.github.nscala_time.time.Imports._

import org.scalatest._

class FrequencyRecordSpec extends FunSpec with Matchers {
  describe("FrequencyRecord") {
    it("should be such that two equal frequencies return true for equals(...)") {
      val f1 = FrequencyRecord("trip", 10.hours, 10.hours + 13.minutes, 300.seconds)
      val f2 = FrequencyRecord("trip", 10.hours, 10.hours + 13.minutes, 300.seconds)

      f1 should be (f2)
    }
  }
}
