'use strict';
angular.module('transitIndicators')
.controller('OTIIndicatorsController',
            ['$scope', '$cookieStore', 'OTIIndicatorsService',
            function ($scope, $cookieStore, OTIIndicatorsService) {

    var defaultIndicator = new OTIIndicatorsService.IndicatorConfig({
        version: 0,
        type: 'num_stops',
        sample_period: 'morning',
        aggregation: 'route'
    });
    // Object used to configure the indicator displayed on the map
    // Retrieve last selected indicator from session cookie, if available
    $scope.indicator = $cookieStore.get('indicator') || defaultIndicator;

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

    var setIndicator = function (indicator) {
        angular.extend($scope.indicator, indicator);
        $cookieStore.put('indicator', $scope.indicator);
        $scope.$broadcast(OTIIndicatorsService.Events.IndicatorUpdated, $scope.indicator);
    };

    $scope.selectType = function (type) {
        $scope.dropdown_type_open = false;
        setIndicator({type: type});
    };

    $scope.selectAggregation = function (aggregation) {
        $scope.dropdown_aggregation_open = false;
        setIndicator({aggregation: aggregation});
    };

    $scope.selectSamplePeriod = function (sample_period) {
        $scope.dropdown_sample_period_open = false;
        setIndicator({sample_period: sample_period});
    };

    $scope.$on('$stateChangeSuccess', function (event, toState) {
        $scope.mapActive = toState.name === 'map' ? true : false;
    });

    $scope.init = function () {
        OTIIndicatorsService.getIndicatorVersion(function (version) {
            setIndicator({version: version});
        });
    };
    $scope.init();
}]);