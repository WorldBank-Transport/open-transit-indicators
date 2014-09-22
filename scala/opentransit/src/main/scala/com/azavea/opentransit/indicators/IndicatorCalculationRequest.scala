package com.azavea.opentransit.indicators

import com.github.nscala_time.time.Imports._

// Calculation request parameters
case class IndicatorCalculationRequest(
  token: String,
  version: String,
  poverty_line: Double,
  nearby_buffer_distance_m: Double,
  max_commute_time_s: Int,
  max_walk_time_s: Int,
  city_boundary_id: Int,
  region_boundary_id: Int,
  avg_fare: Double,
  sample_periods: List[SamplePeriod]
)

case class SamplePeriod(
  id: Int,
  `type`: String,
  period_start: DateTime,
  period_end: DateTime
)

// object IndicatorCalculationRequest {
//   val jsonFormat = jsonFormat10(CalcParams)
// }
