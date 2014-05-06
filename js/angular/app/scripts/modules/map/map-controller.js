'use strict';

angular.module('transitIndicators')
.controller('GTFSMapController',
        ['config', '$scope', '$state',
        function (config, $scope, $state) {

    var leaflet = {};
    $scope.$state = $state;

    $scope.leaflet = angular.extend(config.leaflet, leaflet);

}]);

