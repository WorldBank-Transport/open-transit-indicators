'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosNewsuccessController',
            ['config', '$scope', '$state', 'OTIScenarioManager',
            function (config, $scope, $state, OTIScenarioManager) {

    $scope.scenario = OTIScenarioManager.get();

    $scope.routes = function () {
        $state.go('routes');
    };

    $scope.modes = function () {
        $state.go('routes');
    };

    $scope.back = function () {
        $state.go('list');
    };

}]);
