'use strict';
angular.module('transitIndicators')
.controller('OTITransitController',
            ['config', '$scope', '$rootScope', 'OTIEvents', 'OTIIndicatorsService', 'OTIIndicatorsMapService', 'OTIMapStyleService',
            function (config, $scope, $rootScope, OTIEvents, OTIIndicatorsService, OTIIndicatorsMapService, OTIMapStyleService) {

    var boundaryIndicator = new OTIIndicatorsService.IndicatorConfig({
        calculation_job: 0,
        type: 'num_stops',
        sample_period: 'morning',
        aggregation: 'route'
    });

    $scope.modes = '';

    var overlays = {
        boundary: {
            name: 'Boundary',
            type: 'xyz',
            url: OTIIndicatorsMapService.getBoundaryUrl(),
            visible: true,
            layerOptions: boundaryIndicator
        },
        gtfs_shapes: {
            name: 'Transit Routes',
            type: 'xyz',
            url: OTIIndicatorsMapService.getGTFSShapesUrl(),
            visible: true,
            layerParams: { modes: '' }
        },
        gtfs_stops: {
            name: 'Transit Stops',
            type: 'xyz',
            url: OTIIndicatorsMapService.getGTFSStopsUrl('png'),
            visible: true,
            layerParams: { modes: '' }
        },
        gtfs_stops_utfgrid: {
            name: 'Transit Stops Interactivity',
            type: 'utfGrid',
            url: OTIIndicatorsMapService.getGTFSStopsUrl('utfgrid'),
            visible: true,
            pluginOptions: { 'useJsonP': false },
            layerParams: { modes: '' }
        }
    };

    var updateLegend = function () {
        OTIIndicatorsMapService.getLegendData().then(function (legend) {
            legend.style = 'stacked';
            $rootScope.cache.transitLegend = legend;
            $rootScope.$broadcast('OTIEvent:Root:AvailableModesUpdated',
                OTIIndicatorsMapService.modes);
            $scope.leaflet.legend = legend;
        });
    };

    var setLegend = function () {
        if($rootScope.cache.transitLegend) {
            $scope.leaflet.legend = $rootScope.cache.transitLegend;
            return;
        }
        updateLegend();
    };

    $scope.updateLeafletOverlays(overlays);
    setLegend();

    $scope.$on('leafletDirectiveMap.utfgridClick', function (event, leafletEvent) {
        $scope.leaflet.markers.length = 0;
        if (leafletEvent && leafletEvent.data && leafletEvent.data.stop_routes) {
            $scope.$apply(function () {
                var marker = {
                    lat: leafletEvent.latlng.lat,
                    lng: leafletEvent.latlng.lng,
                    message: leafletEvent.data.stop_routes,
                    focus: true,
                    draggable: false,
                    icon: {
                        type: 'div',
                        iconSize: [0, 0],
                        popupAnchor:  [0, 0]
                    }
                };
                $scope.leaflet.markers.push(marker);
            });
        }
    });

    $rootScope.$on('$translateChangeSuccess', function() {
        updateLegend();
    });

    // This may not be the best place to update the legend on GTFS
    // update, but most of the other legend updating code was here
    $rootScope.$on(OTIEvents.Settings.Upload.GTFSDone, function () {
        OTIIndicatorsMapService.getLegendData().then(function (legend) {
            $rootScope.cache.transitLegend = legend;
            $rootScope.cache.transitLegend.style = 'stacked';
            // only update cache so we don't show legend on the
            // settings view
        });
    });
}]);
