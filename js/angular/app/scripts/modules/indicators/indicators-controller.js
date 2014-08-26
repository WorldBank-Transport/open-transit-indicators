'use strict';
angular.module('transitIndicators')
.controller('OTIIndicatorsController',
            ['$scope', '$cookieStore', 'OTIIndicatorsService',
            function ($scope, $cookieStore, OTIIndicatorsService) {

    $scope.dropdown_sample_period_open = false;

    $scope.aggregations = {};
    $scope.types = {};
    $scope.sample_periods = {};
    $scope.sample_period = $cookieStore.get('sample_period') || 'morning';

    OTIIndicatorsService.getIndicatorTypes().then(function (data) {
        $scope.types = data;
    });
    OTIIndicatorsService.getIndicatorAggregationTypes().then(function (data) {
        $scope.aggregations = data;
    });
    OTIIndicatorsService.getSamplePeriodTypes().then(function (data) {
        $scope.sample_periods = data;
    });

    $scope.selectSamplePeriod = function (sample_period) {
        $scope.dropdown_sample_period_open = false;
        $scope.sample_period = sample_period;
        $cookieStore.put('sample_period', sample_period);
        $scope.$broadcast(OTIIndicatorsService.Events.SamplePeriodUpdated, sample_period);
    };

    $scope.$on('$stateChangeSuccess', function (event, toState) {
        $scope.mapActive = toState.name === 'map' ? true : false;
    });
}]);