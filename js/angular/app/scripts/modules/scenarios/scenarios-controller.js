'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosController',
            ['config', '$scope', '$rootScope', 'OTIEvents', 'OTIIndicatorsMapService',
            function (config, $scope, $rootScope, OTIEvents, OTIIndicatorsMapService) {

    var overlays = {
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

    var setLegend = function () {
        if($rootScope.cache.transitLegend) {
            $scope.leaflet.legend = $rootScope.cache.transitLegend;
            return;
        }
        OTIIndicatorsMapService.getRouteTypeLabels().then(function (labels) {
            var legend = {
                colors: config.gtfsRouteTypeColors,
                labels: labels
            };
            $rootScope.cache.transitLegend = legend;
            $scope.leaflet.legend = legend;
        });
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

}]);
