'use strict';

angular.module('transitIndicators')
.factory('OTIIndicatorModel', ['$resource',
        function ($resource) {

    var module = $resource('/api/indicators/:id/', {id: '@id'}, {
        'update': {
            method: 'PATCH',
            url: '/api/indicators/:id/'
        },
        'search': {
            method: 'GET',
            url: '/api/indicators/',
            isArray: true,
            cache: true
        },
        'csv': {
            method: 'GET',
            url: '/api/indicators/',
            transformResponse: function(data) {
                var csv;
                if (data) {
                    csv = new Blob([data], {
                        type: 'application/csv'
                    });
                }
                return {
                    csv: csv
                };
            }
        },
        'geotiff': {
            method: 'GET',
            url: '/gt/travelshed/jobs/geotiff',
            responseType: 'arraybuffer',
            cache: false,
            transformResponse: function(data) {
                var geotiff;
                if (data) {
                    geotiff = new Blob([data], {
                        type: 'application/octet-stream'
                    });
                }
                return {
                    geotiff: geotiff
                };
            }
        }
    }, {
        stripTrailingSlashes: false
    });

    angular.extend(module.prototype, {});

    return module;

}]);
