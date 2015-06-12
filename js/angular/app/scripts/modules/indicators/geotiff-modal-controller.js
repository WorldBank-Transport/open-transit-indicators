'use strict';
/*
 Modal to prompt user which jobs accessibility geotiff to download (or cancel out).
*/
angular.module('transitIndicators')
.controller('OTIGeotiffModalController',
            ['$scope', '$modalInstance', 'getMessage', 'getIndicators',
            function ($scope, $modalInstance, message, indicators) {

  $scope.indicators = indicators;

  $scope.message = message;

  $scope.selected = function (indicator) {
    $modalInstance.close(indicator);
  };

  $scope.cancel = function () {
    $modalInstance.dismiss();
  };
}]);
