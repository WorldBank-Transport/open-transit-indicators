'use strict';
angular.module('transitIndicators')
.controller('OTIIndicatorsController',
            ['$scope', 'OTIIndicatorsService',
            function ($scope, OTIIndicatorsService) {

    $scope.indicator = new OTIIndicatorsService.IndicatorConfig({
        version: 0,
        type: 'num_stops',
        sample_period: 'morning',
        aggregation: 'route'
    });

    $scope.dropdown_aggregation_open = false;
    $scope.dropdown_type_open = false;
    $scope.dropdown_sample_period_open = false;

    $scope.aggregations = OTIIndicatorsService.aggregations;
    $scope.types = OTIIndicatorsService.types;
    $scope.sample_periods = OTIIndicatorsService.sample_periods;

    $scope.selectType = function (type) {
        $scope.indicator.type = type;
        $scope.dropdown_type_open = false;
    };

    $scope.selectAggregation = function (aggregation) {
        $scope.indicator.aggregation = aggregation;
        $scope.dropdown_aggregation_open = false;
    };

    $scope.selectSamplePeriod = function (sample_period) {
        $scope.indicator.sample_period = sample_period;
        $scope.dropdown_sample_period_open = false;
    };

    $scope.selectType = function (type) {
        $scope.indicator.type = type;
        $scope.dropdown_type_open = false;
    };

    $scope.$on('$stateChangeSuccess', function (event, toState) {
        $scope.mapActive = toState.name === 'map' ? true : false;
    });
}]);