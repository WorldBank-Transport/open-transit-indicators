package com.azavea.opentransit.indicators.stations

import com.azavea.opentransit._

import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.CSVStrategy.DEFAULT_STRATEGY
import java.io.{ByteArrayOutputStream, OutputStreamWriter}
import java.io.{Writer, File}
import java.util.UUID

class StationStatsCSV(outputStream: ByteArrayOutputStream = new ByteArrayOutputStream())
    extends CSVPrinter(new OutputStreamWriter(outputStream)) {
  def value: String = outputStream.toByteArray.mkString
  def save(status: CSVStatus): Unit = {
    Main.csvCache.set(CSVJob(status, outputStream.toByteArray))
  }
  val wrapper = this

  case class StationStats(
    id: String,
    name: String,
    proximalPop1: Int,
    proximalPop2: Int,
    proximalJobs: Int,
    accessibleJobs: Int
  ) {
    def write(): Unit = {
      wrapper.print(id)
      wrapper.print(name)
      wrapper.print(proximalPop1.toString)
      wrapper.print(proximalPop2.toString)
      wrapper.print(proximalJobs.toString)
      wrapper.print(accessibleJobs.toString)
      wrapper.println()
    }
  }

  private var header = false
  def attachHeader(): Unit = {
    if (!header) {
      wrapper.print("proximalPop1")
      wrapper.print("proximalPop2")
      wrapper.print("proximalJobs")
      wrapper.print("accessibleJobs")
      wrapper.println()
      header = true
    }
  }
}

object StationStats {
  def apply(): StationStatsCSV = {
    val printer = new StationStatsCSV()
    CSVCache.set(CSVJob(Pending, Array.empty))
    printer.setStrategy(DEFAULT_STRATEGY)
    printer.attachHeader()
    printer
  }
}
