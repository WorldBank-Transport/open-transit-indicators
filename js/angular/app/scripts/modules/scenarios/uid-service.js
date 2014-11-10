'use strict';

angular.module('transitIndicators')
.factory('OTIUIDService',
        [function () {

    var module = {};

    module.getId = function() {
        var id = [];
        var possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';

        for (var i = 0; i < 8; i++) {
            id.push(possible.charAt(Math.floor(Math.random() * possible.length)));
        }
        return id.join('');
    };

    return module;
}]);
