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