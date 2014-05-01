'use strict';

/**
 * Service responsible for getting data on uploads/problems
 * for GTFS data
 */ 
angular.module('transitIndicators')
.factory('GTFSUploadService', ['$resource', function($resource) {
    var gtfsuploadservice = {};

    // GTFS data
    gtfsuploadservice.gtfsUploads = $resource('/api/gtfs-feeds/:id');

    // GTFS data problems
    gtfsuploadservice.gtfsProblems = $resource('/api/gtfs-feed-problems/:id');

    return gtfsuploadservice;
}
]);
