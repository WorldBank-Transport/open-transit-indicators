'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosRouteseditController',
            ['config', '$scope', '$state', '$stateParams', 'OTIScenariosService',
            function (config, $scope, $state, $stateParams, OTIScenariosService) {

    $scope.scenario = OTIScenariosService.otiScenario;
    $scope.selectedRouteType = 0;
    $scope.route = OTIScenariosService.otiRoute;
    console.log($scope.route);

    $scope.continue = function () {
        // TODO: UI Validation
        if ($scope.newRoute.$valid) {
            OTIScenariosService.upsertRoute($scope.route);
            $state.go('routes-stops');
        }
    };

    $scope.back = function () {
        OTIScenariosService.otiRoute = {};
        $state.go('routes');
    };

}]);
