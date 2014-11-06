'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosRouteeditController',
            ['config', '$filter', '$scope', '$state', '$stateParams', 'OTIRouteManager', 'OTITripManager', 'OTIDrawService',
            function (config, $filter, $scope, $state, $stateParams, OTIRouteManager, OTITripManager, OTIDrawService) {

    var DEFAULT_HEADWAY_MINS = 10;

    var setTrip = function (trip) {
        $scope.trip = trip;
        if (trip.getFrequency) {
            var headwaySeconds = $scope.trip.getFrequency(0).headway;
            $scope.headwayMins = Math.floor(headwaySeconds / 60);
        }
    };

    $scope.continue = function () {
        if ($scope.newRoute.$valid) {
            OTIRouteManager.set($scope.route);
            OTITripManager.set($scope.trip);
            $state.go('route-stops');
        }
    };

    $scope.back = function () {
        OTIRouteManager.clear();
        $state.go('routes');
    };

    $scope.getTrip = function () {
        OTITripManager.retrieve($scope.selectedTripId).then(function (trip) {
            setTrip(trip);
        });
    };

    $scope.setHeadway = function () {
        var frequency = $scope.trip.getFrequency(0);
        frequency.headway = Math.floor($scope.headwayMins* 60);
        $scope.trip.addFrequency(frequency);
    };

    var initialize = function () {
        $scope.selectedRouteType = 0;
        $scope.route = OTIRouteManager.get();
        $scope.selectedTripId = '';
        $scope.headwayMins = DEFAULT_HEADWAY_MINS;
        $scope.trips = [];

        $scope.isRouteNew = OTIRouteManager.isNew();
        if ($scope.isRouteNew) {
            // TODO: Set frequency start/end times == scenario sample period on create
            $scope.trip = OTITripManager.create();
        } else {
            $scope.trip = OTITripManager.get();
            OTITripManager.setRouteId($scope.route.id);
            OTITripManager.list().then(function (trips) {
                $scope.trips = trips;
            });
        }

        OTIDrawService.reset();
    };

    initialize();
}]);
