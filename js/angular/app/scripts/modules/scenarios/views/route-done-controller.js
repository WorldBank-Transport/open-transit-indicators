'use strict';

angular.module('transitIndicators')
.controller('OTIScenariosRoutedoneController',
            ['$scope', '$state', 'OTIScenariosService',
            function ($scope, $state, OTIScenariosService) {

    // LOCAL

    // $SCOPE

    $scope.scenario = OTIScenariosService.otiScenario;
    $scope.route = OTIScenariosService.otiRoute;

    $scope.list = function () {
        $state.go('list');
    };

    $scope.routes = function () {
        $state.go('routes');
    };

    $scope.back = function () {
        $state.go('route-times');
    };


    // INIT

}]);
