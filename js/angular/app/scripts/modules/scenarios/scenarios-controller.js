'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosController',
            ['$scope',
            function ($scope) {

    $scope.pageno = 0;

    $scope.updateLeafletOverlays({});
}]);
