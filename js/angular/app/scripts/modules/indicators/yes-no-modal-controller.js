'use strict';
/*
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
.controller('OTIYesNoModalController',
            ['$scope', '$modalInstance', 'getMessage',
            function ($scope, $modalInstance, message) {

  $scope.message = message;

  $scope.yes = function () {
    $modalInstance.close();
  };

  $scope.no = function () {
    $modalInstance.dismiss();
  };
}]);
