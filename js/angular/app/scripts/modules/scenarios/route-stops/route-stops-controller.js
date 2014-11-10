'use strict';

/* global L */

angular.module('transitIndicators')
.controller('OTIScenariosRoutestopsController',
            ['config', '$scope', '$state', '$stateParams', 'OTIRouteManager', 'OTITripManager', 'leafletData', 'OTIDrawService', 'OTIStopService', '$compile',
            function (config, $scope, $state, $stateParams, OTIRouteManager, OTITripManager, leafletData, OTIDrawService, OTIStopService, $compile) {

    // TODO: Click existing utfgrid stop to add to route
    // TODO: Refactor and cleanup marker add logic?

    // LOCAL

    var layerHash = {};

    var drawControl = OTIDrawService.markerDrawControl;
    var markerController = OTIDrawService.markerController;


    // $SCOPE

    $scope.route = OTIRouteManager.get();
    $scope.trip = OTITripManager.get();

    $scope.continue = function () {
        $state.go('route-shapes');
    };

    $scope.back = function () {
        $state.go('route-edit');
    };

    $scope.removeStopTime = function (stopIndex) {
        OTITripManager.removeStopTime(stopIndex);
        mapStops();
    };

    $scope.$on('$stateChangeStart', function (e) {
        leafletData.getMap().then(function (map) {
            OTIDrawService.reset();
            map.removeControl(drawControl);
            map.off('draw:created', drawCreated);
        });
    });

    $scope.$watch('route.stops', function () {
        $scope.$emit('updateHeight');
    }, true);

    // INIT

    // Get trips for a given db_name, routeId, tripId
    //  Set queryParams db_name, routeId globally via class methods.
    //  Can override db_name, routeId defaults in queryParams
    leafletData.getMap().then(function (map) {
        map.addControl(OTIDrawService.markerDrawControl);
        map.on('draw:created', drawCreated);
    });

    // All mapping functions for stops live here. If they can be moved to a service: awesome.
    // I, unfortunately, haven't been able to do this the clean, 'correct' way and am going to
    // leave this problem for posterity. TODO: break this logic out so that scope
    // isn't so polluted.

    // Add a stop and attach directive
    var addStopToMap = function(stopTime) {
        var e = $compile('<stopup stop-time="stopTime" oti-trip="$scope.trip"></stopup>')($scope);
        var marker = new L.Marker([stopTime.stop.lat, stopTime.stop.long], {
           icon: OTIDrawService.getCircleIcon(stopTime.stopSequence.toString())
        });
        marker.on('click', function() { $scope.stopTime = stopTime; });
        marker.bindPopup(e[0]);

        OTIDrawService.drawnItems.addLayer(marker);
    };

    // Map stops
    var mapStops = function() {
        OTIDrawService.reset();
        leafletData.getMap().then(function (map) {

            _.each($scope.trip.stopTimes, function (stopTime) {
                addStopToMap(stopTime);
            });
            map.addLayer(OTIDrawService.drawnItems);
        });
    };

    // Draw a new stop and register the lat/long into a stop object which is appended to trip
    var drawCreated = function (event) {
        var type = event.layerType,
            layer = event.layer;
        if (type === 'marker') {
            var stopTime = OTIStopService.stopTimeFromLayer(layer);
            stopTime.stopName = 'SimulatedStop-' + $scope.trip.stopTimes.length;
            $scope.trip.addStopTime(stopTime);

            addStopToMap(stopTime);
        }
    };
    mapStops();
}]);
