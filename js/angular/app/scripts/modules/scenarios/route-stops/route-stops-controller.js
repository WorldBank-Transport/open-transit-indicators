'use strict';

/* global L */

angular.module('transitIndicators')
.controller('OTIScenariosRoutestopsController',
            ['config', '$scope', '$state', '$stateParams', 'OTIRouteManager', 'OTITripManager', 'leafletData', 'OTIDrawService', 'OTIStopService',
            function (config, $scope, $state, $stateParams, OTIRouteManager, OTITripManager, leafletData, OTIDrawService, OTIStopService) {

    // TODO: Click existing utfgrid stop to add to route
    // TODO: Refactor and cleanup marker add logic?

    // LOCAL

    var layerHash = {};

    var drawControl = OTIDrawService.markerDrawControl;
    var markerController = OTIDrawService.markerController;

    var someEvent = function (event) {
        console.log(event);
    };


    // $SCOPE

    $scope.route = OTIRouteManager.get();
    $scope.trip = OTITripManager.get();

    $scope.continue = function () {
        $state.go('route-shapes');
    };

    $scope.back = function () {
        $state.go('route-edit');
    };

    $scope.deleteStop = function (stopIndex) {
        OTITripManager.removeStopTime(stopIndex);
    };

    $scope.$on('$stateChangeStart', function () {
        leafletData.getMap().then(function (map) {
            map.removeControl(drawControl);
            map.off('draw:created', OTITripManager.drawCreated);
            map.off('click', someEvent);
        });
    });

    $scope.$watch('route.stops', function () {
        $scope.$emit('updateHeight');
    }, true);


    // INIT
    OTITripManager.mapStops();
    leafletData.getMap().then(function (map) {
        map.addControl(OTIDrawService.markerDrawControl);
        map.on('draw:created', OTITripManager.drawCreated);
    });

}]);
