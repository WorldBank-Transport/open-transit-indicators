'use strict';

/**

This module is exclusively used to generate an angular-leaflet-directive overlay config
for the transit and scenario map views, since they use the same overlay configuration

The scenario to be displayed is handled by the OTIMapService

*/
angular.module('transitIndicators')
.factory('OTITransitMapService', ['leafletData', 'OTIIndicatorManager', 'OTIMapService',
        function (leafletData, OTIIndicatorManager, OTIMapService) {

    var module = {};

    module.createTransitMapOverlayConfig = function () {
        var boundaryIndicator = OTIIndicatorManager.getConfig();
        var scenario = OTIMapService.getScenario();
        return {
            boundary: {
                name: 'Boundary',
                type: 'xyz',
                url: OTIMapService.boundaryUrl(),
                visible: true,
                layerOptions: boundaryIndicator
            },
            gtfs_shapes: {
                name: 'Transit Routes',
                type: 'xyz',
                url: OTIMapService.gtfsShapesUrl(),
                visible: true,
                layerOptions: {
                    modes: OTIMapService.getTransitModes(),
                    scenario: scenario
                }
            },
            gtfs_stops: {
                name: 'Transit Stops',
                type: 'xyz',
                url: OTIMapService.gtfsStopsUrl('png'),
                visible: true,
                layerOptions: {
                    modes: OTIMapService.getTransitModes(),
                    scenario: scenario
                }
            },
            gtfs_stops_utfgrid: {
                name: 'Transit Stops Interactivity',
                type: 'utfGrid',
                url: OTIMapService.gtfsStopsUrl('utfgrid'),
                visible: true,
                pluginOptions: {
                    'useJsonP': false,
                    modes: OTIMapService.getTransitModes(),
                    scenario: scenario
                }
            }
        };
    };

    return module;
}]);
