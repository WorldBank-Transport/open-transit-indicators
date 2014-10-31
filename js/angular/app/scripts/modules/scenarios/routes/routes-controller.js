'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosRoutesController',
            ['$scope', '$state', 'OTIScenarioManager', 'OTIRouteManager',
            function ($scope, $state, OTIScenarioManager, OTIRouteManager) {

    $scope.filteredRoutes = [];
    $scope.scenario = OTIScenarioManager.get();
    $scope.routes = [];
    OTIRouteManager.list({db_name: $scope.scenario.db_name}).then(function (routes) {
        $scope.routes = routes;
    });

    $scope.selectedRouteId = '';
    $scope.routeType = {
        selected: -1
    };

    $scope.editRoute = function (routeId) {
        // TODO: Validate
        if (!routeId) {
            return;
        }
        var route = _.find($scope.routes, function (r) {
            return r.routeId === routeId;
        });
        OTIRouteManager.set(route);
        $state.go('route-edit');
    };

    $scope.newRoute = function () {
        OTIRouteManager.create();
        $state.go('route-edit');
    };

    $scope.back = function () {
        OTIRouteManager.clear();
        $state.go('new-success');
    };

    $scope.$watch('routeType.selected', function (newValue) {
        $scope.filteredRoutes = OTIRouteManager.filter(newValue);
    });
}]);
