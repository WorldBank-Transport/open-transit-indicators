'use strict';

angular.module('transitIndicators')
.controller('OTIIndicatorsMapController',
        ['$scope', '$state', '$translate', 'leafletData',
         'OTICityManager', 'OTIEvents', 'OTIIndicatorManager', 'OTIIndicatorModel',
         'OTIIndicatorJobManager', 'OTIMapStyleService', 'OTIMapService', 'OTISettingsService',
        function ($scope, $state, $translate, leafletData,
                  OTICityManager, OTIEvents, OTIIndicatorManager, OTIIndicatorModel,
                  OTIIndicatorJobManager, OTIMapStyleService, OTIMapService, OTISettingsService) {

    $scope.$state = $state;
    $scope.dropdown_aggregation_open = false;
    $scope.dropdown_type_open = false;
    $scope.dropdown_indicatorjob_selection_open = false;
    $scope.selectedCity = _.findWhere($scope.cities, {id: OTIIndicatorManager.getConfig().calculation_job });

    OTIMapService.setScenario();
    $scope.indicator = OTIIndicatorManager.getConfig();
    var layerOptions = angular.extend($scope.indicator, {scenario: OTIMapService.getScenario()});

    // Cache legend names since they're used in a few different places.
    // Translation triggers a full page refresh, so this should be safe.
    var legendNames = {
        jobs: $translate.instant('MAP.JOBS_INDICATOR_LAYER'),
        pop1: $translate.instant('MAP.POPULATION_METRIC_ONE_LAYER'),
        pop2: $translate.instant('MAP.POPULATION_METRIC_TWO_LAYER'),
        dest: $translate.instant('MAP.DESTINATION_METRIC_LAYER')
    };


    angular.extend($scope.indicator,
        { modes: OTIMapService.getTransitModes() });
    OTIIndicatorManager.setConfig($scope.indicator);

    /* LEAFLET CONFIG */
    var overlays = {
        jobs_indicator: {
            name: legendNames.jobs,
            type: 'wms',
            url: 'gt/travelshed/jobs/render',
            visible: false,
            layerParams: {
                format: 'image/png',
                jobId: layerOptions.calculation_job
            },
            layerOptions: { opacity: 0.7 }
        },
        pop_metric_1: {
            name: legendNames.pop1,
            type: 'xyz',
            url: OTIMapService.demographicsUrl(),
            visible: false,
            layerOptions: { metric: 'population_metric_1', opacity: 0.7 }
        },
        pop_metric_2: {
            name: legendNames.pop2,
            type: 'xyz',
            url: OTIMapService.demographicsUrl(),
            visible: false,
            layerOptions: { metric: 'population_metric_2', opacity: 0.7 }
        },
        dest_metric_1: { // This is destination, not demographic, despite the i18n key name.
            name: legendNames.dest,
            type: 'xyz',
            url: OTIMapService.demographicsUrl(),
            visible: false,
            layerOptions: { metric: 'destination_metric_1', opacity: 0.7 }
        },
        indicator: {
            name: $translate.instant('MAP.GTFS_INDICATOR_LAYER'),
            type: 'xyz',
            url: OTIMapService.indicatorUrl('png'),
            visible: true,
            layerOptions: layerOptions
        },
        boundary: {
            name: $translate.instant('MAP.BOUNDARY'),
            type: 'xyz',
            url: OTIMapService.boundaryUrl(),
            visible: true,
            layerOptions: layerOptions
        },
        utfgrid: {
            name: $translate.instant('MAP.INDICATOR_INTERACTIVITY_LAYER'),
            type: 'utfGrid',
            url: OTIMapService.indicatorUrl('utfgrid'),
            visible: true,
            // When copied to the internal L.Utfgrid class, these options end up on
            //  layer.options, same as for TileLayers
            pluginOptions: angular.extend({ 'useJsonP': false }, $scope.indicator)
        }
    };
    $scope.updateLeafletOverlays(overlays);

    // Set calculation job
    $scope.selectCity = function (city) {
        $scope.selectedCity = city;
        OTIIndicatorManager.setConfig({ calculation_job: city.id });
    };

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

    $scope.leaflet.coverageLegend = {
        show: false,
        coverage: null,
        bufferDistance: null
    };

    // Coverage ratio stops buffer requires a special bit of massaging because it is systemic rather than per route
    // this means it requires its own directive to be shown
    var updateIndicatorLegend = function (indicator) {
        var params = angular.extend({}, indicator, {
            'ordering': 'value'
        });
        if (indicator.type && indicator.type === 'coverage_ratio_stops_buffer') {
            $scope.leaflet.legend = {};
            var minimalParams = {
                calculation_job: params.calculation_job,
                sample_period: params.sample_period,
                type: 'coverage_ratio_stops_buffer'
            };
            OTIIndicatorModel.search(_.extend({}, minimalParams, {type: 'coverage_ratio_stops_buffer'}))
                .$promise.then(function(indicatorResult) {
                    // Draw coverage legend
                    $scope.leaflet.coverageLegend.show = indicatorResult[0] ? true : false;
                    $scope.leaflet.coverageLegend.coverage = indicatorResult[0] ? indicatorResult[0].value : null;
                });
            OTIIndicatorModel.search(_.extend({}, minimalParams, {type: 'system_access'}))
                .$promise.then(function(indicatorResult) {
                    $scope.leaflet.coverageLegend.access1 = indicatorResult[0] ? indicatorResult[0].value : null;
                });
            OTIIndicatorModel.search(_.extend({}, minimalParams, {type: 'system_access_low'}))
                .$promise.then(function(indicatorResult) {
                    $scope.leaflet.coverageLegend.access2 = indicatorResult[0] ? indicatorResult[0].value : null;
                });
            OTISettingsService.configs.query().$promise.then(function(config){
                $scope.leaflet.coverageLegend.bufferDistance = config[0] ? config[0].nearby_buffer_distance_m : null;
            });
        } else {
            OTIIndicatorModel.search(params, function (data) {
                // Redraw new
                $scope.leaflet.legend = OTIMapStyleService.getLegend(indicator.type, data);
                $scope.leaflet.coverageLegend = {};
            });
        }
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
        OTIIndicatorManager.setConfig({calculation_job: calculation_job, city_name: ''}); // TODO: add city name
    });

    // Bind events that will hide/show legends when the respective layer is hidden / shown
    leafletData.getMap().then(function(map) {
        var setLegendVisibility = function(layerName, legendState) {
            switch(layerName) {
                case legendNames.jobs:
                    $scope.leaflet.jobsLegend.show = legendState;
                    break;
                case legendNames.pop1:
                    $scope.leaflet.pop1Legend.show = legendState;
                    break;
                case legendNames.pop2:
                    $scope.leaflet.pop2Legend.show = legendState;
                    break;
                case legendNames.dest:
                    $scope.leaflet.dest1Legend.show = legendState;
                    break;
            }
        };
        map.on('overlayadd', function(eventLayer) {
            setLegendVisibility(eventLayer.name, true);
        });
        map.on('overlayremove', function(eventLayer) {
            setLegendVisibility(eventLayer.name, false);
        });
    });


    $scope.init = function () {
        if($scope.cities.length > 0) {
            if (!$scope.selectedCity) {
                $scope.selectedCity = $scope.cities[0];
            }
            $scope.selectCity($scope.selectedCity);
        }
        updateIndicatorLegend($scope.indicator);
        OTIMapService.getLegendData();
        // Set up demographic legends (static)
        $scope.leaflet.jobsLegend = OTIMapStyleService.getDemographicLegend('green8',
            {
                show: false,
                title: $translate.instant('MAP.JOBS_INDICATOR_TITLE')
            }
        );
        $scope.leaflet.pop1Legend = OTIMapStyleService.getDemographicLegend('red5',
                {
                    show: false,
                    title: $translate.instant('MAP.POPULATION_METRIC_ONE_LAYER')
                }
        );
        $scope.leaflet.pop2Legend = OTIMapStyleService.getDemographicLegend('orange5',
            {
                show: false,
                title: $translate.instant('MAP.POPULATION_METRIC_TWO_LAYER')
            }
        );
        $scope.leaflet.dest1Legend = OTIMapStyleService.getDemographicLegend('blue5',
            {
                show: false,
                title: $translate.instant('MAP.DESTINATION_METRIC_LAYER')
            }
        );
    };

    $scope.$on(OTICityManager.Events.CitiesUpdated, function () {
        $scope.init();
    });

    $scope.init();
}]);
