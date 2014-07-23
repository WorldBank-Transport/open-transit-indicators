'use strict';

angular.module('transitIndicators')
.controller('OTISettingsController',
        ['$scope', 'OTISettingsService', 'config',
        function ($scope, OTISettingsService, config) {

    var setSidebarHighlight = function (viewId) {
        $scope.activeView = viewId;
    };

    $scope.setSidebarCheckmark = function (viewId, isVisible) {
        $scope.checkmarks[viewId] = !!(isVisible);
    };

    $scope.STATUS = OTISettingsService.STATUS;
    $scope.views = config.settingsViews;
    $scope.checkmarks = {};
    _.each($scope.views, function (view) {
        $scope.checkmarks[view.id] = false;
    });

    $scope.$on('$stateChangeSuccess', function (event, toState) {
        setSidebarHighlight(toState.name);
    });

}]);
