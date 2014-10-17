'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosNewsuccessController',
            ['config', '$scope', '$state', '$stateParams', 'OTIScenariosService',
            function (config, $scope, $state, $stateParams, OTIScenariosService) {

    $scope.scenario = OTIScenariosService.otiScenario;

    $scope.routes = function () {
        $state.go('routes');
    };

    $scope.modes = function () {
        $state.go('routes');
    };

    $scope.back = function () {
        $state.go('new');
    };

}]);
