'use strict';

/**
 * Resource for indicator jobs
 */
angular.module('transitIndicators')
.factory('OTIIndicatorJobModel', ['$resource',
        function ($resource) {

    var module = $resource('/api/indicator-jobs/:id/', {id: '@id'}, {
        search: {
            method: 'GET',
            isArray: true,
            url: '/api/indicator-jobs/'
        },
        latest: {
            method: 'GET',
            idArray: false,
            url: '/api/latest-calculation-job/'
        }
    }, {
        stripTrailingSlashes: false
    });

    return module;
}]);
