'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosNewController',
            ['config', '$scope', '$state', '$stateParams', 'OTIScenariosService',
            function (config, $scope, $state, $stateParams, OTIScenariosService) {

    $scope.scenario = OTIScenariosService.otiScenario;

    $scope.create = function () {
        // TODO: Validate
        // TODO: Save scenario
        OTIScenariosService.upsertScenario($scope.scenario);
        $state.go('new-success');
    };

    $scope.back = function () {
        $scope.scenario = {};
        $state.go('list');
    };
}]);
