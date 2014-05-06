'use strict';

angular.module('transitIndicators')
.controller('GTFSMapController',
        ['config', '$scope',
        function (config, $scope) {

    var leaflet = {};

    $scope.leaflet = angular.extend(config.leaflet, leaflet);

}]);

