'use strict';

angular.module('transitIndicators')
.factory('OTITripModel', ['$resource', function ($resource) {

    // This takes the function signature of the angular transformResponse functions
    // @param tripsString String data returned from endpoint
    // @param headers Headers sent with the response
    // @return First trip in each group of trips as Array[String]
    var transformTrips = function (tripsString) {
        var trips2dArray = angular.fromJson(tripsString);
        var trips = [];
        if (trips2dArray && trips2dArray.length) {
            for (var i = 0; i < trips2dArray.length; i++) {
                var tripSet = trips2dArray[i];
                if (tripSet && tripSet.length) {
                    trips.push(tripSet[0]);
                }
            }
        }
        return trips;
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

        // if index undefined, add to end of array
        addShape: function (shape, index) {

        },
        removeShape: function (index) {

        },
        // if index undefined, add to end of array
        addStopTime: function (stopTime, index) {

        },
        removeStopTime: function (index) {

        }

    });

    return module;

}]);