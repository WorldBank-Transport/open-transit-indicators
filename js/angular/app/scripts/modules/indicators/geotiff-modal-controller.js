'use strict';
/*
 TODO: UPDATE COMMENTS
                          
 Basic modal that displays a message and buttons for 'Yes' and 'No'. Example usage:

   $modal.open({
       templateUrl: 'scripts/modules/indicators/yes-no-modal-partial.html',
       controller: 'OTIYesNoModalController',
       windowClass: 'yes-no-modal-window',
       resolve: {
           getMessage: function() {
               return 'CALCULATION.REDIRECT';
           }
       }
   }).result.then(function() {
       $state.go('calculation');
   });

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
