package com.azavea.gtfs

case class Agency(
  id: String,
  name: String,
  url: String,
  timezone: String,
  language: Option[String] = None,
  phone: Option[String] = None,
  fareUrl: Option[String] = None
)
