'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosRouteeditController',
            ['config', '$scope', '$state', '$stateParams', 'OTIRouteManager', 'OTITripManager', 'OTIDrawService',
            function (config, $scope, $state, $stateParams, OTIRouteManager, OTITripManager, OTIDrawService) {

    $scope.selectedRouteType = 0;
    $scope.route = OTIRouteManager.get();
    $scope.selectedTripId = '';
    $scope.trips = [];
    $scope.trip = OTITripManager.get();

    OTITripManager.setRouteId($scope.route.id);
    OTITripManager.list().then(function (trips) {
        $scope.trips = trips;
    });

    OTIDrawService.reset();

    $scope.continue = function () {
        if ($scope.newRoute.$valid) {
            OTIRouteManager.set($scope.route);
            $state.go('route-stops');
        }
    };

    $scope.back = function () {
        OTIRouteManager.clear();
        $state.go('routes');
    };

    $scope.getTrip = function (tripId) {
        OTITripManager.retrieve($scope.selectedTripId).then(function (trip) {
            $scope.trip = trip;
            console.log(trip);
        });
    };

}]);
