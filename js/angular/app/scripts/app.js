'use strict';

angular.module('transitIndicators', [
    'ngAnimate',
    'ngCookies',
    'ngResource',
    'ui.router',
    'angularFileUpload',
    'pascalprecht.translate',
    'leaflet-directive',
    'ui.bootstrap',
    'ui.utils',
    'nvd3ChartDirectives',
    'angular-spinkit'
]).config(['$stateProvider', '$urlRouterProvider', '$locationProvider', 'config', '$httpProvider',
        function ($stateProvider, $urlRouterProvider, $locationProvider, config, $httpProvider) {

    $httpProvider.interceptors.push('authInterceptor');
    $httpProvider.interceptors.push('logoutInterceptor');

    $urlRouterProvider.when('', '/transit');
    $urlRouterProvider.otherwise('/transit');

    $stateProvider
        .state('root', {
            abstract: true,
            templateUrl: 'scripts/modules/root/root-partial.html',
            controller: 'OTIRootController',
            resolve: {
                authService: 'authService',
                OTIUserService: 'OTIUserService',
                user: function (OTIUserService, authService) {
                    return OTIUserService.getUser(authService.getUserId());
                }
            }
        })
        .state('login', {
            url: '/login/',
            templateUrl: 'scripts/modules/auth/login-partial.html',
            controller: 'OTIAuthController'
        })
        .state('transit', {
            parent: 'root',
            url: '/transit',
            templateUrl: 'scripts/modules/transit/transit-partial.html',
            controller: 'OTITransitController'
        })
        .state('indicators', {
            parent: 'root',
            url: '/indicators',
            templateUrl: 'scripts/modules/indicators/indicators-partial.html',
            controller: 'OTIIndicatorsController',
            resolve: {
               OTICityManager: 'OTICityManager',
               cities: function (OTICityManager) {
                    return OTICityManager.list();
               }
            }
        })
        .state('map', {
            parent: 'indicators',
            url: '/map',
            templateUrl: 'scripts/modules/indicators/map-partial.html',
            controller: 'OTIIndicatorsMapController'
        })
        .state('data', {
            parent: 'indicators',
            url: '/data',
            templateUrl: 'scripts/modules/indicators/data-partial.html',
            controller: 'OTIIndicatorsDataController'
        })
        .state('calculation', {
            parent: 'indicators',
            url: '/calculation',
            templateUrl: 'scripts/modules/indicators/calculation-partial.html',
            controller: 'OTIIndicatorsCalculationController'
        })
        .state('scenarios', {
            abstract: true,
            parent: 'root',
            url: '/scenarios',
            templateUrl: 'scripts/modules/scenarios/scenarios-partial.html',
            controller: 'OTIScenariosController',
            resolve: {
                OTITypes: 'OTITypes',
                OTISettingsService: 'OTISettingsService',
                samplePeriods: function (OTISettingsService) {
                    return OTISettingsService.samplePeriods.query();
                },
                samplePeriodI18N: function (OTITypes) {
                    return OTITypes.getSamplePeriodTypes();
                },
                routeTypes: function (OTITypes) {
                    return OTITypes.getRouteTypes();
                }
            }
        })
        .state('settings', {
            parent: 'root',
            url: '/settings',
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

        _.each(config.scenarioViews, function (view) {
            var viewId = view.id;
            var nodash = viewId.replace('-', '');
            var capsId = nodash.charAt(0).toUpperCase() + nodash.slice(1);
            $stateProvider.state(view.id, {
                parent: 'scenarios',
                url: '/' + viewId,
                templateUrl: 'scripts/modules/scenarios/' + viewId + '/' + viewId + '-partial.html',
                controller: 'OTIScenarios' +  capsId + 'Controller',
                resolve: {

                }
            });
        });

}]).config(['$translateProvider', 'config', function($translateProvider, config) {
    $translateProvider.useStaticFilesLoader({
       prefix: 'i18n/',
       suffix: '.json'
    });
    // Log untranslated tokens to console
    $translateProvider.useMissingTranslationHandlerLog();
    // Use browser's set language if one of our supported languages; otherwise, English
    var languageActual = (navigator.language || navigator.userLanguage).substring(0,2);
    /** list of IANA language tags used by browsers here:
    * http://www.iana.org/assignments/language-subtag-registry/language-subtag-registry
    *
    * zh -> Chinese (macrolanguage tag)
    * vi -> Vietnamese
    * lha -> Laha (Viet Nam)
    * nut -> Nung (Viet Nam)
    */
    var languageUsing = (_.contains(_.values(config.languages), languageActual) ? languageActual : config.defaultLanguage);
    $translateProvider.preferredLanguage(languageUsing);
    $translateProvider.fallbackLanguage('en');
}]).config(['$logProvider', function($logProvider) {
    $logProvider.debugEnabled(true);
}]).run(['$cookies', '$http', '$rootScope', '$state', 'authService', 'OTIEvents',
    function($cookies, $http, $rootScope, $state, authService, OTIEvents) {

        // Create cache object for useful global objects, e.g. the legends
        // TODO: Should use $cacheFactory as a service
        $rootScope.cache = {};

        // Django CSRF Token compatibility
        $http.defaults.headers.post['X-CSRFToken'] = $cookies.csrftoken;

        var anonymousStates = ['login'];
        var stateClean = function (state) {
            return _.find(anonymousStates, function (noAuthState) {
                return state.indexOf(noAuthState) === 0;
            });
        };

        // Load login page if user not authenticated
        $rootScope.$on('$stateChangeStart', function (event, to) {
            if (!stateClean(to.name) && !authService.isAuthenticated()) {
                event.preventDefault();
                $state.go('login');
                return;
            }
        });

        $rootScope.$on(OTIEvents.Auth.LoggedOut, function () {
            $rootScope.user = null;
        });

        $rootScope.$on(OTIEvents.Auth.LogOutUser, function () {
            authService.logout();
        });

        // Restore user session on full page refresh
        if (authService.isAuthenticated()) {
            $rootScope.$broadcast(OTIEvents.Auth.LoggedIn);
        }
}]);
