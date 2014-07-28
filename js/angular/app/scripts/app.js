'use strict';

angular.module('transitIndicators', [
    'ngCookies',
    'ngResource',
    'ui.router',
    'angularFileUpload',
    'leaflet-directive',
    'ui.bootstrap',
    'ui.utils'
]).config(['$stateProvider', '$urlRouterProvider', 'config', '$httpProvider',
        function ($stateProvider, $urlRouterProvider, config, $httpProvider) {

    $httpProvider.interceptors.push('authInterceptor');
    $httpProvider.interceptors.push('logoutInterceptor');

    $urlRouterProvider.when('', '/');
    $urlRouterProvider.otherwise('/');

    $stateProvider
        .state('login', {
            url: '/login/',
            templateUrl: 'scripts/modules/auth/login-partial.html',
            controller: 'OTIAuthController'
        })
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
}]).run(['$rootScope', '$state', '$cookies', '$http', 'authService', 'OTIUserService',
    function($rootScope, $state, $cookies, $http, authService, OTIUserService) {

        // Django CSRF Token compatibility
        $http.defaults.headers.post['X-CSRFToken'] = $cookies.csrftoken;

        var anonymousStates = ['login'];
        var stateClean = function (state) {
            return _.find(anonymousStates, function (noAuthState) {
                return state.indexOf(noAuthState) === 0;
            });
        };

        // Load login page if user not authenticated
        $rootScope.$on('$stateChangeStart', function (event, to, toParams, from, fromParams) {
            if (!stateClean(to.name) && !authService.isAuthenticated()) {
                event.preventDefault();
                $state.go('login');
                return;
            }
        });

        $rootScope.$on('authService:loggedIn', function () {
            OTIUserService.getUser(authService.getUserId()).then(function (data) {
                $rootScope.user = data;
            });
        });

        $rootScope.$on('authService:loggedOut', function () {
            $rootScope.user = null;
        });

        $rootScope.$on('authService:logOutUser', function () {
            authService.logout();
        });

        // Restore user session on full page refresh
        if (authService.isAuthenticated()) {
            $rootScope.$broadcast('authService:loggedIn');
        }
}]);

