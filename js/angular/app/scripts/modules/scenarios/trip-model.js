'use strict';

// THIS CLASS IS BUSTED
// It is only a repository for the stubbed trip functions that were written as
//  part of the prototype
// These should be refactored to match RouteModel/RouteManager

angular.module('transitIndicators')
.factory('OTITripModel', ['$http', 'OTIShape',
        function ($http, OTIShape) {

    var Trip = {};

    // Sort by shapePtSequence ascending
    Trip.prototype.sortShapes = function () {
        this.shapes.sort(function (a, b) {
            return a.shapePtSequence - b.shapePtSequence;
        });
    };

    Trip.prototype.addShape = function (latLng) {
        if (latLng && latLng.lat && latLng.lng) {
            var shape = OTIShape.create();
            var index = this.shapes.length;
            shape.shapePtLat = latLng.lat;
            shape.shapePtLon = latLng.lng;
            shape.shapeDistTraveled = 0;
            if (this.lastShapeLatLng) {
                var newDistance = latLng.distanceTo(this.lastShapeLatLng) / 1000.0;
                var oldDistance = this.shapes[index - 1].shapeDistTraveled;
                shape.shapeDistTraveled = oldDistance + newDistance;
            }
            shape.shapePtSequence = index;
            this.shapes.push(shape);
            this.lastShapeLatLng = latLng;
        }
    };

    Trip.prototype.deleteShapes = function () {
        this.shapes = [];
        this.lastShapeLatLng = null;
    };

    return Trip;

    var StopTime = function (stopId) {
        if (!stopId) {
            return null;
        }
        this.stopId = stopId;
        this.arrivalTime = '';
        this.departureTime = '';
    };

    var Shape = function (shape_pt_sequence) {
        this.shapeId = '';
        this.shapePtLat = -999;
        this.shapePtLon = -999;
        this.shapePtSequence = shape_pt_sequence || 0;
        this.shapeDistTraveled = 0;
    };

    Shape.prototype.create = function () {};

    var Stop = function () {
        this.stopId = '';
        this.stopName = '';
        this.stopDesc = '';
        this.stopLat = -999;
        this.stopLon = -999;
        this.stopTime = new StopTime(this.stopId);
    };

    var stopFromMarker = function (marker) {
        var stop = new Stop();
        var latLon = marker.getLatLng();
        stop.stopName = 'Stop ' + (Trip.stops.length + 1);
        stop.stopLat = latLon.lat;
        stop.stopLon = latLon.lng;
        return stop;
    };

}]);