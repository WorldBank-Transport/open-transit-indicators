'use strict';

/* global L */

angular.module('transitIndicators')
.controller('OTIScenariosRoutetimesController',
            ['config', '$scope', '$state', '$stateParams', 'OTIScenariosService', 'leafletData', 'OTIDrawService',
            function (config, $scope, $state, $stateParams, OTIScenariosService, leafletData, OTIDrawService) {

    // LOCAL

    var pad = function (number) {
      if ( number < 10 ) {
        return '0' + number;
      }
      return number;
    };

    // $SCOPE

    $scope.scenario = OTIScenariosService.otiScenario;
    $scope.route = OTIScenariosService.otiRoute;

    $scope.continue = function () {
        $state.go('route-done');
        leafletData.getMap().then(function (map) {
            map.removeLayer(OTIDrawService.drawnItems);
        });
    };

    $scope.back = function () {
        $state.go('route-shapes');
    };


    // INIT

    leafletData.getMap().then(function (map) {
        map.addLayer(OTIDrawService.drawnItems);
    });

    // Set sample stop times if not already defined in route
    var samplePeriod = _.find($scope.samplePeriods, function (p) {
        return p.type === $scope.scenario.samplePeriod;
    });
    _.each($scope.route.stops, function (stop, index) {
        if (!stop.stopTime.arrivalTime) {
            var periodStart = new Date(samplePeriod.period_start);
            var stopArrivalTime = new Date(periodStart.getTime() + 1000 * 60 * $scope.route.headway * index);
            var stopArrivalString = pad(stopArrivalTime.getHours()) + ':' + pad(stopArrivalTime.getMinutes());
            stop.stopTime.arrivalTime = stopArrivalString;
            stop.stopTime.departureTime = stopArrivalString;
        }
    });

}]);
