'use strict';

angular.module('transitIndicators')
.factory('OTITripModel', ['$resource', function ($resource) {

    // This takes the function signature of the angular transformResponse functions
    // @param tripsString String data returned from endpoint
    // @param headers Headers sent with the response
    // @return First trip in each group of trips as Array[String]
    var transformTrips = function (tripsString) {
        var trips2dArray = angular.fromJson(tripsString);
        if (!trips2dArray) {
            return [];
        }
        return _.map(trips2dArray, function (tripArray) {
            return tripArray[0];
        }).filter(function(p) { // Remove undefined/null entries
            return p;
        });
    };

    var baseUrl = '/gt/scenarios/:db_name/routes/:routeId/trips';
    var url = baseUrl + '/:tripId';
    var module = $resource(url, {
        db_name: '@db_name',
        routeId: '@routeId',
        tripId: '@tripId'
    }, {
        // Model specific override of get()
        // Takes the list endpoint which is a 2dArray of grouped trips
        //  and returns via transformResponse a single array of representative trips
        groups: {
            method: 'GET',
            isArray: true,
            transformResponse: transformTrips,
            url: baseUrl
        },
        get: {
          method: 'GET',
          transformResponse: function(tripString) {
            // convert lng,lat points into lat,lng - this is a fix for apparently
            // broken geotrellis json conversion
            var trip = angular.fromJson(tripString);
            trip.shape.coordinates = _.map(
                trip.shape.coordinates, function(lnglat) {
                    return [lnglat[1], lnglat[0]];
                });
            return trip;
          }
        }
    });

    angular.extend(module.prototype, {

        addFrequency: function (frequency, index) {
            // For now, only one frequency is allowed
            this.clearFrequencies();
            this.frequencies.push(frequency);
        },

        getFrequency: function (index) {
            index = parseInt(index, 10);
            if (isNaN(index)) {
                index = 0;
            }
            if (index >= this.frequencies.length) {
                return null;
            }
            return this.frequencies[index];
        },

        clearFrequencies: function () {
            this.frequencies = [];
        },

        makeShape: function (newCoords) {
            // This function takes an array of coordinates (itself of the form [lat, long])
            // and determines, for each pairing of head to tail, the shortest path which connects
            // said head and tail. It then creates a new set of coordinates which joins together
            // old and new coordinates as a line with the shortest possible, inferred connection

            function distance(p, q) {
              return Math.sqrt(Math.pow(p[0] - q[0], 2) + Math.pow(p[1] - q[1], 2));
            }

            var oldCoords = this.shape.coordinates;
            if (oldCoords.length > 0) {
                var head = oldCoords[0],
                    last = oldCoords[oldCoords.length-1],
                    newHead = newCoords[0],
                    newLast = newCoords[newCoords.length-1],
                    headHead = [distance(head, newHead), 'headHead'],
                    headLast = [distance(head, newLast), 'headLast'],
                    lastHead = [distance(last, newHead), 'lastHead'],
                    lastLast = [distance(last, newLast), 'lastLast'],
                    combinations = [headHead, headLast, lastHead, lastLast];
                var leastDistance = _.min(combinations, function(val) { return val[0]; });

                // create the shortest possible line
                switch (leastDistance[1]) {
                    case 'headHead':
                        this.shape.coordinates = newCoords.reverse().concat(oldCoords);
                        break;
                    case 'headLast':
                        this.shape.coordinates = newCoords.concat(oldCoords);
                        break;
                    case 'lastHead':
                        this.shape.coordinates = oldCoords.concat(newCoords);
                        break;
                    case 'lastLast':
                        this.shape.coordinates = oldCoords.concat(newCoords.reverse());
                }
            } else {
              this.shape.coordinates = newCoords;
            }
        },
        removeShape: function (index) {
            this.shape.coordinates = [];
        },
        orderStops: function() {
            this.stopTimes = _.map(this.stopTimes, function(stopTime, index) {
                stopTime.stopSequence = index + 1;
                return stopTime;
            });
        },
        // if index undefined, add to end of array
        addStopTime: function (stopTime, index) {
            index = typeof index !== 'undefined' ? index : this.stopTimes.length;
            this.stopTimes.splice(index, 0, stopTime);
            this.orderStops();
        },
        removeStopTime: function (index) {
            var removed = this.stopTimes.splice(index, 1);
            this.orderStops();
            return removed;
        },
        changeSequence: function (index, delta) {
            var newPosition = (index + delta > 0) ? index + delta : 0;
            var removed = this.stopTimes.splice(index, 1);
            this.stopTimes.splice(newPosition, 0, removed[0]);
            this.orderStops();
        }

    });

    return module;

}]);
