'use strict';

angular.module('transitIndicators')
.factory('OTIRouteManager', ['$q', 'OTIRouteModel', 'OTIScenarioManager', 'OTIUIDService',
         function ($q, OTIRouteModel, OTIScenarioManager, OTIUIDService) {

    var route = {};
    var routes = [];

    var module = {};

    module.create = function () {
        var scenario = OTIScenarioManager.get();
        route = new OTIRouteModel({
            db_name: scenario.db_name,
            id: OTIUIDService.getId(),
            isNew: true
        });
    };

    module.get = function() {
        return route;
    };

    module.set = function (newRoute) {
        route = newRoute;
    };

    /**
     * For now, isNew is defined as the route not having an id
     * Could track this with an internal var instead
     * @return Boolean True if the stored route is new, False if it was retrieved from the API
     */
    module.isNew = function () {
        return !!(route.isNew);
    };

    module.clear = function () {
        route = {};
    };

    module.filter = function (routeType) {
        if (routeType === '' || routeType === -1) {
            return routes;
        }
        return _.filter(routes, function (r) {
            return r.routeType === routeType;
        });
    };

    module.findById = function (routeId) {
        return _.find(routes, function (r) {
            return r.id === routeId;
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
