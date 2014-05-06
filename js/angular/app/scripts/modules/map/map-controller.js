'use strict';

angular.module('transitIndicators')
.controller('GTFSMapController',
        ['$scope',
        function ($scope) {

    $scope.leaflet = {
        center: {
            lat: 39.95,
            lng: -75.1667,
            zoom: 12
        },
        defaults: {
            maxZoom: 16
        }
    };

}]);

