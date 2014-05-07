'use strict';

angular.module('transitIndicators')
.controller('OTIUsersController',
        ['$scope',
        function ($scope) {

    $scope.init = function () {
        console.log('Loaded Users Controller');
    };

}]);
