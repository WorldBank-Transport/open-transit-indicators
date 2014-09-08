'use strict';
angular.module('transitIndicators')
.controller('OTIRootController',
            ['config', '$cookieStore', '$scope', '$state', 'OTIEvents', 'OTIIndicatorsMapService', 'authService',
            function (config, $cookieStore, $scope, $state, OTIEvents, mapService, authService) {

    var mapStates = ['map', 'transit', 'scenarios'];

    // Setup defaults for all leaflet maps:
    // Includes: baselayers, center, bounds
    $scope.leafletDefaults = config.leaflet;
    $scope.leaflet = angular.extend({}, $scope.leafletDefaults);

    $scope.activeState = $cookieStore.get('activeState');
    if (!$scope.activeState) {
        $state.go('transit');
    }

    // asks the server for the data extent and zooms to it
    var zoomToDataExtent = function () {
        mapService.getMapInfo().then(function(mapInfo) {
            if (mapInfo.extent) {
                $scope.leaflet.bounds = mapInfo.extent;
            } else {
                // no extent; zoom out to world
                $scope.leaflet.bounds = config.worldExtent;
            }
        });
    };

    $scope.logout = function () {
        authService.logout();
   };

    $scope.init = function () {
        zoomToDataExtent();
    };

    $scope.updateLeafletOverlays = function (newOverlays) {
        if (!newOverlays) {
            newOverlays = {};
        }
        $scope.leaflet.markers.length = 0;
        $scope.leaflet.layers.overlays = newOverlays;
    };

    var setActiveState = function (activeState) {
        $scope.activeState = activeState;
        $cookieStore.put('activeState', activeState);
    };

    // zoom to the new extent whenever a GTFS file is uploaded
    $scope.$on(OTIEvents.Settings.Upload.GTFSDone, function() {
        zoomToDataExtent();
    });

    // zoom out to world view when data deleted
    $scope.$on(OTIEvents.Settings.Upload.GTFSDelete, function() {
        $scope.leaflet.bounds = config.worldExtent;
    });

    $scope.$on('$stateChangeStart', function () {
        // Always clear the legend when going to a state, we will always need to redraw it with
        // the proper legend in the child state
        $scope.leaflet.legend = {};
    });

    $scope.$on('$stateChangeSuccess', function (event, toState) {

        $scope.mapActive = _.find(mapStates, function (state) { return state === toState.name; }) ? true : false;

        var activeState = toState.name;
        if (toState.parent === 'indicators') {
            activeState = 'indicators';
        } else if (toState.parent === 'settings') {
            activeState = 'settings';
        }
        setActiveState(activeState);
    });

    $scope.init();

}]);