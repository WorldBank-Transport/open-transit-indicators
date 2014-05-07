'use strict';

angular.module('transitIndicators')
.controller('OTIConfigurationController',
        ['$scope',
        function ($scope) {

    $scope.init = function () {
        console.log('Loaded Configuration Controller');
    };

}]);
