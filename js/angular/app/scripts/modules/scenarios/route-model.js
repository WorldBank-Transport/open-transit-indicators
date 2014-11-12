'use strict';

angular.module('transitIndicators')
.factory('OTIRouteModel',
         ['$resource', 'OTIUIDService',
         function ($resource, OTIUIDService) {

    var module = $resource('/gt/scenarios/:db_name/routes/:routeId', {
        db_name: '@db_name',
        routeId: '@id'
    }, {
        create: {
            method: 'POST',
            url: '/gt/scenarios/:db_name/routes',
            params: {
                db_name: '@db_name'
            }
        }
    });

    return module;
}]);