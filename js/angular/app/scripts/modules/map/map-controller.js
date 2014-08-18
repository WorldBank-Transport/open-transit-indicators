'use strict';

angular.module('transitIndicators')
.controller('OTIMapController',
        ['config', '$scope', '$state', '$location', 'leafletData', 'OTIMapService', 'authService',
        function (config, $scope, $state, $location, leafletData, mapService, authService) {

    $scope.$state = $state;

    $scope.logout = function () {
        authService.logout();
    };

    var windshaftHost = $location.protocol() + '://' + $location.host();
    if (config.windshaft && config.windshaft.port) {
        windshaftHost += ':' + config.windshaft.port;
    }

    /* LEAFLET CONFIG */
    var layers = {
        overlays: {
            // TODO: Dynamically create/destroy these layers based on UI selection
            indicators: {
                name: 'GTFS Indicator',
                type: 'xyz',
                url: windshaftHost + '/tiles/transit_indicators/1408110474/num_stops/morning/route/{z}/{x}/{y}.png',
                visible: true
            },
            indicators_utfgrid: {
                name: 'GTFS Indicator Interactivity',
                type: 'utfGrid',
                url: windshaftHost + '/tiles/transit_indicators/1408110474/num_stops/morning/route/{z}/{x}/{y}.grid.json?interactivity=value',
                visible: true,
                pluginOptions: { 'useJsonP': false }
            }
        }
    };

    var leaflet = {
        layers: angular.extend(config.leaflet.layers, layers),
        markers: []
    };
    $scope.leaflet = angular.extend(config.leaflet, leaflet);

    $scope.$on('leafletDirectiveMap.utfgridClick', function(event, leafletEvent) {
        // we need something to bind the popup to, so use a marker with an empty icon

        $scope.leaflet.markers.length = 0;

        if (leafletEvent.data) {
            // use $apply so popup appears right away
            // (otherwise it doesn't show up until the next time the mouse gets moved)
            $scope.$apply(function() {
                var marker = {
                    lat: leafletEvent.latlng.lat,
                    lng: leafletEvent.latlng.lng,
                    focus: true,
                    draggable: false,
                    message: 'Indicator Value: ' + leafletEvent.data.value,
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

    // asks the server for the data extent and zooms to it
    var zoomToDataExtent = function () {
        mapService.getMapInfo().then(function(mapInfo) {
            if (mapInfo.extent) {
                $scope.leaflet.bounds = mapInfo.extent;
            } else {
                // no extent; zoom out to world
                $scope.leaflet.bounds = config.worldExtent;
            }
        });
    };

    // always zoom to the extent when the map is first loaded
    zoomToDataExtent();

    // zoom to the new extent whenever a GTFS file is uploaded
    $scope.$on('upload-controller:gtfs-uploaded', function() {
        zoomToDataExtent();
    });

    // zoom out to world view when data deleted
    $scope.$on('upload-controller:gtfs-deleted', function() {
        $scope.leaflet.bounds = config.worldExtent;
    });
}]);
