package com.azavea.opentransit.indicators.stations

import com.azavea.opentransit._
import com.azavea.opentransit.database._

import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.CSVStrategy.DEFAULT_STRATEGY
import java.io.{ByteArrayOutputStream, OutputStreamWriter, FileOutputStream}
import java.util.UUID

class StationStatsCSV(
  bufferDistance: Double,
  commuteTime: Int,
  jobId: Int,
  csvStore: StationCSVStore = StationCSVDatabase,
  outputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
) extends CSVPrinter(new OutputStreamWriter(outputStream)) {
  private val wrapper = this

  def value: String = new String(outputStream.toByteArray)

  def save(status: CSVStatus): Unit =
    csvStore.set(CSVJob(status, bufferDistance, commuteTime, jobId, outputStream.toByteArray))

  def writeFile(path: String): Unit = {
    val fos = new FileOutputStream(path)
    try {
      fos.write(outputStream.toByteArray)
    } finally {
      fos.close()
    }
  }

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
      wrapper.print("stationID")
      wrapper.print("stationName")
      wrapper.print("proximalPop1")
      wrapper.print("proximalPop2")
      wrapper.print("proximalJobs")
      wrapper.print("accessibleJobs")
      wrapper.println()
      header = true
    }
  }
}

object StationStatsCSV {
  def apply(
    bufferDistance: Double,
    commuteTime: Int,
    jobId: Int,
    csvStore: StationCSVStore = StationCSVDatabase
  ): StationStatsCSV = {
    val printer = new StationStatsCSV(bufferDistance, commuteTime, jobId)
    csvStore.set(CSVJob(Processing, bufferDistance, commuteTime, jobId, Array.empty))
    printer.setStrategy(DEFAULT_STRATEGY)
    printer.attachHeader()
    printer
  }
}
