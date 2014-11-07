'use strict';

/* global L */

angular.module('transitIndicators')
.factory('OTIStopService', ['OTIDrawService', function (OTIDrawService) {

    var module = {};

    function getId() {
        var id = [];
        var possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';

        for (var i = 0; i < 8; i++) {
            id.push(possible.charAt(Math.floor(Math.random() * possible.length)));
        }
        return id.join('');
    }

    module.StopTime = function() {
        this.arrivalTime = '';
        this.departureTime = '';
        this.stopSequence = 0;
        this.stop = {
            lat: 0,
            long: 0,
            name: '',
            stopId: getId()
        };
    };

    module.layerFromStopTime = function (stopTime) {
        var layer = L.Marker([stopTime.stop.lat, stopTime.stop.long], {
            icon: OTIDrawService.getCircleIcon(stopTime.stopSequence.toString())
        });
        OTIDrawService.drawnItems.addLayer(layer);
    };

    module.stopTimeFromLayer = function(layer) {
        var stopTime = new module.StopTime();
        stopTime.stop.lat = layer._latlng.lat;
        stopTime.stop.long = layer._latlng.lng;
        return stopTime;
    };


    return module;

}]);
