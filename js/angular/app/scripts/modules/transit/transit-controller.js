'use strict';
angular.module('transitIndicators')
.controller('OTITransitController',
            ['config', '$scope',
            function (config, $scope) {

    $scope.leaflet = angular.extend({}, $scope.leafletDefaults);

}]);