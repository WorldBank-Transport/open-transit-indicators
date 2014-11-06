'use strict';

angular.module('transitIndicators').
factory('OTIUserService', ['$resource', '$q',
        function ($resource, $q) {

    var userService = {};

    userService.User = $resource('/api/users/:id/', {id: '@id'}, {
        'update': {
            method: 'PATCH',
            url: '/api/users/:id/'
        },
        'changePassword' : {
            method: 'POST',
            url: '/api/users/:id/change_password/'
        },
        'resetPassword' : {
            method: 'POST',
            url: '/api/users/:id/reset_password/'
        }
    }, {
        stripTrailingSlashes: false
    });

    userService.getUser = function (userId) {
        var dfd = $q.defer();
        var result = userService.User.get({id: userId}, function () {
            dfd.resolve(result);
        });
        return dfd.promise;
    };

    return userService;
}]);
