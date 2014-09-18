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
        $scope.setSidebarCheckmark('demographic', false);
    };

    var setUpload = function (upload) {
        $scope.uploadRealtime = upload;
        var valid = upload && !_.isEmpty(upload) ? true : false;
        $scope.setSidebarCheckmark('realtime', valid);
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
            }, function () {
            });
    };

    $scope.RealTimeUpload = OTIRealTimeService.realtimeUpload;
    $scope.realtimeOptions = {
        uploadTimeoutMs: 10 * 60 * 1000,
        checkContinue: function (upload) {
            return !(upload.is_valid && upload.is_processed);
        }
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
        clearUploadProblems();
    });

    $scope.$on('pollingUpload:uploadDelete', function () {
        $scope.setSidebarCheckmark('realtime', false);
        clearUploadProblems();
    });

    $scope.init = function () {

        setUpload(null);
        clearUploadProblems();
        OTIRealTimeService.realtimeUpload.query({}, function (uploads) {
            var validUploads = _.filter(uploads, function (upload) {
                return upload.is_valid === true && upload.is_processed === true;
            });
            if (validUploads.length > 0) {
                setUpload(validUploads[0]);
                viewProblems();
            }
        });
    };

}]);
