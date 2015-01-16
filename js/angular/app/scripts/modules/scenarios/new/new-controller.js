'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosNewController',
            ['config', '$scope', '$state', '$stateParams', 'OTIScenarioManager',
            function (config, $scope, $state, $stateParams, OTIScenarioManager) {

    $scope.scenario = OTIScenarioManager.get();
    $scope.filteredSamplePeriods = _.omit($scope.samplePeriodI18N, 'alltime');

    $scope.isEditing = !!($scope.scenario.sample_period);

    $scope.create = function () {
        // TODO: UI feedback for validation -- all fields are required
        if ($scope.newScenario.$valid) {
            // TODO: Save scenario
            OTIScenarioManager.set($scope.scenario);
            $state.go('new-success');
        }
    };

    $scope.back = function () {
        OTIScenarioManager.clear();
        $state.go('list');
    };
}]);
