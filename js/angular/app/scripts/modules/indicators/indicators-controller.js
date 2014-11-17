'use strict';

angular.module('transitIndicators')
.controller('OTIIndicatorsController',
            ['$scope', '$cookieStore', '$modal',
             'authService',
             'OTIEvents', 'OTIIndicatorManager', 'OTIIndicatorJobManager', 'OTITypes',
             'cities',
            function ($scope, $cookieStore, $modal,
                      authService,
                      OTIEvents, OTIIndicatorManager, OTIIndicatorJobManager, OTITypes,
                      cities) {

    $scope.dropdown_sample_period_open = false;

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

    var userScenarios = [];
    var otherScenarios = [];

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
                    return userScenarios;
                },
                otherScenarios: function () {
                    return otherScenarios;
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
            // TODO: Refactor getCurrentJob to return a promise so that we can resolve it in the state
            //          then set it here via setConfig,
            //          and remove the other getCurrentJob calls in the child controllers
            OTIIndicatorManager.setConfig({calculation_job: calcJob});
        });
        OTIIndicatorJobManager.getJobs().then(function (jobs) {
            jobs = _.chain(jobs)
                   .where({ job_status: 'complete' })
                   .groupBy(function (job) { return job.city_name; })
                   .values()
                   .map(function (jobs) {
                       return _.max(jobs, function (job) {
                           return job.id; // return most recent
                       });
                   }).value();
            console.log(jobs);
            userScenarios = _.where(jobs, function(job) {
                return job.created_by === $scope.user.id &&
                       job.scenario !== null;
            });
            console.log(userScenarios);
            otherScenarios = _.where(jobs, function(job) {
                return job.created_by !== $scope.user.id &&
                       job.scenario !== null;
            });
        });
    };
    $scope.init();
}]);
