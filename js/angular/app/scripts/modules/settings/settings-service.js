'use strict';

/**
 * Service responsible for getting data on uploads/problems
 * for GTFS data
 */
angular.module('transitIndicators')
.factory('OTISettingsService',
         ['$http', '$q',
         function($http, $q) {

    var settingsService = {};

    // Urls used throughout the 'settings' portion of OTI
    var _settings_urls = {
      'gtfs': '/api/gtfs-feeds/:id',
      'gtfsProblems': '/api/gtfs-feed-problems/:id',
      'osm': '/api/osm-data/:id',
      'osmProblems': '/api/osm-data-problems/:id',
      'boundaries': '/api/boundaries/:id',
      'boundaryProblems': '/api/boundary-problems/:id',
      'samplePeriod': '/api/sample-periods/:type/ ',
      'demographics': '/api/demographics/:id/ ',
      'demogProblems': '/api/demographics-problems/:id/ ',
      'demogConfig': '/api/config-demographic/ ',
      'realtime': '/api/real-time/:id/ ',
      'realtimeProblems': '/api/real-time-problems/:id/ ',
      'config': '/api/config/:id/ '
    };

    // Resources to be used throughout settings
    // GTFS resources
    settingsService.gtfsUploads = $resource(_settings_urls.gtfs, {}, {
        'update': {
            method: 'PATCH',
            url: _settings_urls.gtfs
        }
    });
    settingsService.gtfsProblems = $resource(_settings_urls.gtfsProblems);

    settingsService.osmData = $resource(_settings_urls.osm, {id: '@id'}, {
        'save': {
            method: 'POST',
            url: _settings_urls.osm
        }
    });
    settingsService.osmProblems = $resource(_settings_urls.osmProblems);

    // Boundary resources
    settingsService.boundaryUploads = $resource(_settings_urls.boundaries, {}, {
        update: {
            method: 'PATCH',
            url: _settings_urls.boundaries
        }
    });
    settingsService.boundaryProblems = $resource(_settings_urls.boundaryProblems);


    // Demographics resources
    settingsService.demographics = $resource(_settings_urls.demographics, {}, {
        'update': {
            method: 'PATCH',
            url: _settings_urls.demographics
        },
        // After a load POST, is_valid == true and is_loaded == true if success
        // otherwise, is_valid reverts to false
        'load': {
            method: 'POST',
            url: '/api/demographics/:id/load/ ',
            params: {
                population_metric_1: '@population_metric_1',
                population_metric_2: '@population_metric_2'
            }
        }
    });
    settingsService.demogProblems = $resource(_settings_urls.demogProblems);
    settingsService.demogConfigs = $resource(_settings_urls.demogConfig);

    // Real-time (i.e. observed) resources
    settingsService.realtimes = $resource(_settings_urls.realtime, {}, {
        'update': {
            method: 'PATCH',
            url: _settings_urls.realtime
        }
    });
    settingsService.realtimeProblems = $resource(_settings_urls.realtimeProblems);

    // User entered config resources
    settingsService.configs = $resource(_settings_urls.config);
    settingsService.samplePeriods = $resource(_settings_urls.samplePeriod, {type: '@type'}, {
        update: {
            method: 'PUT',
            url:_settings_urls.samplePeriod
        }
    });

    settingsService.checkStatus = {};
    settingsService.checkStatus.gtfs = function() {
        return $http({method: 'GET', url: '/api/gtfs-feeds'});
    };
    settingsService.checkStatus.config = function() {
        return $http({method: 'GET', url: '/api/config'});
    };


    settingsService.STATUS = {
        START: -1,
        UPLOADERROR: -2,
        PROCESSING: 100,
        DONE: 101
    };


    return settingsService;
}]);
