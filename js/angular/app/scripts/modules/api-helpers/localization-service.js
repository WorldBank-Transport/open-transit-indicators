'use strict';

angular.module('transitIndicators')
.factory('OTILocalization', ['$http', '$q', '$resource',
        function ($http, $q, $resource) {

    var module = {};

    module.getLanguages = function () {
        var dfd = $q.defer();
        $http.get('/api/languages/', {
            cache: true
        }).success(function (data) {
            dfd.resolve(data);
        }).error(function (error) {
            console.error('OTILanguages.getLanguages', error.error);
            dfd.resolve({});
        });
        return dfd.promise;
    }

    module.getTimeZones = function () {
        var dfd = $q.defer();
        $http.get('/api/timezones/', {
            cache: true
        }).success(function (data) {
            dfd.resolve(data);
        }).error(function (error) {
            console.error('OTILanguages.getTimeZones', error.error);
            dfd.resolve({});
        });
        return dfd.promise;
    }

    module.setTimeZone = function (timezone) {
        var dfd = $q.defer();
        $http.post('/api/timezones/', {
            data: {
                timezone: timezone
            }
        }).success(function (data) {
            dfd.resolve(data);
        }).error(function (error) {
            console.error('OTILanguages.setTimeZone', error.error);
            dfd.resolve({});
        });
        return dfd.promise;
    }

    return module;
}]);
