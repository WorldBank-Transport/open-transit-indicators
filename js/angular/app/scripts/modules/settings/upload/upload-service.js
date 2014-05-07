'use strict';

/**
 * Service responsible for getting data on uploads/problems
 * for OTI data
 */
angular.module('transitIndicators')
.factory('OTIUploadService', ['$resource', function($resource) {
    var otiuploadservice = {};

    // OTI data
    otiuploadservice.gtfsUploads = $resource('/api/gtfs-feeds/:id', {}, {
        'update': {
            method: 'PATCH',
            url: '/api/gtfs-feeds/:id'
        }
    });

    // OTI data problems
    otiuploadservice.gtfsProblems = $resource('/api/gtfs-feed-problems/:id');

    return otiuploadservice;
}
]);
