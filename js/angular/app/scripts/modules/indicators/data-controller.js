'use strict';
angular.module('transitIndicators')
.controller('OTIIndicatorsDataController',
            ['$scope', 'OTIEvents', 'OTIIndicatorsService',
            function ($scope, OTIEvents, OTIIndicatorsService) {

    var cache = {};
    $scope.cities = [];

    var updating = false;

    $scope.routeXFunction = function () {
        return function (data) {
            return data.route_id;
        };
    };

    $scope.modeXFunction = function () {
        return function (data) {
            return data.route_type;
        };
    };

    $scope.yFunction = function () {
        return function (data) {
            return data.value;
        };
    };

    $scope.tooltipFunction = function () {
        return function (key, x, y) {
            return '<h3>' + key + '</h3><p>' + y.formatted_value + '</p>';
        };
    };

    /**
     * Transforms the /api/indicators/ response into something that we can use in the graphs/table
     * source data structure:

[OTIIndicatorService.Indicator]

     * dest data structure:

{
    "<type>": {
        "<city_name>": {
            "<aggregation>": [OTIIndicatorService.Indicator]
        }
    }
}

     * @param source data structure defined above
     * @return dest data structure defined above
     */
    var transformData = function (data) {
        var transformed = {};
        _.each(data, function (indicator) {
            if (!transformed[indicator.type]) {
                transformed[indicator.type] = {};
            }
            if (!transformed[indicator.type][indicator.city_name]) {
                transformed[indicator.type][indicator.city_name] = {};
            }
            if (!transformed[indicator.type][indicator.city_name][indicator.aggregation]) {
                transformed[indicator.type][indicator.city_name][indicator.aggregation] = [];
            }
            transformed[indicator.type][indicator.city_name][indicator.aggregation].push(indicator);
        });
        return transformed;
    };

    /**
     * Get a unique list of String city names
     *
     * @param source data structure, of type [OTIIndicatorService.Indicator]
     * @return unique array of city names
     */
    var setCities = function (data) {
        return _.chain(data).groupBy('city_name').keys().value();
    };

    var getIndicatorData = function () {
        updating = true;
        var version = $scope.indicatorVersion;
        var period = $scope.sample_period;
        if (version && period) {
            var params = {
                version: version,
                sample_period: period
            };

            if (!cache[version]) {
                cache[version] = {};
            }
            if (cache && cache[version] && cache[version][period]) {
                $scope.indicatorData = cache[version][period];
                updating = false;
            } else {
                OTIIndicatorsService.search(params).then(function (data) {
                    var indicators = transformData(data);
                    $scope.cities = setCities(data);
                    $scope.indicatorData = indicators;
                    cache[version][period] = indicators;
                    updating = false;
                }, function (error) {
                    console.error('Error getting indicator data:', error);
                    updating = false;
                });
            }
        }
    };

    $scope.getIndicatorDescriptionTranslationKey = function(key) {
        return 'INDICATOR_DESCRIPTION.' + key;
    };

    $scope.$on(OTIEvents.Indicators.SamplePeriodUpdated, function () {
        getIndicatorData();
    });

    $scope.$on(OTIEvents.Indicators.IndicatorVersionUpdated, function () {
        getIndicatorData();
    });

    getIndicatorData();
}]);