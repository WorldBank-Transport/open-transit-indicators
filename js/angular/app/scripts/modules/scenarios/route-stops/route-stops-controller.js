'use strict';

/* global L */

angular.module('transitIndicators')
.controller('OTIScenariosRoutestopsController', [
            '$scope', '$state', '$stateParams', '$compile',
             'leafletData',
            'config', 'OTIMapService', 'OTIRouteManager', 'OTITripManager', 'OTIDrawService', 'OTIStopService',
            function ($scope, $state, $stateParams, $compile,
                      leafletData,
                      config, OTIMapService, OTIRouteManager, OTITripManager, OTIDrawService, OTIStopService) {

    // TODO: Click existing utfgrid stop to add to route
    // TODO: Refactor and cleanup marker add logic?

    // LOCAL

    var drawControl = OTIDrawService.markerDrawControl;

    var showStopAddPopup = function (stopTime) {
        leafletData.getMap().then(function (map) {
            var element = $compile('<stop-add stop-time="stopTime"></stop-add>')($scope);
            $scope.stopTime = stopTime;
            L.popup({
                minWidth: 100
            }).setLatLng([stopTime.stop.lat, stopTime.stop.long])
            .setContent(element[0])
            .openOn(map);
        });
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

    $scope.removeStopTime = function (stopIndex) {
        OTITripManager.removeStopTime(stopIndex);
        mapStops();
    };

    $scope.addStopTime = function (stopTime) {
        $scope.trip.addStopTime(stopTime);
        addStopToMap(stopTime);
        OTIMapService.closePopup();
    };

    $scope.changeSequence = function(stopIndex, newPosition) {
        $scope.trip.changeSequence(stopIndex-1, newPosition);
        mapStops();
    };

    $scope.closePopup = OTIMapService.closePopup;

    $scope.$on('$stateChangeStart', function () {
        leafletData.getMap().then(function (map) {
            OTIDrawService.reset();
            map.removeControl(drawControl);
            map.off('draw:created', drawCreated);
        });
    });

    $scope.$on('leafletDirectiveMap.utfgridClick', function (event, leafletEvent) {
        if (!leafletEvent.data) {
            return;
        }
        var text = leafletEvent.data.stop_routes;
        var latLng = leafletEvent.latlng;
        var match = text.match(/<strong>(.*)<\/strong>/i);
        var stopName = match.length > 0 ? match[1] : '';
        var stopTime = new OTIStopService.StopTime();
        stopTime.stop.name = stopName;
        stopTime.stop.lat = latLng.lat;
        stopTime.stop.long = latLng.lng;
        showStopAddPopup(stopTime);
    });

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
        var e = $compile('<stopup stop-time="stopTime"></stopup>')($scope);
        var marker = new L.Marker([stopTime.stop.lat, stopTime.stop.long], {
           icon: OTIDrawService.getCircleIcon(stopTime.stopSequence.toString()),
           draggable: true
        });
        marker.on('click', function() { $scope.stopTime = stopTime; });
        marker.on('click', function() { $scope.currentStopPosition = stopTime.stopSequence; });
        marker.on('dragstart', function() { $scope.stopTime = stopTime; });
        marker.on('dragend', function() { $scope.trip.changeCoords(stopTime.stopSequence-1, marker.getLatLng()); });
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
