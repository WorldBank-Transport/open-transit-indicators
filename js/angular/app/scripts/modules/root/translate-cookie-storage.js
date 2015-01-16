'use strict';
angular.module('transitIndicators')
.factory('$translateCookieStorage', ['$cookies', function ($cookies) {
    var $translateCookieStorage = {
        get: function (name) {
            return $cookies[name];
        },
        set: function (name, value) {
            $cookies[name] = value;
        }
    };
    return $translateCookieStorage;
}]);
