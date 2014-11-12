'use strict';

angular.module('transitIndicators')
.factory('OTITripModel', ['$resource', function ($resource) {

    function OTITripValidationError() {
        this.isValid = true;
        this.errors = [];
    }

    /** Add an error to the object, setting isValid to false
     *
     *  @param messageKey String An angular ui translate key to pass to UI for translation/display
     */
    OTITripValidationError.prototype.addError = function (messageKey) {
        this.isValid = false;
        this.errors.push(messageKey);
    };

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

        // This function returns km of line-length, given lat/longs using the haversine formula
        // https://en.wikipedia.org/wiki/Haversine_formula
        // http://stackoverflow.com/questions/27928/how-do-i-calculate-distance-between-two-latitude-longitude-points
        calculateDistance: function() {
            function getDistanceFromLatLonInKm(line) {
                var R = 6371; // Radius of the earth in km
                var dLat = deg2rad(line[0][0] - line[1][0]);  // deg2rad below
                var dLon = deg2rad(line[0][1] - line[1][1]);
                var a =
                    Math.sin(dLat/2) * Math.sin(dLat/2) +
                    Math.cos(deg2rad(line[0][0])) * Math.cos(deg2rad(line[1][0])) *
                    Math.sin(dLon/2) * Math.sin(dLon/2);
                var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
                var d = R * c; // Distance in km
                return d;
            }

            function deg2rad(deg) {
                return deg * (Math.PI/180);
            }

            var latlngs = this.shape.coordinates;
            var lines = _.zip(_.initial(latlngs), _.tail(latlngs));

            this.shape.distance = _.chain(lines)
                                 .map(getDistanceFromLatLonInKm)
                                 .foldl(function(acc, num) { return acc + num; }, 0)
                                 .value();
            return this.shape.distance;

        },

        makeShape: function (newCoords) {
            // This function takes an array of coordinates (itself of the form [lat, long])
            // and determines, for each pairing of head to tail, the shortest path which connects
            // said head and tail. It then creates a new set of coordinates which joins together
            // old and new coordinates as a line with the shortest possible, inferred connection

            function eucDistance(p, q) {
              return Math.sqrt(Math.pow(p[0] - q[0], 2) + Math.pow(p[1] - q[1], 2));
            }

            var oldCoords = this.shape.coordinates;
            if (oldCoords.length > 0) {
                var head = oldCoords[0],
                    last = oldCoords[oldCoords.length-1],
                    newHead = newCoords[0],
                    newLast = newCoords[newCoords.length-1],
                    headHead = [eucDistance(head, newHead), 'headHead'],
                    headLast = [eucDistance(head, newLast), 'headLast'],
                    lastHead = [eucDistance(last, newHead), 'lastHead'],
                    lastLast = [eucDistance(last, newLast), 'lastLast'],
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
            this.calculateDistance();
        },

        clearShape: function () {
            this.shape.coordinates = [];
            this.calculateDistance();
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
        },

        validate: function () {
            var validation = new OTITripValidationError();

            // Validate shape exists
            if (!(this.shape && this.shape.coordinates && this.shape.coordinates.length)) {
                validation.addError('SCENARIO.ROUTE_ERRORS.NO_SHAPES');
            }

            // Validate stopTimes exists
            if (!(this.stopTimes && this.stopTimes.length)) {
                validation.addError('SCENARIO.ROUTE_ERRORS.NO_STOPTIMES');
            }

            // Validate each stopTime has a stop
            var missingStops = false;
            _.each(this.stopTimes, function (stopTime) {
                if (!(stopTime.stop && stopTime.stop.lat && stopTime.stop.long)) {
                    missingStops = true;
                }
            });
            if (missingStops) {
                validation.addError('SCENARIO.ROUTE_ERRORS.MISSING_STOPS');
            }

            // Validate at least one frequency exists
            var freq = this.getFrequency(0);
            if (!(freq && freq.headway && freq.start && freq.end)) {
                validation.addError('SCENARIO.ROUTE_ERRORS.NO_FREQUENCY');
            }

            return validation;
        }
    });

    return module;

}]);
