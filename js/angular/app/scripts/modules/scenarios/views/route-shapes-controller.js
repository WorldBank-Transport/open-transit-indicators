'use strict';

/* global L */

angular.module('transitIndicators')
.controller('OTIScenariosRouteshapesController',
            ['config', '$scope', '$state', '$stateParams', 'OTIScenariosService', 'leafletData', 'OTIDrawService',
            function (config, $scope, $state, $stateParams, OTIScenariosService, leafletData, OTIDrawService) {

    // TODO: Click existing utfgrid stop to add to route
    // TODO: Refactor and cleanup marker add logic?

    // LOCAL
    var layerCache = null;
    var drawControl = OTIDrawService.lineDrawControl;

    var drawCreated = function (event) {
        var layer = event.layer;
        OTIDrawService.drawnItems.addLayer(layer);
        var latLngs = layer.getLatLngs();
        _.each(latLngs, function (latLng) {
            $scope.route.addShape(latLng);
        });
        layerCache = layer;
    };


    // $SCOPE

    $scope.scenario = OTIScenariosService.otiScenario;
    $scope.route = OTIScenariosService.otiRoute;
    $scope.route =  new OTIScenariosService.Route();

    $scope.delete = function () {
        $scope.route.deleteShapes();
        OTIDrawService.drawnItems.removeLayer(layerCache);
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

    $scope.$watch('route.shapes', function () {
        $scope.$emit('updateHeight');
    }, true);


    // INIT

    leafletData.getMap().then(function (map) {
        map.addControl(drawControl);

        map.addLayer(OTIDrawService.drawnItems);
        map.on('draw:created', drawCreated);

        // TODO: Load polyline from existing shapes
    });

}]);
