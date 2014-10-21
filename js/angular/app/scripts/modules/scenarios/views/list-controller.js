'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosListController',
            ['config', '$scope', '$state', '$stateParams', 'OTIScenariosService',
            function (config, $scope, $state, $stateParams, OTIScenariosService) {

    OTIScenariosService.otiScenario = {};

    $scope.create = function () {
        OTIScenariosService.otiScenario = new OTIScenariosService.Scenario();
        $state.go('new');
    };

    $scope.edit = function (index) {
        OTIScenariosService.otiScenario = $scope.scenarios[index];
        $state.go('new');
    };
}]);
