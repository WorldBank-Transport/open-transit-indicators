'use strict';

angular.module('transitIndicators', [
    'ngCookies',
    'ngResource',
    'ui.router',
    'angularFileUpload',
    'leaflet-directive',
    'ui.bootstrap'
]).config(['$stateProvider', '$urlRouterProvider', 'config',
        function ($stateProvider, $urlRouterProvider, config) {

    $urlRouterProvider.when('', '/');
    $urlRouterProvider.otherwise('/');

    $stateProvider
        .state('map', {
            url: '/',
            templateUrl: 'scripts/modules/map/map-partial.html',
            controller: 'GTFSMapController'
        })

        .state('settings', {
            parent: 'map',
            url: 'settings',
            templateUrl: 'scripts/modules/settings/settings-partial.html',
            controller: 'GTFSSettingsController'
        });

        // URL matches to /settings/<view.id>
        // Add new entries to scripts/config.js in settingsViews
        _.each(config.settingsViews, function (view) {
            var viewId = view.id;
            var capsId = viewId.charAt(0).toUpperCase() + viewId.slice(1);
            $stateProvider.state(view.id, {
                parent: 'settings',
                url: '/' + viewId,
                templateUrl: 'scripts/modules/settings/' + viewId + '/' + viewId + '-partial.html',
                controller: 'GTFS' + capsId + 'Controller'
            });
        });
}]);

