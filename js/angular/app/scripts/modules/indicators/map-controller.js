'use strict';

angular.module('transitIndicators')
.controller('OTIIndicatorsMapController',
        ['$cookieStore', '$rootScope', '$scope', '$state',
         'leafletData', 'OTIEvents', 'OTIIndicatorManager', 'OTIIndicatorModel',
         'OTIIndicatorJobManager', 'OTIMapStyleService', 'OTIMapService',
        function ($cookieStore, $rootScope, $scope, $state,
                  leafletData, OTIEvents, OTIIndicatorManager, OTIIndicatorModel,
                  OTIIndicatorJobManager, OTIMapStyleService, OTIMapService) {

    $scope.$state = $state;
    $scope.dropdown_aggregation_open = false;
    $scope.dropdown_type_open = false;

    OTIMapService.setScenario();
    $scope.indicator = OTIIndicatorManager.getConfig();
    var layerOptions = angular.extend($scope.indicator, {scenario: OTIMapService.getScenario()});

    angular.extend($scope.indicator,
        { modes: OTIMapService.getTransitModes() });
    OTIIndicatorManager.setConfig($scope.indicator);

    /* LEAFLET CONFIG */
    var overlays = {
        jobs_indicator: {
            name: 'Jobs Indicator',
            type: 'wms',
            url: 'gt/travelshed/jobs/render',
            visible: true,
            layerParams: {
                format: 'image/png',
            },
            layerOptions: { opacity: 0.7 }
        },
        indicator: {
            name: 'GTFS Indicator',
            type: 'xyz',
            url: OTIMapService.indicatorUrl('png'),
            visible: true,
            layerOptions: layerOptions
        },
        boundary: {
            name: 'Boundary',
            type: 'xyz',
            url: OTIMapService.boundaryUrl(),
            visible: true,
            layerOptions: layerOptions
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

    var updateIndicatorLegend = function (indicator) {
        var params = angular.extend({}, indicator, {
            'ordering': 'value'
        });
        OTIIndicatorModel.search(params, function (data) {
            // Redraw new
            $scope.leaflet.legend = OTIMapStyleService.getLegend(indicator.type, data);
        });
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
        OTIMapService.refreshLayers();
        updateIndicatorLegend(indicator);
    };

    $scope.selectType = function (type) {
        $scope.dropdown_type_open = false;
        OTIIndicatorManager.setConfig({type: type});
    };

    $scope.selectAggregation = function (aggregation) {
        $scope.dropdown_aggregation_open = false;
        OTIIndicatorManager.setConfig({aggregation: aggregation});
    };

    $scope.$on('leafletDirectiveMap.utfgridClick', function(event, leafletEvent) {
        $scope.leaflet.markers.length = 0;
        if (leafletEvent.data && leafletEvent.data.indicator_id) {
            OTIIndicatorModel.get({id: leafletEvent.data.indicator_id}, function (indicator) {
                utfGridMarker(leafletEvent, indicator);
            });
        }
    });

    $scope.$on(OTIIndicatorManager.Events.IndicatorConfigUpdated, function (event, indicator) {
        $scope.updateIndicatorLayers(indicator);
    });

    $scope.$on(OTIIndicatorManager.Events.SamplePeriodUpdated, function (event, sample_period) {
        OTIIndicatorManager.setConfig({sample_period: sample_period});
    });

    $scope.$on(OTIIndicatorJobManager.Events.JobUpdated, function (event, calculation_job) {
        OTIIndicatorManager.setConfig({calculation_job: calculation_job});
    });

    $scope.init = function () {
        updateIndicatorLegend($scope.indicator);
        OTIMapService.getLegendData();
    };
    $scope.init();
}]);
