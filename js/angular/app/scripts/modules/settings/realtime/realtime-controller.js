'use strict';

angular.module('transitIndicators')
.controller('OTIRealtimeController',
        ['$scope', 'OTIRealTimeService',
        function ($scope, OTIRealTimeService) {

    var displayUploadProblems = function (upload) {

    };

    var clearUploadProblems = function (upload) {

    };

    $scope.RealTimeUpload = OTIRealTimeService.realtimeUpload;
    $scope.realtimeOptions = {
        uploadTimeoutMs: 5 * 60 * 1000
    };
    $scope.uploadRealtime = {};

    $scope.$on('pollingUpload:pollingFinished', function (event, upload) {
        $scope.setSidebarCheckmark('realtime', true);
    });

    $scope.$on('pollingUpload:processingError', function (event, upload) {
        displayUploadProblems(upload);
    });

    $scope.$on('pollingUpload:uploadCancel', function (event, upload) {
        clearUploadProblems(upload);
    });

    $scope.$on('pollingUpload:uploadDelete', function () {
        $scope.setSidebarCheckmark('realtime', false);
    });

    $scope.init = function () {

    };

}]);
