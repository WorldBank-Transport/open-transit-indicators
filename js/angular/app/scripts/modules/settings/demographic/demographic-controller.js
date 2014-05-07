'use strict';

angular.module('transitIndicators')
.controller('OTIDemographicController',
        ['$scope',
        function ($scope) {

    $scope.init = function () {
        console.log('Loaded Demographic Controller');
    };

}]);
