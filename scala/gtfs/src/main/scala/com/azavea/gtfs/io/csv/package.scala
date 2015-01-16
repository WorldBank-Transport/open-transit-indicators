package com.azavea.gtfs.io

import com.github.nscala_time.time.Imports._
import org.joda.time.format.PeriodFormatterBuilder

package object csv {
  val periodFormatter = new PeriodFormatterBuilder()
    .appendHours().appendSuffix(":")
    .appendMinutes().appendSuffix(":")
    .appendSeconds()
    .toFormatter
  val dateRegex = """(\d{4})(\d{2})(\d{2})""".r

  implicit def parsePeriod(s: String): Period = {
    if (s == "" || s == "””" || s == "''") null else periodFormatter.parsePeriod(s)
  }

  implicit def parseLocalDate(s: String): LocalDate = {
    val dateRegex(year, month, day) = s
    new LocalDate(year.toInt, month.toInt, day.toInt)
  }

  implicit def parseLocalTime(s: String): LocalTime = {
    val chunks = s.split(":").map(_.toInt)
    require(chunks.length == 3)
    new LocalTime(chunks(0), chunks(1), chunks(2))
  }
}
