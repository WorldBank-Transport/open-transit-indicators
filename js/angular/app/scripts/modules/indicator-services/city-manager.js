'use strict';

angular.module('transitIndicators')
.factory('OTICityManager', ['$http', '$q',
        function ($http, $q) {

    var module = {};

    module.Events = {
        CitiesUpdated: 'OTI:CityManager:CitiesUpdated'
    };

    /**
     * Get the list of cities loaded into the app
     */
    module.list = function () {
        var dfd = $q.defer();
        $http.get('/api/indicator-cities/').success(function (data) {
            dfd.resolve(data.sort());
        }).error(function(error) {
            console.error('OTICityManager.list()', error);
            dfd.resolve([]);
        });
        return dfd.promise;
    };

    /**
     * Delete a chosen city
     */
    module.delete = function (cityname) {
        var dfd = $q.defer();
        $http.delete('/api/indicator-cities/', {
            params: {
                city_name: cityname
            }
        }).success(function () {
            dfd.resolve();
        }).error(function (error) {
            console.error('OTICityManager.delete()', error);
            dfd.reject(error);
        });
        return dfd.promise;
    };

    return module;
}]);
