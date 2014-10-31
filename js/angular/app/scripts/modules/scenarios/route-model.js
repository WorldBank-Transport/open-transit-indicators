'use strict';

angular.module('transitIndicators')
.factory('OTIRouteModel', ['$resource', function ($resource) {

    var module = $resource('/gt/scenarios/:db_name/routes', {db_name: '@db_name'});

    return module;

}]);