'use strict';
angular.module('transitIndicators')
.controller('OTIRootController',
            ['config', '$scope', 'OTIIndicatorsMapService', 'authService',
            function (config, $scope, mapService, authService) {

    // Setup defaults for all leaflet maps:
    // Includes: baselayers, center, bounds
    $scope.leafletDefaults = config.leaflet;
    $scope.active = 'transit';

    // asks the server for the data extent and zooms to it
    var zoomToDataExtent = function () {
        mapService.getMapInfo().then(function(mapInfo) {
            if (mapInfo.extent) {
                $scope.leafletDefaults.bounds = mapInfo.extent;
            } else {
                // no extent; zoom out to world
                $scope.leafletDefaults.bounds = config.worldExtent;
            }
        });
    };

    $scope.logout = function () {
        authService.logout();
   };

    $scope.init = function () {
        zoomToDataExtent();
    };

    // zoom to the new extent whenever a GTFS file is uploaded
    $scope.$on('upload-controller:gtfs-uploaded', function() {
        zoomToDataExtent();
    });

    // zoom out to world view when data deleted
    $scope.$on('upload-controller:gtfs-deleted', function() {
        $scope.leafletDefaults.bounds = config.worldExtent;
    });

    $scope.$on('$stateChangeSuccess', function (event, toState) {
        var activeState = toState.name;
        if (toState.parent === 'indicators') {
            activeState = 'indicators';
        } else if (toState.parent === 'settings') {
            activeState = 'settings';
        }
        $scope.active = activeState;
    });

    $scope.init();

}]);