'use strict';

angular.module('transitIndicators', [
    'ngCookies',
    'ngResource',
    'ui.router',
    'angularFileUpload',
    'ui.bootstrap'
]).config(['$stateProvider', '$urlRouterProvider',
        function ($stateProvider, $urlRouterProvider) {

    $urlRouterProvider.otherwise('/upload/');
    $stateProvider
        .state('upload', {
            url: '/upload/',
            templateUrl: 'scripts/modules/upload/gtfs/gtfs-upload-partial.html',
            controller: 'GTFSUploadController'
        });
}]);

