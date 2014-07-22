'use strict';

angular.module('transitIndicators')
.controller('OTIAuthController',
        ['config', '$scope', '$state', 'authService',
        function (config, $scope, $state, authService) {

    var leaflet = {
        bounds: config.worldExtent,
        center: {
            lat: 20.0,
            lng: 0.0,
            zoom: 2
        }
    };
    $scope.leaflet = angular.extend(config.leaflet, leaflet);

    $scope.auth = {};

    $scope.alerts = [];
    $scope.addAlert = function(alertObject) {
        $scope.alerts.push(alertObject);
    };
    $scope.closeAlert = function(index) {
        $scope.alerts.splice(index, 1);
    };

    $scope.authenticate = function() {
        $scope.alerts = [];
        $scope.authenticated = authService.authenticate($scope.auth);
        $scope.authenticated.then(function(result) {
            if (result.isAuthenticated) {
                $state.go('map');
            } else {
                handleError(result);
            }
        }, function (result) {
            handleError(result);
        });
    };

    var handleError = function (result) {
        $scope.auth.failure = true;
        var msg = result.error || (result.status + ': Unknown Error.');
        $scope.addAlert({
            type: 'danger',
            msg: msg
        });
    };

}]);
