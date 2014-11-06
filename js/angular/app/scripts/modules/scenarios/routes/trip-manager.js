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
.factory('OTITripManager', ['$q', 'OTITripModel', 'OTIFrequencyModel', 'leafletData', 'OTIDrawService', 'OTIStopService',
         function ($q, OTITripModel, OTIFrequencyModel, leafletData, OTIDrawService, OTIStopService) {

    var getId = function () {
        var id = [];
        var possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';

        for (var i = 0; i < 8; i++) {
            id.push(possible.charAt(Math.floor(Math.random() * possible.length)));
        }
        return id.join('');
    };

    var makeTrip = function () {
        var trip = new OTITripModel();
        trip.tripId = getId();
        trip.routeId = _routeId;
        trip.headsign = '';
        trip.stopTimes = [];
        trip.frequencies = [
            // TODO: Set frequency start/end times == scenario sample period on create
            new OTIFrequencyModel()
        ];
        trip.shape = {
            type: SHAPE_TYPE,
            coordinates: []
        };
        return trip;
    };

    var SHAPE_TYPE = 'LineString';
    var DEFAULT_TRIP_ID = 'BLANKTRIP';

    var _dbName = null;
    var _routeId = null;
    var _trip = {};
    var _representativeTrips = [];

    var module = {};

    module.create = function () {
        var trip = makeTrip();
        _trip = trip;
        return trip;
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
            module.set(trip.orderStops());
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

    module.removeStopTime = function(stop) {
      _trip.removeStopTime(stop.stopSequence-1);
      module.mapStops();
    };

    // Call this function whenever trips need to be mapped after a change occurs
    module.mapStops = function() {
        OTIDrawService.reset();
        leafletData.getMap().then(function (map) {

            _.each(_trip.stopTimes, function (stopTime) {
                var marker = new L.Marker([stopTime.stop.lat, stopTime.stop.long], {
                   icon: OTIDrawService.getCircleIcon(stopTime.stopSequence.toString())
                }).on('click', function(){console.log(stopTime);});

                OTIDrawService.drawnItems.addLayer(marker);
            });
            map.addLayer(OTIDrawService.drawnItems);
        });
    };

    module.drawCreated = function (event) {
        var type = event.layerType,
            layer = event.layer;
        if (type === 'marker') {
            var stopTime = OTIStopService.stopTimeFromLayer(layer);
            stopTime.stopName = 'SimulatedStop-' + _trip.stopTimes.length;
            _trip.addStopTime(stopTime);

            var marker = new L.Marker([layer._latlng.lat, layer._latlng.lng], {
               icon: OTIDrawService.getCircleIcon(stopTime.stopSequence.toString())
            });
            OTIDrawService.drawnItems.addLayer(marker);
        }
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
