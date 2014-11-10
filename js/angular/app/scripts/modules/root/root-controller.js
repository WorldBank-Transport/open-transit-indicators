'use strict';
angular.module('transitIndicators')
.controller('OTIRootController',
            ['config', '$cookieStore', '$cookies', '$modal', '$scope', '$rootScope', '$timeout',
             '$translate', '$state', '$stateParams',
             'OTIEvents', 'OTIMapService', 'authService','leafletData', 'user',
            function (config, $cookieStore, $cookies, $modal, $scope, $rootScope, $timeout,
                      $translate, $state, $stateParams,
                      OTIEvents, OTIMapService, authService, leafletData, user) {

    var invalidateMapDiv = function () {
        leafletData.getMap().then(function (map) {
            $timeout(function () {
                map.invalidateSize();
            });
        });
    };

    var mapStates = ['map', 'transit', 'scenarios'];
    // Add all scenario views to map states
    mapStates = mapStates.concat(_.map(config.scenarioViews, function (view) { return view.id; }));

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

    // asks the server for the data extent and zooms to it
    var zoomToDataExtent = function () {
        OTIMapService.getMapExtent().then(function(extent) {
                $scope.leaflet.bounds = extent;
        }, function () {
                $scope.leaflet.bounds = config.worldExtent;
        });
    };

    var initialize = function () {
        // Setup defaults for all leaflet maps:
        // Includes: baselayers, center, bounds
        $scope.leafletDefaults = config.leaflet;
        $scope.leaflet = angular.extend({}, $scope.leafletDefaults);

        $scope.activeState = $cookieStore.get('activeState');
        if (!$scope.activeState) {
            $state.go('transit');
        }

        $scope.languages = config.languages;

        $rootScope.user = user;

        // Make Angular respect language cookies on page reload
        $translate.use($cookies.openTransitLanguage);

        $scope.modes = [];
        zoomToDataExtent();
    };

    $scope.logout = function () {
        authService.logout();
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

    $scope.setMapModes = function (modes) {
        OTIMapService.setTransitModes(modes);
        OTIMapService.refreshLayers();
    };

    $scope.changePassword = function() {
        $modal.open({
            templateUrl: 'scripts/modules/userdata/change-password.html',
            controller: 'OTIUserdataChangePasswordController',
            size: 'sm'
        });
    };

    // zoom to the new extent whenever a GTFS file is uploaded
    $scope.$on(OTIEvents.Settings.Upload.GTFSDone, function() {
        zoomToDataExtent();
    });

    // zoom out to world view and clear legend when data deleted
    $scope.$on(OTIEvents.Settings.Upload.GTFSDelete, function() {
        $scope.cache.transitLegend = undefined;
        $scope.leaflet.legend = {};
        $scope.leaflet.bounds = config.worldExtent;
    });

    $scope.$on('$stateChangeStart', function () {
        // Always clear the legend when going to a state, we will always need to redraw it with
        // the proper legend in the child state

        $scope.leaflet.legend = {};
    });

    $scope.$on('$stateChangeSuccess', function (event, toState) {

        $scope.mapActive = _.find(mapStates, function (state) { return state === toState.name; }) ? true : false;
        if ($scope.mapActive) {
            // When we go to the map we want to update the map div. It changes height based on which
            //  view we were last viewing.
            invalidateMapDiv();
            zoomToDataExtent();
        }

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

    $scope.$on(OTIMapService.Events.AvailableModesUpdated, function(event, modes) {
        $scope.modes = modes;
    });

    initialize();
}]);
