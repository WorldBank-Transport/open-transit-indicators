'use strict';

angular.module('transitIndicators')
.controller('OTIIndicatorsController',
            ['$scope', '$cookieStore', '$modal', 'OTIEvents', 'OTIIndicatorsService', 'cities',
            function ($scope, $cookieStore, $modal, OTIEvents, OTIIndicatorsService, cities) {

    $scope.dropdown_sample_period_open = false;
    $scope.indicatorVersion = 0;

    $scope.aggregations = {};
    $scope.types = {};
    $scope.sample_periods = {};
    $scope.sample_period = $cookieStore.get('sample_period') || 'morning';

    $scope.cities = cities;

    var setIndicatorVersion = function (version) {
        $scope.indicatorVersion = version;
        $scope.$broadcast(OTIEvents.Indicators.IndicatorVersionUpdated, version);
    };

    OTIIndicatorsService.getIndicatorTypes().then(function (data) {
        // filter indicator types to only show those for map display
        $scope.types = {};
        _.each(data, function(obj, key) {
            if (obj.display_on_map == true) {
                $scope.types[key] = obj.display_name;
            }
        });
    });
    OTIIndicatorsService.getIndicatorAggregationTypes().then(function (data) {
        $scope.aggregations = data;
    });
    OTIIndicatorsService.getSamplePeriodTypes().then(function (data) {
        $scope.sample_periods = data;
    });

    $scope.openCityModal = function () {
        var modalCities = $scope.cities;
        var modalInstance = $modal.open({
            templateUrl: 'scripts/modules/indicators/city-modal-partial.html',
            controller: 'OTICityModalController',
            size: 'sm',
            windowClass: 'indicators-city-modal-window',
            resolve: {
                cities: function () {
                    return modalCities;
                },
                userScenarios: function () {
                    // TODO: Send user-defined scenarios here once scenarios are implemented
                    return [];
                },
                otherScenarios: function () {
                    // TODO: Send other user's scenarios here once scenarios are implemented
                    return [];
                }
            }
        });
    };

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
