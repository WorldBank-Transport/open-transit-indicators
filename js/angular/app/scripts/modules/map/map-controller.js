'use strict';

angular.module('transitIndicators')
.controller('OTIMapController',
        ['config', '$scope', '$state', 'leafletData', 'OTIMapService', 'authService',
        function (config, $scope, $state, leafletData, mapService, authService) {

    $scope.$state = $state;
    $scope.indicator_dropdown_open = false;

    // Object used to configure the indicator displayed on the map
    // Will never actually be posted or saved
    // TODO: Save this indicator somewhere else to save map state on refresh
    //       or set via page GET params
    $scope.indicator = {
        version: 0,
        type: 'num_stops',
        sample_period: 'morning',
        aggregation: 'route'
    };

    /* LEAFLET CONFIG */
    var layers = {
        overlays: {
            indicator: {
                name: 'GTFS Indicator',
                type: 'xyz',
                url: mapService.getIndicatorUrl('png'),
                visible: true,
                layerOptions: $scope.indicator
            },
            utfgrid: {
                name: 'GTFS Indicator Interactivity',
                type: 'utfGrid',
                url: mapService.getIndicatorUrl('utfgrid'),
                visible: true,
                // When copied to the internal L.Utfgrid class, these options end up on
                //  layer.options, same as for TileLayers
                pluginOptions: angular.extend({ 'useJsonP': false }, $scope.indicator)
            }
        }
    };

    var leaflet = {
        layers: angular.extend(config.leaflet.layers, layers),
        markers: []
    };
    $scope.leaflet = angular.extend(config.leaflet, leaflet);

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

    // Create utfgrid popup from leaflet event
    var utfGridMarker = function (leafletEvent) {
        if (leafletEvent && leafletEvent.data) {
            // use $apply so popup appears right away
            // (otherwise it doesn't show up until the next time the mouse gets moved)
            $scope.$apply(function() {
                var marker = {
                    lat: leafletEvent.latlng.lat,
                    lng: leafletEvent.latlng.lng,
                    focus: true,
                    draggable: false,
                    message: 'Indicator Value: ' + leafletEvent.data.value,
                    // we need something to bind the popup to, so use a marker with an empty icon
                    icon: {
                        type: 'div',
                        iconSize: [0, 0],
                        popupAnchor:  [0, 0]
                    }
                };
                $scope.leaflet.markers.push(marker);
            });
        }
    };

    $scope.setIndicatorVersion = function (version) {
        $scope.indicator_version = version;
        $scope.$broadcast('map-controller:set-indicator-version', version);
    };

    // TODO: Update this method to allow changes on aggregation, version, sample_period
    $scope.setIndicator = function (type) {
        $scope.indicator.type = type;
        $scope.updateIndicatorLayers($scope.indicator);
        $scope.indicator_dropdown_open = false;
    };

    /**
     * Indicator type to configure the layers with
     * Use the map object directly to iterate over layers
     * angular-leaflet-directive does not support a way to redraw existing layers that have
     * updated properties but haven't changed their layer key
     *
     * @param indicator: mapService.Indicator instance
     */
    $scope.updateIndicatorLayers = function (indicator) {
        leafletData.getMap().then(function(map) {
            map.eachLayer(function (layer) {
                if (!layer) {
                    return;
                }
                // layer is one of the indicator overlays
                if (layer.options.type) {
                    angular.extend(layer.options, indicator);
                    if (layer.redraw) {
                        layer.redraw();
                    } else if (layer._update) {
                        // Temporary hack for Leaflet.Utfgrid. It doesn't have a redraw function
                        layer._cache = {};
                        layer._update();
                    }
                }
            });
        });
    };

    $scope.logout = function () {
        authService.logout();
    };

    $scope.$on('leafletDirectiveMap.utfgridClick', function(event, leafletEvent) {
        $scope.leaflet.markers.length = 0;
        utfGridMarker(leafletEvent);
    });

    // zoom to the new extent whenever a GTFS file is uploaded
    $scope.$on('upload-controller:gtfs-uploaded', function() {
        zoomToDataExtent();
    });

    // zoom out to world view when data deleted
    $scope.$on('upload-controller:gtfs-deleted', function() {
        $scope.leaflet.bounds = config.worldExtent;
    });

    $scope.$on('map-controller:set-indicator-version', function (event, version) {
        $scope.indicator.version = version;
        $scope.updateIndicatorLayers($scope.indicator);
    });

    $scope.init = function () {
        // always zoom to the extent when the map is first loaded
        zoomToDataExtent();

        mapService.Indicator.query({}, function(data){
            var currentIndicator = _.sortBy(data, 'version').pop();
            $scope.setIndicatorVersion(currentIndicator.version);
        });
    };
    $scope.init();
}]);
