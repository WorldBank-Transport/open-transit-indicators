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

        // if index undefined, add to end of array
        addShape: function (shape, index) {

        },
        removeShape: function (index) {

        },
        orderStops: function() {
            this.stopTimes = _.map(this.stopTimes, function(stopTime, index) {
                stopTime.stopSequence = index + 1;
                return stopTime;
            });
        },
        fixStopOrder: function() {
            this.stopTimes = function() {
                var newOrder = [];
                _.each(this.stopTimes, function(stopTime) {
                    newOrder[stopTime.stopSequence-1] = stopTime;
                });
                return newOrder;
            };
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
            console.log(delta)
            var newPosition = (index + delta > 0) ? index + delta : 0;
            console.log(newPosition)
            var removed = this.stopTimes.splice(index, 1);
            console.log(removed)
            this.stopTimes.splice(newPosition, 0, removed[0]);
            this.orderStops();
        }

    });

    return module;

}]);
