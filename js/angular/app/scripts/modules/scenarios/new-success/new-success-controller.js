'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosNewsuccessController',
            ['config', '$scope', '$state', '$timeout',
             'OTIIndicatorJobModel', 'OTIMapService', 'OTIScenarioManager', 'OTIScenarioModel',
             'OTITripManager',
            function (config, $scope, $state, $timeout,
                      OTIIndicatorJobModel, OTIMapService, OTIScenarioManager, OTIScenarioModel,
                      OTITripManager) {

    var checkScenarioCreate = function (scenario) {

        var POLLING_TIMEOUT_MS = 2 * 1000;

        $scope.scenario = scenario;

        var checkUpload = function () {
            if ($scope.scenario.isProcessing()) {
                $scope.timeoutId = $timeout(function () {
                    OTIScenarioModel.get({db_name: $scope.scenario.db_name}, function (data) {
                        $scope.scenario = data;
                        checkUpload();
                    }, function () {
                        // Ignore errors here and reschedule check, will eventually timeout
                        $scope.scenario.job_status = 'error';
                    });
                }, POLLING_TIMEOUT_MS);
            } else if ($scope.scenario.isComplete()) {
                OTITripManager.setScenarioDbName($scope.scenario.db_name);
                // Calling setScenario refreshes the layers with the new database configured,
                //  without updating the $scope overlay config (since only the layer params change)
                OTIMapService.setScenario($scope.scenario.db_name);
            }
            OTIScenarioManager.set($scope.scenario);
            $scope.$emit('updateHeight');
        };
        checkUpload();
    };

    $scope.scenario = OTIScenarioManager.get();

    $scope.routes = function () {
        if ($scope.scenario.isComplete()) {
            $state.go('routes');
        }
    };

    $scope.back = function () {
        if ($scope.timeoutId) {
            $timeout.cancel($scope.timeoutId);
        }
        $state.go('list');
    };

    $scope.saving = false;

    $scope.calculateIndicators = function () {
        var job = new OTIIndicatorJobModel({
            city_name: $scope.scenario.name,
            scenario: $scope.scenario.id
        });
        $scope.saving = true;
        job.$save(function () {
            $state.go('list');
        }, function () {
            $scope.saving = false;
        });
    };

    if ($scope.scenario) {
        if ($scope.scenario.job_status) {
            $scope.scenario.$update().then(checkScenarioCreate);
        } else {
            $scope.scenario.$save().then(checkScenarioCreate, function (error) {
                var msg = [
                    'ERROR saving scenario.'
                ];
                console.error(msg, error);
            });
        }
    }

}]);
