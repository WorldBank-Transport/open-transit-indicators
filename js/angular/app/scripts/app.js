'use strict';

angular.module('transitIndicators', [
    'ngCookies',
    'ngResource',
    'ui.router',
    'angularFileUpload',
    'leaflet-directive',
    'ui.bootstrap'
]).config(['$stateProvider', '$urlRouterProvider',
        function ($stateProvider, $urlRouterProvider) {

    $urlRouterProvider.when('', '/');
    $urlRouterProvider.otherwise('/');

    $stateProvider
        .state('map', {
            url: '/',
            templateUrl: 'scripts/modules/map/map-partial.html',
            controller: 'GTFSMapController'
        })
/*
        // TODO: Implement this abstract parent controller for the settings modules
        .state('settings', {
            abstract: true,
            parent: 'map',
            url: 'settings',
            templateUrl: 'scripts/modules/settings/settings-partial.html',
            controller: 'GTFSSettingsController'
*/
        // Actual url to navigate to would be <host>/#/upload
        .state('upload', {
            // TODO: Replace existing parent/url keys with these once abstract parent exists
            //parent: 'settings',
            //url: '/upload',
            parent: 'map',
            url: 'upload',
            templateUrl: 'scripts/modules/upload/gtfs/gtfs-upload-partial.html',
            controller: 'GTFSUploadController'
        });
}]);

