'use strict';
angular.module('transitIndicators')
.controller('OTIRootController',
            ['config', '$cookieStore', '$cookies', '$scope', '$translate', '$state', '$stateParams', 'OTIEvents', 'OTIIndicatorsMapService', 'authService','leafletData',
            function (config, $cookieStore, $cookies, $scope, $translate, $state, $stateParams, OTIEvents, mapService, authService, leafletData) {

    var mapStates = ['map', 'transit', 'scenarios'];
    // Add all scenario views to map states
    mapStates = mapStates.concat(_.map(config.scenarioViews, function (view) { return view.id; }));

    // Setup defaults for all leaflet maps:
    // Includes: baselayers, center, bounds
    $scope.leafletDefaults = config.leaflet;
    $scope.leaflet = angular.extend({}, $scope.leafletDefaults);

    $scope.activeState = $cookieStore.get('activeState');
    if (!$scope.activeState) {
        $state.go('transit');
    }

    $scope.languages = config.languages;

    $scope.selectLanguage = function(language) {
        $translate.use(language);

        // Cannot use cookieStore because cookieStore
        // serializes to json and django will not deserialize
        // the cookie
        $cookies.openTransitLanguage = language;

        // $state.reload has a bug that does not actually force a refresh.
        // See: https://github.com/angular-ui/ui-router/issues/582
        // TODO: Use $state.reload() when ui-router is fixed
        $state.transitionTo($state.current,
                            $stateParams,
                            { reload: true, inherit: true, notify: true });
    };
    // Make Angular respect language cookies on page reload
    $translate.use($cookies.openTransitLanguage);

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

    $scope.$on('$stateChangeStart', function (event, toState, toStateParams, fromState) {
        // Always clear the legend when going to a state, we will always need to redraw it with
        // the proper legend in the child state

        $scope.leaflet.legend = {};

        if (fromState.parent.name === "scenarios") {
            leafletData.getMap().then(function(map) { // remove layers from the scenario
                map.eachLayer(function (layer) {      // prototype
                    if(layer.isScenarioLayer) {
                        map.removeLayer(layer);
                    }
                });
            });
        }

    });

    $scope.$on('$stateChangeSuccess', function (event, toState) {

        $scope.mapActive = _.find(mapStates, function (state) { return state === toState.name; }) ? true : false;
        $scope.mapClassNav2 = false;

        var activeState = toState.name;
        if (toState.parent === 'indicators') {
            activeState = 'indicators';
            $scope.mapClassNav2 = true;
        } else if (toState.parent === 'scenarios') {
            activeState = 'scenarios';
        } else if (toState.parent === 'settings') {
            activeState = 'settings';
        }
        setActiveState(activeState);
    });

    $scope.init();

}]);
