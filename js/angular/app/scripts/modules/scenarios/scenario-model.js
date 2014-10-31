'use strict';

angular.module('transitIndicators')
.factory('OTIScenarioModel', ['$resource', function ($resource) {

    var module = $resource('/api/scenarios/:id/', {version: '@id'});

    return module;

}]);