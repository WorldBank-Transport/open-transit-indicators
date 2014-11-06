'use strict';

/**
 * Service responsible for getting data on uploads/problems
 * for GTFS data
 */
angular.module('transitIndicators')
.factory('OTISettingsService',
         ['$http', '$q', '$resource',
         function($http, $q, $resource) {

    var settingsService = {};

    // Urls used throughout the 'settings' portion of OTI
    var _settings_urls = {
      'gtfs': '/api/gtfs-feeds/:id/',
      'gtfsProblems': '/api/gtfs-feed-problems/:id/',
      'osm': '/api/osm-data/:id/',
      'osmProblems': '/api/osm-data-problems/:id/',
      'boundaries': '/api/boundaries/:id/',
      'boundaryProblems': '/api/boundary-problems/:id/',
      'samplePeriod': '/api/sample-periods/:type/',
      'demographics': '/api/demographics/:id/',
      'demogProblems': '/api/demographics-problems/:id/',
      'demogConfig': '/api/config-demographic/',
      'realtime': '/api/real-time/:id/',
      'realtimeProblems': '/api/real-time-problems/:id/',
      'config': '/api/config/:id/'
    };

    // Resources to be used throughout settings
    // GTFS resources
    settingsService.gtfsUploads = $resource(_settings_urls.gtfs, {}, {
        'update': {
            method: 'PATCH',
            url: _settings_urls.gtfs
        }
    }, {
        stripTrailingSlashes: false
    });
    settingsService.gtfsProblems = $resource(_settings_urls.gtfsProblems, null, null, {
        stripTrailingSlashes: false
    });

    settingsService.osmData = $resource(_settings_urls.osm, {id: '@id'}, {
        'save': {
            method: 'POST',
            url: _settings_urls.osm
        }
    }, {
        stripTrailingSlashes: false
    });
    settingsService.osmProblems = $resource(_settings_urls.osmProblems, null, null, {
        stripTrailingSlashes: false
    });

    // OTI city name
    settingsService.cityName = $resource('/api/city-name/', {}, {
        'save': {
            method: 'POST',
            url: '/api/city-name/ '
        }
    }, {
        stripTrailingSlashes: false
    });


    // Boundary resources
    settingsService.boundaryUploads = $resource(_settings_urls.boundaries, {}, {
        update: {
            method: 'PATCH',
            url: _settings_urls.boundaries
        }
    }, {
        stripTrailingSlashes: false
    });
    settingsService.boundaryProblems = $resource(_settings_urls.boundaryProblems, null, null, {
        stripTrailingSlashes: false
    });


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
            url: '/api/demographics/:id/load/',
            params: {
                population_metric_1: '@population_metric_1',
                population_metric_2: '@population_metric_2'
            }
        }
    }, {
        stripTrailingSlashes: false
    });
    settingsService.demogProblems = $resource(_settings_urls.demogProblems, null, null, {
        stripTrailingSlashes: false
    });
    settingsService.demogConfigs = $resource(_settings_urls.demogConfig, null, null, {
        stripTrailingSlashes: false
    });

    // Real-time (i.e. observed) resources
    settingsService.realtimes = $resource(_settings_urls.realtime, {}, {
        'update': {
            method: 'PATCH',
            url: _settings_urls.realtime
        }
    }, {
        stripTrailingSlashes: false
    });
    settingsService.realtimeProblems = $resource(_settings_urls.realtimeProblems, null, null, {
        stripTrailingSlashes: false
    });

    // User entered config resources
    settingsService.configs = $resource(_settings_urls.config, null, {
        update: {
            method: 'PATCH',
            url: _settings_urls.config
        }
    }, {
        stripTrailingSlashes: false
    });
    settingsService.samplePeriods = $resource(_settings_urls.samplePeriod, {type: '@type'}, {
        update: {
            method: 'PUT',
            url:_settings_urls.samplePeriod
        }
    }, {
        stripTrailingSlashes: false
    });

    settingsService.checkStatus = {};
    settingsService.checkStatus.gtfs = function() {
        return $http({method: 'GET', url: '/api/gtfs-feeds/'});
    };
    settingsService.checkStatus.config = function() {
        return $http({method: 'GET', url: '/api/config/'});
    };

    return settingsService;
}]);
