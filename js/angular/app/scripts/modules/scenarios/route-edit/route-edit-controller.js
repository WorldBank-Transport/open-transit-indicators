'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosRouteeditController',
            ['config', '$filter', '$scope', '$state', '$stateParams',
             'OTIRouteManager', 'OTIScenarioManager', 'OTITripManager', 'OTITypes', 'OTIDrawService',
            function (config, $filter, $scope, $state, $stateParams,
                      OTIRouteManager, OTIScenarioManager, OTITripManager, OTITypes, OTIDrawService) {

    var DEFAULT_HEADWAY_MINS = 10;
    var scenario = null;

    var setFrequency = function () {
        $scope.frequency = {
            start: '00:00:00',
            end: '23:59:00',
            headway: DEFAULT_HEADWAY_MINS * 60
        };
        OTITypes.SamplePeriod.get({
            'samplePeriod': scenario.sample_period
        }, function (data) {
            var start = new Date(data.period_start);
            var end = new Date(data.period_end);
            $scope.frequency.start = $filter('date')(start, 'HH:mm:ss');
            $scope.frequency.end = $filter('date')(end, 'HH:mm:ss');
        });
    };

    var setTrip = function (trip) {
        $scope.trip = trip;
        if (trip.addFrequency) {
            $scope.trip.addFrequency($scope.frequency);
        }
    };

    $scope.continue = function () {
        if ($scope.newRoute.$valid) {
            OTIRouteManager.set($scope.route);
            $scope.trip.addFrequency($scope.frequency);
            OTITripManager.set($scope.trip);

            // Create the route if it doesn't exist in the DB and create it
            if ($scope.route.isNew) {
                $scope.route.$create();
            } else {
                OTIRouteManager.update($scope.route);
            }
            $state.go('route-stops');
        }
    };

    $scope.back = function () {
        OTIRouteManager.clear();
        $state.go('routes');
    };

    $scope.getTrip = function () {
        if (!$scope.selected.tripId) {
            return;
        }
        OTITripManager.retrieve($scope.selected.tripId).then(function (trip) {
            setTrip(trip);
        });
    };

    $scope.setHeadway = function () {
        $scope.frequency.headway = $scope.selected.headwayMins * 60;
    };

    var initialize = function () {
        scenario = OTIScenarioManager.get();
        $scope.selectedRouteType = 0;
        $scope.route = OTIRouteManager.get();
        $scope.selected = {
            tripId: '',
            headwayMins: DEFAULT_HEADWAY_MINS
        };
        $scope.trips = [];

        setFrequency();

        // Assume new trip until user overrides with their own
        OTITripManager.setRouteId($scope.route.id);
        var trip = OTITripManager.create();
        OTITripManager.list().then(function (trips) {
            $scope.trips = trips;
            $scope.$emit('updateHeight');
        });
        setTrip(trip);
        OTITripManager.setScenarioDbName(scenario.db_name);
        OTIDrawService.reset();
    };

    initialize();
}]);
