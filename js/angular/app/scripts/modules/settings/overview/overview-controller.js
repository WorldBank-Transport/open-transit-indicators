'use strict';

angular.module('transitIndicators')
.controller('GTFSOverviewController',
        ['$scope',
        function ($scope) {

    $scope.init = function () {
        console.log('Loaded Overview Controller');
    };

}]);
