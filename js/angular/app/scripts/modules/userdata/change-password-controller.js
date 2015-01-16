'use strict';
angular.module('transitIndicators')
.controller('OTIUserdataChangePasswordController',
            ['$scope', '$modalInstance', 'authService', 'OTIUserService',
            function ($scope, $modalInstance, authService, OTIUserService) {

    $scope.password = '';
    $scope.passwordconfirm = '';
    $scope.saving = false;

    $scope.cancel = function () {
        $modalInstance.dismiss();
    };

    $scope.save = function () {
        $scope.saving = true;
        var userid = authService.getUserId();
        OTIUserService.User.changePassword({ id: userid }, { password: $scope.password },
        function () { // then
            $modalInstance.close();
        }, function () { // else
            $scope.saving = false;
        });
    };

}]);
