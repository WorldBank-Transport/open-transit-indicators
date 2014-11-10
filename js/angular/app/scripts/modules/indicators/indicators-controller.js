'use strict';

angular.module('transitIndicators')
.controller('OTIIndicatorsController',
            ['$scope', '$cookieStore', '$modal',
             'OTIEvents', 'OTIIndicatorManager', 'OTIIndicatorJobManager', 'OTITypes',
             'cities',
            function ($scope, $cookieStore, $modal,
                      OTIEvents, OTIIndicatorManager, OTIIndicatorJobManager, OTITypes,
                      cities) {

    $scope.dropdown_sample_period_open = false;
    $scope.indicatorCalcJob = 0;

    $scope.aggregations = {};
    $scope.types = {};
    $scope.sample_periods = {};
    $scope.sample_period = OTIIndicatorManager.getSamplePeriod();

    $scope.cities = cities;
    $scope.showingState = 'data';

    OTITypes.getIndicatorTypes().then(function (data) {
        // filter indicator types to only show those for map display
        $scope.types = {};
        $scope.mapTypes = {};
        _.each(data, function(obj, key) {
            var name = obj.display_name;
            $scope.types[key] = name;
            if (obj.display_on_map) {
                $scope.mapTypes[key] = name;
            }
        });
    });
    OTITypes.getIndicatorAggregationTypes().then(function (data) {
        $scope.aggregations = data;
    });
    OTITypes.getSamplePeriodTypes().then(function (data) {
        $scope.sample_periods = data;
    });

    $scope.openCityModal = function () {
        var modalCities = $scope.cities;
        OTIIndicatorManager.setModalStatus(true);
        $modal.open({
            templateUrl: 'scripts/modules/indicators/city-modal-partial.html',
            controller: 'OTICityModalController',
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
        }).result.finally(function () {
            OTIIndicatorManager.setModalStatus(false);
        });
    };

    $scope.selectSamplePeriod = function (sample_period) {
        $scope.dropdown_sample_period_open = false;
        $scope.sample_period = sample_period;
        OTIIndicatorManager.setSamplePeriod(sample_period);
    };

    $scope.$on('$stateChangeSuccess', function (event, toState) {
        $scope.showingState = toState.name;
    });

    $scope.init = function () {
        OTIIndicatorJobManager.getCurrentJob(function (calcJob) {
            $scope.indicatorCalcJob = calcJob;
        });
    };
    $scope.init();
}]);
