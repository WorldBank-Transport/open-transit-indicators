'use strict';

angular.module('transitIndicators')
.controller('OTIRealtimeController',
        ['$scope', 'OTIRealTimeService',
        function ($scope, OTIRealTimeService) {

    var clearUploadProblems = function () {
        $scope.uploadProblems = {
            warnings: [],
            errors: []
        };
    };

    var setUpload = function (upload) {
        $scope.uploadRealtime = upload;
        var valid = upload && !_.isEmpty(upload) ? true : false;
        if (upload !== null) {
            $scope.setSidebarCheckmark('realtime', valid);
        }
    };

    var viewProblems = function () {
        var upload = $scope.uploadRealtime;
        if (!(upload && upload.id)) {
            return;
        }

        OTIRealTimeService.realtimeProblems.query({},
            function(data) {
                $scope.uploadProblems.warnings = _.filter(data, function (problem) {
                    return problem.realtime === upload.id && problem.type === 'war';
                });
                $scope.uploadProblems.errors = _.filter(data, function (problem) {
                    return problem.realtime === upload.id && problem.type === 'err';
                });
            });
    };

    $scope.RealTimeUpload = OTIRealTimeService.realtimeUpload;
    $scope.realtimeOptions = {
        uploadTimeoutMs: 10 * 60 * 1000
    };
    $scope.uploadRealtime = {};

    $scope.$on('pollingUpload:pollingFinished', function () {
        $scope.setSidebarCheckmark('realtime', true);
        viewProblems();
    });

    $scope.$on('pollingUpload:processingError', function () {
        viewProblems();
    });

    $scope.$on('pollingUpload:uploadCancel', function () {
        $scope.setSidebarCheckmark('realtime', false);
        clearUploadProblems();
    });

    $scope.$on('pollingUpload:uploadDelete', function () {
        $scope.setSidebarCheckmark('realtime', false);
        clearUploadProblems();
    });

    $scope.init = function () {

        clearUploadProblems();
        OTIRealTimeService.realtimeUpload.query({}, function (uploads) {
            if (uploads.length > 0) {
                var upload = uploads.pop();
                setUpload(upload);
                viewProblems();
            }
        });
    };

}]);
