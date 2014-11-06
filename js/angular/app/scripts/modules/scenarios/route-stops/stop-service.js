'use strict';

/* global L */

angular.module('transitIndicators')
.factory('OTIStopService', ['OTIDrawService', function (OTIDrawService) {

    var module = {};

    module.layerFromStopTime = function (stopTime) {
        var layer = L.Marker([stopTime.stop.lat, stopTime.stop.long], {
            icon: OTIDrawService.getCircleIcon(stopTime.stopSequence.toString())
        });
        OTIDrawService.drawnItems.addLayer(layer);
    };

    module.stopTimeFromLayer = function(layer) {
        var stopTime = {};
        var getId = function () {
            var id = [];
            var possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';

            for (var i = 0; i < 8; i++) {
                id.push(possible.charAt(Math.floor(Math.random() * possible.length)));
            }
            return id.join('');
        };
        stopTime.arrivalTime = '';
        stopTime.departureTime = '';
        stopTime.stopSequence = 0;
        stopTime.stop = {
            lat: layer._latlng.lat,
            long: layer._latlng.lng,
            name: '',
            stopId: getId()
        };
        return stopTime;
    };


    return module;

}]);
