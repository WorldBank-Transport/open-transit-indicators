'use strict';

angular.module('transitIndicators')
.controller('GTFSMapController',
        ['$scope',
        function ($scope) {

    $scope.leaflet = {
        center: {
            lat: 51.505,
            lng: -0.09,
            zoom: 8
        },
        defaults: {
            tileLayer: 'http://{s}.tile.opencyclemap.org/cycle/{z}/{x}/{y}.png',
            maxZoom: 14
        }
    };

}]);

