'use strict';

/* global L */

angular.module('transitIndicators')
.controller('OTIScenariosRoutetimesController',
            ['config', '$scope', '$state', '$stateParams', 'leafletData',
             'OTIDrawService', 'OTIScenarioManager', 'OTITripManager',
            function (config, $scope, $state, $stateParams, leafletData,
                      OTIDrawService, OTIScenarioManager, OTITripManager) {

    // LOCAL

    var DEFAULT_HEADWAY_MINS = 10;

    var pad = function (number) {
        if ( number < 10 ) {
            return '0' + number;
        }
        return number;
    };

    var intialize = function () {

        var scenario = OTIScenarioManager.get();
        $scope.trip = OTITripManager.get();

        leafletData.getMap().then(function (map) {
            map.addLayer(OTIDrawService.drawnItems);
        });

        var samplePeriod = _.find($scope.samplePeriods, function (p) {
            return p.type === scenario.sample_period;
        });
        var periodStart = new Date(samplePeriod.period_start);
        _.each($scope.trip.stopTimes, function (stopTime, index) {
            if (!stopTime.arrivalTime) {
                var stopArrivalTime = new Date(periodStart.getTime() + 1000 * 60 * DEFAULT_HEADWAY_MINS * index);
                var stopArrivalString = pad(stopArrivalTime.getHours()) + ':' + pad(stopArrivalTime.getMinutes()) + ':00';
                stopTime.arrivalTime = stopArrivalString;
                stopTime.departureTime = stopArrivalString;
            }
        });
    };

    var setFrequencyStartEnd = function () {
        var frequency = $scope.trip.getFrequency(0);
        var stopTimes = $scope.trip.stopTimes;
        var firstStop = stopTimes[0];
        var lastStop = stopTimes[stopTimes.length - 1];
        frequency.start = firstStop.arrivalTime;
        frequency.end = lastStop.departureTime;
        $scope.trip.addFrequency(frequency);
    };

    $scope.updateHeight = function () {
        $scope.$emit('updateHeight');
    };

    // $SCOPE
    $scope.continue = function () {
        if ($scope.tripStopTimesForm.$valid) {
            setFrequencyStartEnd();
            $state.go('route-done');
            leafletData.getMap().then(function (map) {
                map.removeLayer(OTIDrawService.drawnItems);
            });
        }
    };

    $scope.back = function () {
        $state.go('route-shapes');
    };

    intialize();
}]);
