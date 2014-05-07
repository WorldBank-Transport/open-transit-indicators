'use strict';

/**
 * Service responsible for getting data on uploads/problems
 * for GTFS data
 */ 
angular.module('transitIndicators')
.factory('GTFSSettingsService', ['$resource', function($resource) {
    var settingsService = {};

    settingsService.STATUS = {
        START: -1,
        UPLOADERROR: -2,
        PROCESSING: 100,
        DONE: 101
    };

    return settingsService;
}
]);
