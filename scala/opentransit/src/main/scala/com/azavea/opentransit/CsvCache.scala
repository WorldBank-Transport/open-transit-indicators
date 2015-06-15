package com.azavea.opentransit

sealed trait CSVStatus
case object Success extends CSVStatus
case object Pending extends CSVStatus
case object Failure extends CSVStatus
case object Empty extends CSVStatus

case class CSVJob(status: CSVStatus, data: Array[Byte])

object CSVCache {
  private var cacheValue: CSVJob = CSVJob(status = Empty, data = Array.empty)
  def get(): CSVJob = cacheValue
  def set(csvJob: CSVJob): Unit = {
    cacheValue = csvJob
  }
}
