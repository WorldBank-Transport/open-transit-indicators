'use strict';

angular.module('transitIndicators')
.factory('OTITypes', ['$http', '$q', '$resource',
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

    module.getIndicatorTypes = function () {
        var dfd = $q.defer();
        $http.get('/api/indicator-types/', {
            cache: true
        }).success(function (data) {
            dfd.resolve(data);
        }).error(function (error) {
            console.error('OTIIndicatorService.getIndicatorTypes', error);
            dfd.resolve({});
        });
        return dfd.promise;
    };

    module.getIndicatorAggregationTypes = function () {
        var dfd = $q.defer();
        $http.get('/api/indicator-aggregation-types/', {
            cache: true
        }).success(function (data) {
            dfd.resolve(data);
        }).error(function (error) {
            console.error('OTIIndicatorService.getIndicatorAggregationTypes', error);
            dfd.resolve({});
        });
        return dfd.promise;
    };

    module.getSamplePeriodTypes = function () {
        var dfd = $q.defer();
        $http.get('/api/sample-period-types/', {
            cache: true
        }).success(function (data) {
            dfd.resolve(data);
        }).error(function (error) {
            console.error('OTIIndicatorService.getSamplePeriodTypes', error);
            dfd.resolve({});
        });
        return dfd.promise;
    };

    module.SamplePeriod = $resource('/api/sample-periods/:samplePeriod/', {
        'samplePeriod': '@samplePeriod'
    }, null, {
        stripTrailingSlashes: false
    });

    module.getRouteTypes = function () {
        var dfd = $q.defer();
        $http.get('/api/gtfs-route-types/', {
            cache: true
        }).success(function (data) {
            dfd.resolve(data);
        }).error(function (error) {
            console.error('getRouteTypes Error: ', error);
            dfd.resolve([]);
        });
        return dfd.promise;
    };

    return module;
}]);
