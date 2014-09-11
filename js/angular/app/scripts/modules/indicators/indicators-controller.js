'use strict';
angular.module('transitIndicators')
.controller('OTIIndicatorsController',
            ['$scope', '$cookieStore', 'OTIEvents', 'OTIIndicatorsService',
            function ($scope, $cookieStore, OTIEvents, OTIIndicatorsService) {

    $scope.dropdown_sample_period_open = false;
    $scope.indicatorVersion = 0;

    $scope.aggregations = {};
    $scope.types = {};
    $scope.sample_periods = {};
    $scope.sample_period = $cookieStore.get('sample_period') || 'morning';

    var setIndicatorVersion = function (version) {
        $scope.indicatorVersion = version;
        $scope.$broadcast(OTIEvents.Indicators.IndicatorVersionUpdated, version);
    };

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
        $scope.$broadcast(OTIEvents.Indicators.SamplePeriodUpdated, sample_period);
    };

    $scope.$on('$stateChangeSuccess', function (event, toState) {
        $scope.showingMap = toState.name === 'map' ? true : false;
    });

    $scope.init = function () {
        OTIIndicatorsService.getIndicatorVersion(function (version) {
            setIndicatorVersion(version);
        });
    };
    $scope.init();
}]);