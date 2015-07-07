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
        absoluteJobs: $translate.instant('MAP.ABSOLUTE_JOBS_INDICATOR_LAYER'),
        percentageJobs: $translate.instant('MAP.PERCENTAGE_JOBS_INDICATOR_LAYER'),
        pop1: $translate.instant('MAP.POPULATION_METRIC_ONE_LAYER'),
        pop2: $translate.instant('MAP.POPULATION_METRIC_TWO_LAYER'),
        dest: $translate.instant('MAP.DESTINATION_METRIC_LAYER'),

        // Only shown when ranges can't be retrieved
        fewer: $translate.instant('MAP.JOBS_INDICATOR_FEWER'),
        more: $translate.instant('MAP.JOBS_INDICATOR_MORE')
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
                jobId: layerOptions.calculation_job,
                indicator: 'jobs_travelshed'
            },
            layerOptions: { opacity: 0.7 }
        },
        jobs_absolute_indicator: {
            name: legendNames.absoluteJobs,
            type: 'wms',
            url: 'gt/travelshed/jobs/render',
            visible: false,
            layerParams: {
                format: 'image/png',
                jobId: layerOptions.calculation_job,
                indicator: 'jobs_absolute_travelshed'
            },
            layerOptions: { opacity: 0.7 }
        },
        jobs_percentage_indicator: {
            name: legendNames.percentageJobs,
            type: 'wms',
            url: 'gt/travelshed/jobs/render',
            visible: false,
            layerParams: {
                format: 'image/png',
                jobId: layerOptions.calculation_job,
                indicator: 'jobs_percentage_travelshed'
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
        },
        gtfs_shapes: {
            name: $translate.instant('MAP.TRANSIT_ROUTES'),
            type: 'xyz',
            url: OTIMapService.gtfsShapesUrl(),
            visible: false,
            layerOptions: {
                modes: OTIMapService.getTransitModes(),
                scenario: OTIMapService.getScenario()
            }
        },
        gtfs_stops: {
            name: $translate.instant('MAP.TRANSIT_STOPS'),
            type: 'xyz',
            url: OTIMapService.gtfsStopsUrl('png'),
            visible: true,
            layerOptions: {
                modes: OTIMapService.getTransitModes(),
                scenario: OTIMapService.getScenario()
            }
        },
        gtfs_stops_utfgrid: {
            name: $translate.instant('MAP.TRANSIT_STOPS_INTERACTIVITY'),
            type: 'utfGrid',
            url: OTIMapService.gtfsStopsUrl('utfgrid'),
            visible: false,
            pluginOptions: {
                'useJsonP': false,
                modes: OTIMapService.getTransitModes(),
                scenario: OTIMapService.getScenario()
            }
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
                case legendNames.absoluteJobs:
                    $scope.leaflet.absoluteJobsLegend.show = legendState;
                    break;
                case legendNames.percentageJobs:
                    $scope.leaflet.percentageJobsLegend.show = legendState;
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

    /**
     * Helper for making a legend
     * Retrieves the min/max range, constructs the legend, and assigns it to the leaflet variable
     *
     * @param getRangeName: String, OTIMapService function name to call for retrieving the range
     * @param legendName: String, Name of variable to set on leaflet object
     * @param color: String, Color of legend -- ending in a number which denotes the number of bins
     * @param title: String, Translatable identifier for use in displaying the legend title
     */
    function makeLegend(getRangeName, legendName, color, title) {
        // Start off with "Fewer/More", just in case ranges can't be fetched
        $scope.leaflet[legendName] = OTIMapStyleService.getDemographicLegend(color, {
            show: false,
            title: $translate.instant(title),
            range: {
                min: legendNames.fewer,
                max: legendNames.more
            }
        });

        OTIMapService[getRangeName](layerOptions.calculation_job).then(function(range) {
            $scope.leaflet[legendName].range = range;
        }, function(err) {
            console.error('Error fetching ranges for: ' + legendName + ' -- ', err.data.error);
        });
    }

    $scope.init = function () {
        if($scope.cities.length > 0) {
            if (!$scope.selectedCity) {
                $scope.selectedCity = $scope.cities[0];
            }
            $scope.selectCity($scope.selectedCity);
        }

        // only show current city and its scenarios on map page drop-down
        OTISettingsService.cityName.get({}, function (data) {
            var cityName = data.city_name;
            $scope.current_scenarios = _.filter($scope.cities, function(city) {
                return (city.city_name === cityName || city.scenario !== null);
            });
        });

        updateIndicatorLegend($scope.indicator);
        OTIMapService.getLegendData();

        // Set up demographic legends (static)
        makeLegend('getJobsTravelShedRange', 'jobsLegend',
                'green8', 'MAP.JOBS_INDICATOR_TITLE');
        makeLegend('getAbsoluteJobsTravelShedRange', 'absoluteJobsLegend',
                'green8', 'MAP.ABSOLUTE_JOBS_INDICATOR_TITLE');
        makeLegend('getPercentageJobsTravelShedRange', 'percentageJobsLegend',
                'green8', 'MAP.PERCENTAGE_JOBS_INDICATOR_TITLE');
        makeLegend('getPopOneRange', 'pop1Legend', 'red5', 'MAP.POPULATION_METRIC_ONE_LAYER');
        makeLegend('getPopTwoRange', 'pop2Legend', 'orange5', 'MAP.POPULATION_METRIC_TWO_LAYER');
        makeLegend('getDestOneRange', 'dest1Legend', 'blue5', 'MAP.DESTINATION_METRIC_LAYER');
    };

    $scope.$on(OTICityManager.Events.CitiesUpdated, function () {
        $scope.init();
    });

    $scope.init();
}]);
