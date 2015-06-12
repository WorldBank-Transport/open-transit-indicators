package com.azavea.opentransit

object CSVCache {
  private var cacheValue: Option[Array[Byte]] = None
  def get(): Option[Array[Byte]] = cacheValue
  def set(csvBytes: Array[Byte]): Unit = {
    cacheValue = Some(csvBytes)
  }
}
