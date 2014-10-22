'use strict';
angular.module('transitIndicators')
.controller('OTITransitController',
            ['config', '$scope', '$rootScope', 'OTIEvents', 'OTIIndicatorsService', 'OTIIndicatorsMapService', 'OTIMapStyleService',
            function (config, $scope, $rootScope, OTIEvents, OTIIndicatorsService, OTIIndicatorsMapService, OTIMapStyleService) {

    var boundaryIndicator = new OTIIndicatorsService.IndicatorConfig({
        version: 0,
        type: 'num_stops',
        sample_period: 'morning',
        aggregation: 'route'
    });

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
            visible: true
        },
        gtfs_stops: {
            name: 'Transit Stops',
            type: 'xyz',
            url: OTIIndicatorsMapService.getGTFSStopsUrl('png'),
            visible: true
        },
        gtfs_stops_utfgrid: {
            name: 'Transit Stops Interactivity',
            type: 'utfGrid',
            url: OTIIndicatorsMapService.getGTFSStopsUrl('utfgrid'),
            visible: true,
            pluginOptions: { 'useJsonP': false }
        }
    };

    var updateLegend = function () {
        OTIIndicatorsMapService.getRouteTypeLabels().then(function (labels) {
            var legend = {
                colors: OTIMapStyleService.routeTypeColorRamp(),
                labels: labels
            };
            $rootScope.cache.transitLegend = legend;
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
}]);
