'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosRoutesController',
            ['$scope', '$state', 'OTIScenarioManager', 'OTIRouteManager',
            function ($scope, $state, OTIScenarioManager, OTIRouteManager) {

    var scenario = OTIScenarioManager.get();
    $scope.filteredRoutes = [];
    OTIRouteManager.list({db_name: scenario.db_name}).then(function (routes) {
        $scope.filteredRoutes = routes;
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
        var route = OTIRouteManager.findById(routeId);
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
