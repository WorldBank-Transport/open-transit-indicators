'use strict';

angular.module('transitIndicators')
.controller('OTIIndicatorsMapController',
        ['config', '$cookieStore', '$scope', '$state', 'leafletData', 'OTIIndicatorsService', 'OTIIndicatorsMapService',
        function (config, $cookieStore, $scope, $state, leafletData, OTIIndicatorsService, mapService) {

    $scope.$state = $state;
    $scope.indicator_dropdown_open = false;

    // Object used to configure the indicator displayed on the map
    // Retrieve last selected indicator from session cookie, if available
    $scope.indicator = $cookieStore.get('indicator');

    if (!$scope.indicator) {
        $scope.indicator = new mapService.IndicatorConfig({
            version: 0,
            type: 'num_stops',
            sample_period: 'morning',
            aggregation: 'route'
        });

        $cookieStore.put('indicator', $scope.indicator);
    }

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
    $scope.leaflet = angular.extend($scope.leafletDefaults, leaflet);

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
        $scope.$broadcast('map-controller:set-indicator-version', version);
    };

    /**
     * Indicator type to configure the layers with
     * Use the map object directly to iterate over layers
     * angular-leaflet-directive does not support a way to redraw existing layers that have
     * updated properties but haven't changed their layer key
     *
     * @param indicator: OTIIndicatorsService.Indicator instance
     */
    $scope.updateIndicatorLayers = function (indicator) {
        $cookieStore.put('indicator', $scope.indicator);
        leafletData.getMap().then(function(map) {
            map.eachLayer(function (layer) {
                // layer is one of the indicator overlays -- only redraw them
                if (layer && layer.options && layer.options.type) {
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

    $scope.$on('leafletDirectiveMap.utfgridClick', function(event, leafletEvent) {
        $scope.leaflet.markers.length = 0;
        utfGridMarker(leafletEvent);
    });

    $scope.$on('map-controller:set-indicator-version', function (event, version) {
        $scope.indicator.version = version;
        $scope.updateIndicatorLayers($scope.indicator);
    });

    $scope.$on(OTIIndicatorsService.Events.IndicatorUpdated, function (event, indicator) {
        $scope.updateIndicatorLayers(indicator);
    });

    $scope.init = function () {
        OTIIndicatorsService.getIndicatorVersion(function (version) {
            $scope.setIndicatorVersion(version);
        });
    };
    $scope.init();
}]);
