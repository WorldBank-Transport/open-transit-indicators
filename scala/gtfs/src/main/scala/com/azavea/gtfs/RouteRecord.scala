package com.azavea.gtfs

case class RouteRecord(
  id: String,
  shortName: String,
  longName: String,
  routeType: RouteType,
  agencyId: Option[String] = None,
  description: Option[String] = None,
  url: Option[String] = None,
  color: Option[String] = None,
  textColor: Option[String] = None
) 
