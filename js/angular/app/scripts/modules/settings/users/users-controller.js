'use strict';

angular.module('transitIndicators')
/**
 * Angular filter to present user role
 *
 * @param input <boolean> represents if user is staff (administrator)
 */
.filter('user_type', function() {
    return function(bool) {
        return bool ? 'Administrator' : 'User';
    };
})
.controller('OTIUsersController',
        ['$scope', '$modal', 'OTIUserService',
        function ($scope, $modal, OTIUserService) {

    // Instantiate variables/defaults
    $scope.addingUser = null;
    $scope.userRoles = [{label:'User', key: false}, {label:'Administrator', key: true}];

    OTIUserService.User.query({}, function(data){
        $scope.users = data;
    });

    /**
     * Helper function to handle showing create user form
     */
    $scope.addUser = function() {
        $scope.userCreateForm.role = $scope.userRoles[0];
        $scope.addingUser = true;
    };

    /**
     * Function to create a user
     */
    $scope.createUser = function() {
        OTIUserService.User.save(
            {username: $scope.userCreateForm.username,
             password: $scope.userCreateForm.password,
             is_staff: $scope.userCreateForm.role.key},
            function(user) {
                $scope.users.push(user);
                $scope.cancelUserCreate();
            },
            function(error) {
                console.log(error);
            }
        );
    };

    /**
     * Function to clean + hide user create form
     */
    $scope.cancelUserCreate = function() {
        $scope.userCreateForm.username = null;
        $scope.userCreateForm.password = null;
        $scope.userCreateForm.confirm_password = null;
        $scope.userCreateForm.role = $scope.userRoles[0];
        $scope.addingUser = false;
    };

    /**
     * Function to open modal to confirm resetting password
     *
     * @param user <object> user resource
     */
    $scope.resetPasswordModal = function(user) {
        var resetPasswordModalInstance = $modal.open({
            templateUrl: '/scripts/modules/settings/users/reset-password-modal-partial.html',
            controller: 'OTIPasswordResetController',
            resolve: {
                user: function() {
                    return user;
                }
            }

        });
    };

    /**
     * Function to open modal to confirm deleting user
     *
     * @param user <object> user resource
     */
    $scope.deleteUserModal = function(user) {
        var deleteUserModalInstance = $modal.open({
            templateUrl: '/scripts/modules/settings/users/delete-user-modal-partial.html',
            controller: 'OTIDeleteUserController',
            resolve: {
                user: function() {
                    return user;
                }
            }
        });
        deleteUserModalInstance.result.then( function (user) {
            $scope.users = _.without($scope.users, user);
        });
    };

}]);

/**
 *  Controller to handle modal instance for password resets
 */
angular.module('transitIndicators')
.controller('OTIPasswordResetController',
        ['$scope', '$modalInstance', 'user',
        function($scope, $modalInstance, user) {

    $scope.newPassword = null;
    $scope.user = user;

    $scope.resetPassword = function() {
        $scope.user.$resetPassword( function(user) {
            $scope.newPassword = user.password;
        },
        function(error) {
            console.log(error);
        });
    };

    $scope.close = function() {
        $modalInstance.dismiss('cancel');
    };

}]);

/**
 *  Controller to handle modal instance for user deletion
 */
angular.module('transitIndicators')
.controller('OTIDeleteUserController',
        ['$scope', '$modalInstance', 'user',
        function($scope, $modalInstance, user) {

    $scope.user = user;

    $scope.deleteUser = function() {
        $scope.user.$delete( function (user) {
            $modalInstance.close(user);
        },
        function(error) {
            console.log(error);
        });
    };

    $scope.close = function() {
        $modalInstance.dismiss('cancel');
    };

}]);
