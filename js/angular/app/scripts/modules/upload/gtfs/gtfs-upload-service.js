'use strict';

/**
 * Service responsible for getting data on uploads/problems
 * for GTFS data
 */ 
angular.module('transitIndicators')
.factory('GTFSUploadService', ['$resource', function($resource) {
    var gtfsuploadservice = {};

    gtfsuploadservice.STATUS = {
        START: -1,
        UPLOADERROR: -2,
        PROCESSING: 100,
        DONE: 101
    };

    // GTFS data
    gtfsuploadservice.gtfsUploads = $resource('/api/gtfs-feeds/:id', {}, {
        'update': {
            method: 'PATCH',
            url: '/api/gtfs-feeds/:id'
        }
    });

    // GTFS data problems
    gtfsuploadservice.gtfsProblems = $resource('/api/gtfs-feed-problems/:id');

    return gtfsuploadservice;
}
]);
