'use strict';
/*
 Basic modal that displays a message and an 'OK' button. Example usage:

   $modal.open({
       templateUrl: 'scripts/modules/indicators/ok-modal-partial.html',
       controller: 'OTIOKModalController',
       windowClass: 'ok-modal-window',
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
.controller('OTIOKModalController',
            ['$scope', '$modalInstance', 'getMessage',
            function ($scope, $modalInstance, message) {

  $scope.message = message;

  $scope.ok = function () {
    $modalInstance.close();
  };
}]);
