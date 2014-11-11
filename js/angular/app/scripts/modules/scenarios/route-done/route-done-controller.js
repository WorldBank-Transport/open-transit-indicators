'use strict';

angular.module('transitIndicators')
.controller('OTIScenariosRoutedoneController',
            ['$modal', '$scope', '$state', 'OTIScenarioManager', 'OTITripManager',
            function ($modal, $scope, $state, OTIScenarioManager, OTITripManager) {

    // LOCAL

    var scenario = null;
    var validation = null;
    var statuses = {
        SAVING: 'saving',
        SAVED: 'saved',
        ERROR: 'error',
        INVALID: 'invalid'
    };
    var initialize = function () {
        $scope.statuses = statuses;
        $scope.status = statuses.INVALID;

        scenario = OTIScenarioManager.get();
        $scope.trip = OTITripManager.get();
        $scope.validation = $scope.trip.validate();
        if ($scope.validation.isValid) {
            $scope.status = statuses.SAVING;
            $scope.$emit('updateHeight');
            var dfd = $scope.trip.$save({db_name: scenario.db_name});
            dfd.then(function () {
                $scope.status = statuses.SAVED;
            }, function () {
                $scope.status = statuses.ERROR;
            }).finally(function () {
                $scope.$emit('updateHeight');
            });
        }
    };

    var openConfirmModal = function () {
        return $modal.open({
            templateUrl: 'scripts/modules/indicators/yes-no-modal-partial.html',
            controller: 'OTIYesNoModalController',
            windowClass: 'yes-no-modal-window',
            resolve: {
                getMessage: function() {
                    return 'SCENARIO.CONFIRM_NAVIGATION';
                },
                getList: function () {
                    return null;
                }
            }
        });
    };

    // $SCOPE

    $scope.goWithConfirmationTo = function (toState) {
        if ($scope.status === statuses.INVALID) {
            var modal = openConfirmModal(toState);
            modal.result.then(function () {
                $state.go(toState);
            });
        } else {
            $state.go(toState);
        }
    };

    $scope.back = function () {
        $state.go('route-times');
    };

    // INIT
    initialize();
}]);
