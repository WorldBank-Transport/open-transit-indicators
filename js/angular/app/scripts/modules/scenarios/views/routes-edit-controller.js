'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosRoutesController',
            ['config', '$scope', '$state', '$stateParams', 'OTIScenariosService',
            function (config, $scope, $state, $stateParams, OTIScenariosService) {

    var filterRoutes = function (routes, filterValue) {
        if (filterValue === -1) {
            return routes;
        }
        return _.filter(routes, function (r) {
            return r.routeType === filterValue;
        });
    };

    $scope.scenario = OTIScenariosService.otiScenario;
    $scope.routes = OTIScenariosService.routes;
    $scope.route = {};

    $scope.selectedRouteId = '';
    $scope.selectedRouteType = -1;

    $scope.editRoute = function (routeId) {
        // TODO: Validate
        if (!routeId) {
            return;
        }
        $scope.route = _.find($scope.routes, function (r) {
            return r.routeId === routeId;
        });

        $state.go('routes-edit');
    };

    $scope.newRoute = function () {
        $scope.route = new OTIScenariosService.Route();

        $state.go('routes-edit');
    };

    $scope.back = function () {
        $scope.scenario = {};
        $state.go('new-success');
    };

    $scope.$watch('selectedRouteType', function (newValue) {
        $scope.routes = filterRoutes(OTIScenariosService.routes, newValue);
    });
}]);
