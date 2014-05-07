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
            controller: 'OTIMapController'
        })

        .state('settings', {
            parent: 'map',
            url: 'settings',
            templateUrl: 'scripts/modules/settings/settings-partial.html',
            controller: 'OTISettingsController'
        });

        /*
         * config.settingsView view objects are:
         * {
         *      id: '<unique string>',
         *      label: '<sidebar display string>'
         * }
         * URL matches to /settings/<view.id>
         * To add a new entry in the settings sidebar:
         *  1) Add new 'view' object to config.settingsView in config.js with
         *     id and label properties
         *  2) Add controller and partial files to the path:
         *     modules/settings/<view.id>/<view.id>-controller.js
         *     modules/settings/<view.id>/<view.id>-partial.html
         *  3) Ensure controller has the name OTI<view.id>Controller where the first letter of
         *     view.id is capitalized, e.g. for view.id == upload, name is OTIUploadController
         *  4) Add <script> tag for your controller in ../index.html
         */
        _.each(config.settingsViews, function (view) {
            var viewId = view.id;
            var capsId = viewId.charAt(0).toUpperCase() + viewId.slice(1);
            $stateProvider.state(view.id, {
                parent: 'settings',
                url: '/' + viewId,
                templateUrl: 'scripts/modules/settings/' + viewId + '/' + viewId + '-partial.html',
                controller: 'OTI' + capsId + 'Controller'
            });
        });
}]);

