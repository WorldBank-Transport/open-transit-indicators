'use strict';

angular.module('transitIndicators')
.controller('GTFSSettingsController',
        ['$scope', 'GTFSSettingsService', 'config',
        function ($scope, GTFSSettingsService, config) {

    $scope.STATUS = GTFSSettingsService.STATUS;
    $scope.views = config.settingsViews;
    $scope.checkmarks = {};
    _.each($scope.views, function (view) {
        $scope.checkmarks[view.id] = false;
    });

    $scope.setSidebarCheckmark = function (viewId, isVisible) {
        $scope.checkmarks[viewId] = !!(isVisible);
    };

}]);
