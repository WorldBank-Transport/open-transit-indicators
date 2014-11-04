'use strict';

angular.module('transitIndicators')
.factory('OTIIndicatorsDataService',
        ['OTIIndicatorsService', 'OTIMapStyleService',
        function (OTIIndicatorsService, OTIMapStyleService) {

    var routeTypes = {};
    OTIIndicatorsService.getRouteTypes().then(function (data) {
        _.each(data, function (routeType) {
            routeTypes[routeType.route_type] = routeType.description;
        });
    });

    var getRouteTypeLabel = function (routeType) {
        if (!routeTypes) {
            return routeType;
        }

        var label = routeTypes[routeType] || routeType;
        return label;

    };

    var routeTypeColorRamp = OTIMapStyleService.routeTypeColorRamp();
    var defaultColor = OTIMapStyleService.defaultColor;

    var otiDataService = {};

    /// DEFAULT CHART FUNCTIONS
    var defaultTooltipFunction = function () {
        return function (key, x, y) {
            return '<h3>' + getRouteTypeLabel(key) + '</h3><p>' + y + '</p>';
        };
    };
    var defaultXFunctionMode = function () {
        return function (data) {
            return data.route_type;
        };
    };
    var defaultXFunctionRoute = function () {
        return function (data) {
            return data.route_id;
        };
    };
    var defaultYFunction = function () {
        return function (data) {
            return data.value;
        };
    };
    var xFunctionZero = function () {
        return function () {
            return 0;
        };
    };
    var defaultFilterFunction = function (citydata, aggregation) {
        return citydata[aggregation];
    };
    var defaultForceYFunction = function (data, type, aggregation) {
        return Math.ceil(data[type][aggregation].max);
    };

    var multiBarFilterFunction = function (citydata, aggregation) {
        if (!(citydata && citydata[aggregation])) {
            return null;
        }
        var tempdata = {};
        _.each(citydata[aggregation][0].values, function (value) {
            if (!tempdata[value.route_type]) {
                tempdata[value.route_type] = [];
            }
            tempdata[value.route_type].push(_.extend({}, value));
        });

        var transformed = [];
        _.each(tempdata, function (value, key) {
            transformed.push({
                key: key,
                values: value
            });
        });
        return transformed;
    };

    // Chart configuration
    otiDataService.Charts = {
        pie: {
            xFunctionMode: defaultXFunctionMode,
            xFunctionRoute: defaultXFunctionRoute,
            yFunction: defaultYFunction,
            tooltipFunction: function () {
                return function (key, x, y) {
                    return '<h3>' + getRouteTypeLabel(key) + '</h3><p>' + y.value + '</p>';
                };
            },
            filterFunction: function (citydata, aggregation) {
                if (citydata && citydata[aggregation]) {
                    return citydata[aggregation][0].values;
                }
                return null;
            },
            colorFunction: function () {
                return function (data) {
                    return routeTypeColorRamp[data.data.route_type] || defaultColor;
                };
            }
        },
        bar: {
            xFunctionMode: defaultXFunctionMode,
            xFunctionRoute: defaultXFunctionRoute,
            yFunction: defaultYFunction,
            forceYFunction: defaultForceYFunction,
            filterFunction: defaultFilterFunction,
            xAxisTickFormatFunction: function () {
                return function (x) {
                    var label = getRouteTypeLabel(x);
                    if (label.length > 6) {
                        label = label.substr(0, 6) + '...';
                    }
                    return label;
                };
            },
            colorFunction: function () {
                return function (data) {
                    return routeTypeColorRamp[data.route_type] || defaultColor;
                };
            }
        },
        stacked: {
            xFunctionMode: xFunctionZero,
            xFunctionRoute: xFunctionZero,
            yFunction: defaultYFunction,
            forceYFunction: defaultForceYFunction,
            colorFunction: function () {
                return function (data) {
                    return routeTypeColorRamp[data.key] || defaultColor;
                };
            },
            filterFunction: multiBarFilterFunction,
            tooltipFunction: defaultTooltipFunction
        },
        horizontal: {
            xFunctionMode: xFunctionZero,
            xFunctionRoute: xFunctionZero,
            yFunction: defaultYFunction,
            forceYFunction: defaultForceYFunction,
            filterFunction: multiBarFilterFunction,
            colorFunction: function () {
                return function (data) {
                    return routeTypeColorRamp[data.key] || defaultColor;
                };
            },
            tooltipFunction: defaultTooltipFunction
        }
    };

    otiDataService.IndicatorConfig = {
        'affordability': {
            'mode': 'bar',
            'system': 'number'
        },
        'avg_service_freq': {
            'mode': 'bar',
            'system': 'number'
        },
        'coverage_ratio_stops_buffer': {
            'system': 'number'
        },
        'distance_stops': {
            'mode': 'bar',
            'system': 'number'
        },
        'dwell_time': {
            'mode': 'bar',
            'system': 'number'
        },
        'hours_service': {
            'mode': 'bar',
            'system': 'number'
        },
        'length': {
            'mode': 'horizontal',
            'system': 'number'
        },
        'line_network_density': {
            'system': 'number'
        },
        'lines_roads': {
            'system': 'number'
        },
        'num_routes': {
            'mode': 'pie',
            'system': 'number'
        },
        'num_stops': {
            'mode': 'pie',
            'system': 'number'
        },
        'on_time_perf': {
            'mode': 'bar',
            'system': 'number'
        },
        'ratio_suburban_lines': {
            'system': 'number'
        },
        'regularity_headways': {
            'mode': 'bar',
            'system': 'number'
        },
        'service_freq_weighted': {
            'mode': 'bar',
            'system': 'number'
        },
        'service_freq_weighted_low': {
            'mode': 'bar',
            'system': 'number'
        },
        'stops_route_length': {
            'mode': 'bar',
            'system': 'number'
        },
        'system_access': {
            'mode': 'bar',
            'system': 'number'
        },
        'system_access_low': {
            'mode': 'bar',
            'system': 'number'
        },
        'time_traveled_stops': {
            'mode': 'bar',
            'system': 'number'
        },
        'travel_time': {
            'mode': 'bar',
            'system': 'number'
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
                    key: '<route_type|route_id>',
                    values: [OTIIndicatorService.Indicator]
                }]
            }
        }
    }
}

     * @param source data structure defined above
     * @return dest data structure defined above
     */
    otiDataService.transformData = function (data, cities) {
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

    otiDataService.getChartTypeForIndicator = function (type) {
        var config = otiDataService.IndicatorConfig;
        var chartType = config && config[type] && config[type].mode ? config[type].mode : 'nodata';
        return chartType;
    };

    return otiDataService;
}]);
