'use strict';
angular.module('transitIndicators')
.controller('OTIIndicatorsDataController',
            ['$scope', 'OTIEvents', 'OTIIndicatorsService', 'OTIIndicatorsDataService',
            function ($scope, OTIEvents, OTIIndicatorsService, OTIIndicatorsDataService) {

    var cache = {};
    $scope.updating = false;

    var defaultTooltipFunction = function () {
        return function (key, x, y) {
            return '<h3>' + key + '</h3><p>' + y.formatted_value + '</p>';
        };
    };

    $scope.charts = {
        pie: {
            xFunctionMode: function () {
                return function (data) {
                    return data.route_type;
                };
            },
            xFunctionRoute: function () {
                return function (data) {
                    return data.route_id;
                };
            },
            yFunction: function () {
                return function (data) {
                    return data.value;
                };
            },
            tooltipFunction: defaultTooltipFunction
        },
        bar: {
            xFunctionMode: function () {
                return function (data) {
                    return data.route_type;
                };
            },
            yFunction: function () {
                return function (data) {
                    return data.value;
                };
            },
            forceYFunction: function (type, aggregation) {
                return Math.ceil($scope.indicatorData[type][aggregation].max);
            }
        }
    };

    /**
     * Transforms the /api/indicators/ response into something that we can use in the graphs/table
     * source data structure:

[OTIIndicatorService.Indicator]

     * dest data structure:

{
    "<type>": {
        "<aggregation>": {
            max: <number>,
            min: <number>
        },
        "cities": {
            "<city_name>": {
                "<aggregation>": [{
                    key: '<aggregation>',
                    values: [OTIIndicatorService.Indicator]
                }]
            }
        }
    }
}

     * @param source data structure defined above
     * @return dest data structure defined above
     */
    var transformData = function (data, cities) {
        var transformed = {};
        _.each(data, function (indicator) {
            if (!transformed[indicator.type]) {
                transformed[indicator.type] = {
                    cities: {}
                };
            }

            // Calculate max/min for each indicator type
            if (!transformed[indicator.type].cities[indicator.city_name]) {
                var indicatorCities = {};
                // The cities must be set in this object, even if there is no data for that indicator,
                //  so that we can loop them in the template. If we loop in the template via
                //  $scope.cities rather than this object, we lose the 2-way binding and updates
                //  to the indicatorData object no longer update the view.
                _.each(cities, function (city) {
                    indicatorCities[city] = {};
                });
                transformed[indicator.type].cities = indicatorCities;
            }

            // Set the indicator into it's proper location
            if (!transformed[indicator.type].cities[indicator.city_name][indicator.aggregation]) {
                transformed[indicator.type].cities[indicator.city_name][indicator.aggregation] = [{
                    key: indicator.aggregation,
                    values: []
                }];
            }
            transformed[indicator.type].cities[indicator.city_name][indicator.aggregation][0].values.push(indicator);

            // Calculate min/max values of indicator type/aggregation so that
            //  we can properly scale the graphs
            if (!transformed[indicator.type][indicator.aggregation]) {
                transformed[indicator.type][indicator.aggregation] = {
                    max: Number.NEGATIVE_INFINITY,
                    min: Number.POSITIVE_INFINITY
                };
            }
            var minmax = transformed[indicator.type][indicator.aggregation];
            if (indicator.value > minmax.max) {
                minmax.max = indicator.value;
            }
            if (indicator.value < minmax.min) {
                minmax.min = indicator.value;
            }
        });
        return transformed;
    };

    var getIndicatorData = function () {
        $scope.updating = true;
        var period = $scope.sample_period;
        if (period) {
            var params = {
                sample_period: period
            };

            if (cache && cache[period]) {
                $scope.indicatorData = cache[period];
                $scope.updating = false;
            } else {
                OTIIndicatorsService.query('GET', params).then(function (data) {
                    var indicators = transformData(data, $scope.cities);
                    $scope.indicatorData = null;
                    $scope.indicatorData = indicators;
                    cache[period] = indicators;
                    $scope.updating = false;
                }, function (error) {
                    console.error('Error getting indicator data:', error);
                    $scope.updating = false;
                });
            }
        }
    };

    $scope.displayIndicator = function (type, aggregation) {
        var config = $scope.indicatorConfig;
        var display = !!(config && config[type] && config[type][aggregation]);
        return display;
    };

    $scope.getModePartialForIndicator = function (type) {
        var config = $scope.indicatorConfig;
        var chartType = config && config[type] && config[type].mode ? config[type].mode : 'nodata';
        var url = '/scripts/modules/indicators/charts/:charttype-mode-partial.html';
        return url.replace(':charttype', chartType);
    };

    $scope.indicatorConfig = OTIIndicatorsDataService.IndicatorConfig;

    $scope.getIndicatorDescriptionTranslationKey = function(key) {
        return 'INDICATOR_DESCRIPTION.' + key;
    };

    $scope.$on(OTIEvents.Indicators.SamplePeriodUpdated, function () {
        getIndicatorData();
    });

    $scope.$on(OTIEvents.Indicators.CitiesUpdated, function () {
        cache = {};
        getIndicatorData();
    });

    getIndicatorData();
}]);