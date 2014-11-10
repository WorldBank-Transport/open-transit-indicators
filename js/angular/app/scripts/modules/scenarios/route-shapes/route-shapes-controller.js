'use strict';

/* global L */

angular.module('transitIndicators')
.controller('OTIScenariosRouteshapesController',
            ['config', '$scope', '$state', '$stateParams', 'leafletData', 'OTIDrawService', 'OTITripManager',
            function (config, $scope, $state, $stateParams, leafletData, OTIDrawService, OTITripManager) {

    // TODO: Click existing utfgrid stop to add to route
    // TODO: Refactor and cleanup marker add logic?

    // LOCAL
    var layerCache = null;
    var drawControl = OTIDrawService.lineDrawControl;

    var drawCreated = function (event) {
        var layer = event.layer;
        OTIDrawService.drawnItems.addLayer(layer);
        var latLngs = _.map(layer._latlngs, function (latlng) { return [latlng.lat, latlng.lng]; });
        $scope.trip.makeShape(latLngs);
        layerCache = layer;
        mapTrip();
    };


    // $SCOPE

    $scope.trip = OTITripManager.get();

    $scope.clearShape = function () {
        $scope.trip.clearShape();
        mapTrip();
    };

    $scope.continue = function () {
        $state.go('route-times');
    };

    $scope.back = function () {
        $state.go('route-stops');
    };

    $scope.$on('$stateChangeStart', function () {
        leafletData.getMap().then(function (map) {
            map.removeControl(drawControl);
            map.off('draw:created', drawCreated);
        });
    });

    $scope.$watch('trip.shape', function () {
        $scope.$emit('updateHeight');
    }, true);


    // INIT

    leafletData.getMap().then(function (map) {
        map.addControl(drawControl);

        map.addLayer(OTIDrawService.drawnItems);
        map.on('draw:created', drawCreated);

        // TODO: Load polyline from existing shapes
    });

    var addStopToMap = function(stopTime) {
        var marker = new L.Marker([stopTime.stop.lat, stopTime.stop.long], {
            icon: OTIDrawService.getCircleIcon(stopTime.stopSequence.toString())
        });
        // Clicking on a stop should connect it in the shortest fashion to a drawn line
        marker.on('click', function() {
          $scope.trip.makeShape([[stopTime.stop.lat, stopTime.stop.long]]);
          mapTrip();
        });

        OTIDrawService.drawnItems.addLayer(marker);
    };


    // Create stops for use in drawing lines
    var mapTrip = function() {
        OTIDrawService.reset();
        leafletData.getMap().then(function (map) {
            var tripShape = L.polyline($scope.trip.shape.coordinates, OTIDrawService.defaultPolylineOpts);
            OTIDrawService.drawnItems.addLayer(tripShape);
            _.each($scope.trip.stopTimes, function (stopTime) {
                addStopToMap(stopTime);
            });
            map.addLayer(OTIDrawService.drawnItems);
        });
        $scope.trip.calculateDistance();
    };

    mapTrip();
}]);
