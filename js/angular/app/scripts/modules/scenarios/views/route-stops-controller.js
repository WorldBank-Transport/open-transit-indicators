'use strict';

/* global L */

angular.module('transitIndicators')
.controller('OTIScenariosRoutestopsController',
            ['config', '$scope', '$state', '$stateParams', 'OTIScenariosService', 'leafletData', 'OTIDrawService',
            function (config, $scope, $state, $stateParams, OTIScenariosService, leafletData, OTIDrawService) {

    // TODO: Click existing utfgrid stop to add to route
    // TODO: Refactor and cleanup marker add logic?

    // LOCAL

    var layerHash = {};

    var drawControl = OTIDrawService.markerDrawControl;
    var markerController = OTIDrawService.markerController;

    var drawCreated = function (event) {
        var type = event.layerType,
            layer = event.layer;
        if (type === 'marker') {
            var stop = OTIScenariosService.stopFromMarker(layer);
            stop.stopName = 'Stop ' + markerController.count();
            layerHash[stop.stopId] = layer;
            $scope.route.stops.push(stop);
            markerController.increment();
        }
        OTIDrawService.drawnItems.addLayer(layer);
    };


    // $SCOPE

    $scope.scenario = OTIScenariosService.otiScenario;
    $scope.route = OTIScenariosService.otiRoute;

    $scope.continue = function () {
        $state.go('route-shapes');
    };

    $scope.back = function () {
        $scope.scenario = {};
        $state.go('route-edit');
    };

    $scope.deleteStop = function (stopIndex) {
        var removed = $scope.route.stops.splice(stopIndex, 1)[0];
        OTIDrawService.drawnItems.removeLayer(layerHash[removed.stopId]);
    };

    $scope.$on('$stateChangeStart', function () {
        leafletData.getMap().then(function (map) {
            map.removeControl(drawControl);
            map.off('draw:created', drawCreated);
        });
    });

    $scope.$watch('route.stops', function () {
        $scope.$emit('updateHeight');
    }, true);


    // INIT

    leafletData.getMap().then(function (map) {
        map.addControl(drawControl);

        _.each($scope.route.stops, function (stop) {
            var marker = new L.Marker([stop.stopLat, stop.stopLon], {
               icon: OTIDrawService.getCircleIcon()
            });
            OTIDrawService.drawnItems.addLayer(marker);
            layerHash[stop.stopId] = marker;
            markerController.increment();
        });
        map.addLayer(OTIDrawService.drawnItems);
        map.on('draw:created', drawCreated);
    });

}]);
