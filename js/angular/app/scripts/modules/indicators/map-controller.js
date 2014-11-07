'use strict';

angular.module('transitIndicators')
.controller('OTIIndicatorsMapController',
        ['$cookieStore', '$rootScope', '$scope', '$state',
         'config', 'leafletData', 'OTIEvents', 'OTIIndicatorsService', 'OTIIndicatorsMapService',
         'OTIMapStyleService', 'OTIMapService',
        function ($cookieStore, $rootScope, $scope, $state,
                  config, leafletData, OTIEvents, OTIIndicatorsService, OTIIndicatorsMapService,
                  OTIMapStyleService, OTIMapService) {

    var defaultIndicator = new OTIIndicatorsService.IndicatorConfig({
        calculation_job: 0,
        type: 'num_stops',
        sample_period: 'morning',
        aggregation: 'route'
    });

    $scope.$state = $state;
    $scope.dropdown_aggregation_open = false;
    $scope.dropdown_type_open = false;

    // Object used to configure the indicator displayed on the map
    // Retrieve last selected indicator from session cookie, if available
    $scope.indicator = $cookieStore.get('indicator') || defaultIndicator;

    angular.extend($scope.indicator,
        { modes: OTIMapService.getTransitModes() });

    /* LEAFLET CONFIG */
    var overlays = {
        indicator: {
            name: 'GTFS Indicator',
            type: 'xyz',
            url: OTIMapService.indicatorUrl('png'),
            visible: true,
            layerOptions: $scope.indicator
        },
        boundary: {
            name: 'Boundary',
            type: 'xyz',
            url: OTIMapService.boundaryUrl(),
            visible: true,
            layerOptions: $scope.indicator
        },
        utfgrid: {
            name: 'GTFS Indicator Interactivity',
            type: 'utfGrid',
            url: OTIMapService.indicatorUrl('utfgrid'),
            visible: true,
            // When copied to the internal L.Utfgrid class, these options end up on
            //  layer.options, same as for TileLayers
            pluginOptions: angular.extend({ 'useJsonP': false }, $scope.indicator)
        }
    };
    $scope.updateLeafletOverlays(overlays);

    // Create utfgrid popup from leaflet event
    var utfGridMarker = function (leafletEvent, indicator) {
        if (leafletEvent && leafletEvent.data && indicator) {
            var marker = {
                lat: leafletEvent.latlng.lat,
                lng: leafletEvent.latlng.lng,
                focus: true,
                draggable: false,
                message: indicator.formatted_value,
                // we need something to bind the popup to, so use a marker with an empty icon
                icon: {
                    type: 'div',
                    iconSize: [0, 0],
                    popupAnchor:  [0, 0]
                }
            };
            $scope.leaflet.markers.push(marker);
        }
    };

    var setIndicator = function (indicator) {
        angular.extend($scope.indicator, indicator);
        $cookieStore.put('indicator', $scope.indicator);
        $scope.$broadcast(OTIEvents.Indicators.IndicatorUpdated, $scope.indicator);
    };

    var updateIndicatorLegend = function (indicator) {
        var params = angular.extend({}, indicator, {
            'ordering': 'value'
        });
        OTIIndicatorsService.query('GET', params).then(function (data) {
            // Redraw new
            $scope.leaflet.legend = OTIMapStyleService.getLegend(indicator.type, data);
        });
    };

    $scope.setIndicator = function (options) {
        angular.extend($scope.indicator, options);
        $scope.updateIndicatorLayers($scope.indicator);
        $scope.indicator_dropdown_open = false;
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
        $scope.indicator.modes = OTIMapService.getTransitModes();
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

        updateIndicatorLegend(indicator);
    };

    $scope.selectType = function (type) {
        $scope.dropdown_type_open = false;
        setIndicator({type: type});
    };

    $scope.selectAggregation = function (aggregation) {
        $scope.dropdown_aggregation_open = false;
        setIndicator({aggregation: aggregation});
    };

    $scope.$on('leafletDirectiveMap.utfgridClick', function(event, leafletEvent) {
        $scope.leaflet.markers.length = 0;
        if (leafletEvent.data && leafletEvent.data.indicator_id) {
            var formattedValue = OTIIndicatorsMapService.getIndicatorFormattedValue(leafletEvent.data.indicator_id);
            formattedValue.then(function(indicator) {
                utfGridMarker(leafletEvent, indicator);
            });
        }
    });

    $scope.$on(OTIEvents.Indicators.IndicatorUpdated, function (event, indicator) {
        $scope.updateIndicatorLayers(indicator);
    });

    $scope.$on(OTIEvents.Indicators.SamplePeriodUpdated, function (event, sample_period) {
        setIndicator({sample_period: sample_period});
    });

    $scope.$on(OTIEvents.Indicators.IndicatorCalcJobUpdated, function (event, calculation_job) {
        setIndicator({calculation_job: calculation_job});
    });

    $scope.init = function () {
        updateIndicatorLegend($scope.indicator);
        OTIIndicatorsMapService.getLegendData().then(function() {
            $rootScope.$broadcast(OTIMapService.Events.AvailableModesUpdated,
                                  OTIIndicatorsMapService.modes);
        });
    };
    $scope.init();
}]);
