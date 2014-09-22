package com.azavea.gtfs

import com.github.nscala_time.time.Imports._

import org.scalatest._

class TransitSystemFactorySpec extends FunSpec with Matchers {
  describe("TransitSystemFactory") {
    ignore("should leave out stops in a trip if they are before or after the start and end date") {
      false should be (true)
    }
  }
}
