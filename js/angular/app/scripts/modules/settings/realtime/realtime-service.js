'use strict';

/**
 * Service responsible for getting data on demographics settings page
 */
angular.module('transitIndicators')
.factory('OTIRealTimeService', ['$resource', function($resource) {
    var otirealtimeservice = {};

    otirealtimeservice.realtimeUpload = $resource('/api/real-time/:id/', {}, {
        'update': {
            method: 'PATCH',
            url: '/api/real-time/:id/'
        }
    }, {
        stripTrailingSlashes: false
    });

    // Data problems
    otirealtimeservice.realtimeProblems = $resource('/api/real-time-problems/:id/', null, null, {
        stripTrailingSlashes: false
    });

    return otirealtimeservice;
}
]);