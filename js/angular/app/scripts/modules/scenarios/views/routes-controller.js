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

    $scope.routes = OTIScenariosService.getRoutes();
    $scope.filteredRoutes = [];
    $scope.scenario = OTIScenariosService.otiScenario;

    $scope.selectedRouteId = '';
    $scope.selectedRouteType = -1;

    $scope.editRoute = function (routeId) {
        // TODO: Validate
        if (!routeId) {
            return;
        }
        OTIScenariosService.otiRoute = _.find($scope.routes, function (r) {
            return r.routeId === routeId;
        });
        $state.go('routes-edit');
    };

    $scope.newRoute = function () {
        OTIScenariosService.otiRoute = new OTIScenariosService.Route();
        $state.go('routes-edit');
    };

    $scope.back = function () {
        $scope.scenario = {};
        $state.go('new-success');
    };

    $scope.$watch('selectedRouteType', function (newValue) {
        $scope.filteredRoutes = filterRoutes($scope.routes, newValue);
    });
}]);
