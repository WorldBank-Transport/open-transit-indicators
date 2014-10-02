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
        $scope.mapTypes = {};
        _.each(data, function(obj, key) {
            var name = obj.display_name;
            $scope.types[key] = name;
            if (obj.display_on_map) {
                $scope.mapTypes[key] = name;
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

    /**
     * Submits a job for calculating indicators
     */
    $scope.calculateIndicators = function () {
        var job = new OTIIndicatorsService.IndicatorJob({
            city_name: OTIIndicatorsService.selfCityName
        });
        job.$save().then(function (data) {
            // This alert is temporary. It will be switched to a
            // progress grid once status updates are available.
            alert('Calculation job started with id #' + data.id);
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
