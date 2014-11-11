'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosRouteeditController',
            ['config', '$filter', '$scope', '$state', '$stateParams',
             'OTIRouteManager', 'OTIScenarioManager', 'OTITripManager', 'OTIDrawService',
            function (config, $filter, $scope, $state, $stateParams,
                      OTIRouteManager, OTIScenarioManager, OTITripManager, OTIDrawService) {

    var DEFAULT_HEADWAY_MINS = 10;

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
            }
            $state.go('route-stops');
        }
    };

    $scope.back = function () {
        OTIRouteManager.clear();
        $state.go('routes');
    };

    $scope.getTrip = function () {
        OTITripManager.retrieve($scope.selected.tripId).then(function (trip) {
            setTrip(trip);
        });
    };

    $scope.setHeadway = function () {
        $scope.frequency.headway = $scope.selected.headwayMins * 60;
    };

    var initialize = function () {
        $scope.selectedRouteType = 0;
        $scope.route = OTIRouteManager.get();
        $scope.selected = {
            tripId: '',
            headwayMins: DEFAULT_HEADWAY_MINS
        };
        $scope.trips = [];

        $scope.frequency = {
            start: '00:00:00',
            end: '23:59:00',
            headway: DEFAULT_HEADWAY_MINS * 60
        };

        // Assume new trip until user overrides with their own
        OTITripManager.setRouteId($scope.route.id);
        var trip = OTITripManager.create();
        OTITripManager.list().then(function (trips) {
            $scope.trips = trips;
            $scope.$emit('updateHeight');
        });
        setTrip(trip);
        var scenario = OTIScenarioManager.get();
        OTITripManager.setScenarioDbName(scenario.db_name);

        OTIDrawService.reset();
    };

    initialize();
}]);
