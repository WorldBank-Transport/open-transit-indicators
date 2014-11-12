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
            isArray: true
        }
    }, {
        stripTrailingSlashes: false
    });

    angular.extend(module.prototype, {});

    return module;

}]);
