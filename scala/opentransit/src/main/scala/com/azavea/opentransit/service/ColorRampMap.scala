package com.azavea.opentransit.service

import geotrellis.raster.render._

/** Provides a string keyed map to predefined color ramps
  * to be used for coloring rendered rasters.
  */
object ColorRampMap {
  val rampMap =
    Map(
    "blue-to-orange" -> ColorRamps.BlueToOrange,
    "green-to-orange" -> ColorRamps.LightYellowToOrange,
    "blue-to-red" -> ColorRamps.BlueToRed,
    "green-to-red-orange" -> ColorRamps.GreenToRedOrange,
    "light-to-dark-sunset" -> ColorRamps.LightToDarkSunset,
    "light-to-dark-green" -> ColorRamps.LightToDarkGreen,
    "yellow-to-red-heatmap" -> ColorRamps.HeatmapYellowToRed,
    "blue-to-yellow-to-red-heatmap" -> ColorRamps.HeatmapBlueToYellowToRedSpectrum,
    "dark-red-to-yellow-heatmap" -> ColorRamps.HeatmapDarkRedToYellowWhite,
    "purple-to-dark-purple-to-white-heatmap" -> ColorRamps.HeatmapLightPurpleToDarkPurpleToWhite,
    "bold-land-use-qualitative" -> ColorRamps.ClassificationBoldLandUse,
    "muted-terrain-qualitative" -> ColorRamps.ClassificationMutedTerrain
  )

  def get(s:String) = rampMap.get(s)
  def getOrElse(s:String,cr:ColorRamp) = rampMap.getOrElse(s,cr)
}
