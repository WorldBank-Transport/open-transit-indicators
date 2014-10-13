'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosRouteeditController',
            ['config', '$scope', '$state', '$stateParams', 'OTIScenariosService', 'OTIDrawService',
            function (config, $scope, $state, $stateParams, OTIScenariosService, OTIDrawService) {

    $scope.scenario = OTIScenariosService.otiScenario;
    $scope.selectedRouteType = 0;
    $scope.route = OTIScenariosService.otiRoute;

    OTIDrawService.reset();

    $scope.continue = function () {
        // TODO: UI Validation
        if ($scope.newRoute.$valid) {
            OTIScenariosService.upsertRoute($scope.route);
            $state.go('route-stops');
        }
    };

    $scope.back = function () {
        OTIScenariosService.otiRoute = {};
        $state.go('routes');
    };

}]);
