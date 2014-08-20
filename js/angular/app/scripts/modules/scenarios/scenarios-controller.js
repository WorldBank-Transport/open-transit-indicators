'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosController',
            ['$scope',
            function ($scope) {

    $scope.leaflet = angular.extend({}, $scope.leafletDefaults);
}]);