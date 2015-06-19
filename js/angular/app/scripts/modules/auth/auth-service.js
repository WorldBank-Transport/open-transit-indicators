'use strict';

angular.module('transitIndicators')
.factory('authService',
        ['config', '$q', '$http', '$cookieStore', '$rootScope', '$timeout', '$window', 'OTIEvents',
        function (config, $q, $http, $cookieStore, $rootScope, $timeout, $window, OTIEvents) {

    var userIdCookieString = 'authService.userId';
    var tokenCookieString = 'authService.token';
    var firstLoginCookieString = 'authService.firstLogin';
    var cookieTimeout = null;
    var cookieTimeoutMillis = 24 * 60 * 60 * 1000;      // 24 hours

    var setToken = function (token) {
        if (!token) {
            return;
        }

        // clear timeout if we re-authenticate for whatever reason
        if (cookieTimeout) {
            $timeout.cancel(cookieTimeout);
            cookieTimeout = null;
        }

        $cookieStore.put(tokenCookieString, token);

        cookieTimeout = $timeout(function() {
            authService.logout();
        }, cookieTimeoutMillis);

    };

    var setUserId = function(id) {
        var userId = parseInt(id, 10);
        userId = !isNaN(userId) && userId >= 0 ? userId : -1;
        $cookieStore.put(userIdCookieString, userId);
    };

    var authService = {

        isAuthenticated: function () {
            return !!(authService.getToken() && authService.getUserId() >= 0);
        },

        authenticate: function (auth) {
            var self = this;
            var dfd = $q.defer();
            $http.post('/api-token-auth/', auth)
            .success(function(data, status, headers, config) {
                var result = {
                    status: status,
                    error: ''
                };
                if (data && data.user) {
                    setUserId(data.user);
                }
                if (data && data.token) {
                    setToken(data.token);
                }
                $cookieStore.put(firstLoginCookieString, data.firstLogin);
                result.isAuthenticated = authService.isAuthenticated();
                if (result.isAuthenticated) {
                    $rootScope.$broadcast(OTIEvents.Auth.LoggedIn);
                } else {
                    result.error = 'Unknown error logging in.';
                }
                dfd.resolve(result);
            })
            .error(function(data, status, headers, config) {
                var error = _.values(data).join(' ');
                if (data.username) {
                    error = 'Username field required.';
                }
                if (data.password) {
                    error = 'Password field required.';
                }
                var result = {
                    isAuthenticated: false,
                    status: status,
                    error: error
                };
                dfd.resolve(result);
            });

            return dfd.promise;
        },

        getToken: function () {
            return $cookieStore.get(tokenCookieString);
        },

        getUserId: function () {
            var userId = parseInt($cookieStore.get(userIdCookieString), 10);
            return isNaN(userId) ? -1 : userId;
        },

        logout: function () {
            setUserId(null);
            $cookieStore.remove(firstLoginCookieString);
            $cookieStore.remove(tokenCookieString);
            $rootScope.$broadcast(OTIEvents.Auth.LoggedOut);
            if (cookieTimeout) {
                $timeout.cancel(cookieTimeout);
                cookieTimeout = null;
            }
            // trigger full page refresh
            $window.location.reload();
        }
    };

    return authService;
}]);
