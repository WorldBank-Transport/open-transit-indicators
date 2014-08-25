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

    $scope.aggregations = {};
    $scope.types = {};
    $scope.sample_periods = {};

    OTIIndicatorsService.getIndicatorTypes().then(function (data) {
        $scope.types = data;
    });
    OTIIndicatorsService.getIndicatorAggregationTypes().then(function (data) {
        $scope.aggregations = data;
    });
    OTIIndicatorsService.getSamplePeriodTypes().then(function (data) {
        $scope.sample_periods = data;
    });

    $scope.selectType = function (type) {
        $scope.indicator.type = type;
        $scope.dropdown_type_open = false;
        $scope.$broadcast(OTIIndicatorsService.Events.IndicatorUpdated, $scope.indicator);
    };

    $scope.selectAggregation = function (aggregation) {
        $scope.indicator.aggregation = aggregation;
        $scope.dropdown_aggregation_open = false;
        $scope.$broadcast(OTIIndicatorsService.Events.IndicatorUpdated, $scope.indicator);
    };

    $scope.selectSamplePeriod = function (sample_period) {
        $scope.indicator.sample_period = sample_period;
        $scope.dropdown_sample_period_open = false;
        $scope.$broadcast(OTIIndicatorsService.Events.IndicatorUpdated, $scope.indicator);
    };

    $scope.selectType = function (type) {
        $scope.indicator.type = type;
        $scope.dropdown_type_open = false;
        $scope.$broadcast(OTIIndicatorsService.Events.IndicatorUpdated, $scope.indicator);
    };

    $scope.$on('$stateChangeSuccess', function (event, toState) {
        $scope.mapActive = toState.name === 'map' ? true : false;
    });
}]);