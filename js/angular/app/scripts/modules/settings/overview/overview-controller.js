'use strict';

angular.module('transitIndicators')
.controller('OTIOverviewController',
        ['$scope',
        function ($scope) {

    $scope.init = function () {
        console.log('Loaded Overview Controller');
    };

}]);
