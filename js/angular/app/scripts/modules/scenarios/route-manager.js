'use strict';

angular.module('transitIndicators')
.factory('OTIRouteManager', ['$q', 'OTIRouteModel',
         function ($q, OTIRouteModel) {

    var route = {};
    var routes = [];

    var module = {};

    module.create = function () {
        route = new OTIRouteModel();
    };

    module.get = function() {
        return route;
    };

    module.set = function (newRoute) {
        route = newRoute;
    };

    module.clear = function () {
        route = {};
    };

    module.filter = function (routeType) {
        if (routeType === -1) {
            return routes;
        }
        return _.filter(routes, function (r) {
            return r.routeType === routeType;
        });
    };

    module.list = function (queryParams) {
        var dfd = $q.defer();
        OTIRouteModel.query(queryParams,function (result) {
            routes = result;
            dfd.resolve(result);
        }, function (error) {
            var empty = [];
            routes = empty;
            console.error('OTIRouteModel.list(): ', error);
            dfd.resolve(empty);
        });
        return dfd.promise;
    };

    return module;

}]);
