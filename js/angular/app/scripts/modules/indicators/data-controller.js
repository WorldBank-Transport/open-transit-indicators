'use strict';
angular.module('transitIndicators')
.controller('OTIIndicatorsDataController',
            ['$scope', 'OTIEvents', 'OTIIndicatorsService',
            function ($scope, OTIEvents, OTIIndicatorsService) {

    var cache = {};
    $scope.updating = false;

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
    var transformData = function (data, cities) {
        var transformed = {};
        _.each(data, function (indicator) {
            if (!transformed[indicator.type]) {
                transformed[indicator.type] = {};
            }
            if (!transformed[indicator.type][indicator.city_name]) {
                var indicatorCities = {};
                // The cities must be set in this object, even if there is no data for that indicator,
                //  so that we can loop them in the template. If we loop in the template via
                //  $scope.cities rather than this object, we lose the 2-way binding and updates
                //  to the indicatorData object no longer update the view.
                _.each(cities, function (city) {
                    indicatorCities[city] = {};
                });
                transformed[indicator.type] = indicatorCities;
            }
            if (!transformed[indicator.type][indicator.city_name][indicator.aggregation]) {
                transformed[indicator.type][indicator.city_name][indicator.aggregation] = [];
            }
            transformed[indicator.type][indicator.city_name][indicator.aggregation].push(indicator);
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