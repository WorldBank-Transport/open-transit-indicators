'use strict';

angular.module('transitIndicators')
.factory('OTITypes', ['$http', '$q',
        function ($http, $q) {

    var module = {};

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
