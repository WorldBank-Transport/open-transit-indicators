'use strict';

/**

This module is exclusively used to generate an angular-leaflet-directive overlay config
for the transit and scenario map views, since they use the same overlay configuration

The scenario to be displayed is handled by the OTIMapService

*/
angular.module('transitIndicators')
.factory('OTITransitMapService', ['$translate', 'leafletData', 'OTIIndicatorManager', 'OTIMapService',
        function ($translate, leafletData, OTIIndicatorManager, OTIMapService) {

    var module = {};

    module.createTransitMapOverlayConfig = function () {
        var boundaryIndicator = OTIIndicatorManager.getConfig();
        var scenario = OTIMapService.getScenario();
        return {
            boundary: {
                name: $translate.instant('MAP.BOUNDARY'),
                type: 'xyz',
                url: OTIMapService.boundaryUrl(),
                visible: true,
                layerOptions: boundaryIndicator
            },
            gtfs_stop_deltas: {
                name: $translate.instant('MAP.TRANSIT_ROUTES'),
                type: 'xyz',
                url: OTIMapService.gtfsStopDeltasUrl(),
                visible: true,
                layerOptions: {
                    modes: OTIMapService.getTransitModes(),
                    scenario: OTIMapService.getScenario()
                }
            },
            gtfs_shape_deltas: {
                name: $translate.instant('MAP.TRANSIT_ROUTES'),
                type: 'xyz',
                url: OTIMapService.gtfsShapeDeltasUrl(),
                visible: true,
                layerOptions: {
                    modes: OTIMapService.getTransitModes(),
                    scenario: OTIMapService.getScenario()
                }
            },
            gtfs_shapes: {
                name: $translate.instant('MAP.TRANSIT_ROUTES'),
                type: 'xyz',
                url: OTIMapService.gtfsShapesUrl(),
                visible: true,
                layerOptions: {
                    modes: OTIMapService.getTransitModes(),
                    scenario: scenario
                }
            },
            gtfs_stops: {
                name: $translate.instant('MAP.TRANSIT_STOPS'),
                type: 'xyz',
                url: OTIMapService.gtfsStopsUrl('png'),
                visible: true,
                layerOptions: {
                    modes: OTIMapService.getTransitModes(),
                    scenario: scenario
                }
            },
            gtfs_stops_utfgrid: {
                name: $translate.instant('MAP.TRANSIT_STOPS_INTERACTIVITY'),
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
