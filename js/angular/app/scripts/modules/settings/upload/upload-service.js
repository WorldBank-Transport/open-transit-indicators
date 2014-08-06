'use strict';

/**
 * Service responsible for getting data on uploads/problems
 * for OTI data
 */
angular.module('transitIndicators')
.factory('OTIUploadService', ['$resource', function($resource) {
    var otiuploadservice = {};

    // OTI gtfs data
    otiuploadservice.gtfsUploads = $resource('/api/gtfs-feeds/:id', {}, {
        'update': {
            method: 'PATCH',
            url: '/api/gtfs-feeds/:id'
        }
    });

    // OTI data problems
    otiuploadservice.gtfsProblems = $resource('/api/gtfs-feed-problems/:id');


    // OTI OpenStreetMap data import
    otiuploadservice.osmImport = $resource('/api/osm-data/:id', {id: '@id'}, {
        'save': {
            method: 'POST',
            url: '/api/osm-data/ '
        }
    });

    // OTI OpenStreetMap data import problems
    otiuploadservice.osmImportProblems = $resource('/api/osm-data-problems/:id');

    return otiuploadservice;
}
]);
