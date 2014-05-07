'use strict';

angular.module('transitIndicators')
.controller('OTIRealtimeController',
        ['$scope',
        function ($scope) {

    $scope.init = function () {
        console.log('Loaded Realtime Controller');
    };

}]);
