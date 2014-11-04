'use strict';

/**

TripManager singleton

Usage:
Set the trip manager database and route with:
setScenarioDbName(db_name)
setRouteId(routeId)

Then get trips with:
list({
    tripId: '<trip id here>'
})

*/

angular.module('transitIndicators')
.factory('OTITripManager', ['$q', 'OTITripModel',
         function ($q, OTITripModel) {

    var SHAPE_TYPE = 'LineString';

    var _dbName = null;
    var _routeId = null;
    var _trip = {};
    var _representativeTrips = [];

    var module = {};

    module.create = function () {
        var trip = new OTITripModel();
        trip.tripId = '';
        trip.routeId = _routeId;
        trip.headsign = '';
        trip.stopTimes = [];
        trip.frequencies = [];
        trip.shape = {
            type: SHAPE_TYPE,
            coordinates: []
        };
        _trip = trip;
    };

    module.get = function() {
        return _trip;
    };

    module.retrieve = function (tripId) {
        var dfd = $q.defer();
        var params = {
            db_name: _dbName,
            routeId: _routeId,
            tripId: tripId
        };
        OTITripModel.get(params, function (trip) {
            module.set(trip);
            dfd.resolve(trip);
        }, function (error) {
            console.error('OTITripManager.retrieve(): Error ', tripId, error);
            dfd.resolve({});
        });
        return dfd.promise;
    };

    module.set = function (trip) {
        _trip = trip;
    };

    module.setScenarioDbName = function (dbName) {
        _dbName = dbName;
    };

    module.setRouteId = function (routeId) {
        _routeId = routeId;
    };

    module.clear = function () {
        _trip = {};
    };

    // Get trips for a given db_name, routeId, tripId
    //  Set queryParams db_name, routeId globally via class methods.
    //  Can override db_name, routeId defaults in queryParams
    module.list = function (queryParams) {
        var dfd = $q.defer();
        var params = {
            db_name: _dbName,
            routeId: _routeId
        };
        params = angular.extend({}, params, queryParams);

        OTITripModel.groups(params,function (result) {
            _representativeTrips = result;
            dfd.resolve(result);
        }, function (error) {
            var empty = [];
            _representativeTrips = empty;
            console.error('OTIRouteModel.list(): ', error);
            dfd.resolve(empty);
        });
        return dfd.promise;
    };

    return module;

}]);
