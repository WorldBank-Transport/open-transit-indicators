'use strict';

angular.module('transitIndicators')
/**
 * Angular filter to set processing status string
 *
 * @param input <boolean> expects boolean value from
 *     a gtfsfeed object's is_processed value
 */
.filter('processing_status', function() {
    return function(input) {
        return input ? 'Finished Validating OTI Data' : 'Validating OTI Data';
    };
})
/**
 * Angular filter to set problem type class for rows
 * in the problem table. Each class has a color associated
 * with it via bootstrap
 *
 * @param input <string> expects OTIFeed problem type string
 *     that should either be 'war' or 'err' depending on the
 *     type of problem
 */
.filter('problem_type', function() {
    return function(input) {
        return input === 'war' ? 'warning' : 'danger';
    };
})
/**
 * Main controller for OTI upload page
 */
.controller('OTIUploadController',
    ['$scope', '$rootScope', '$timeout', '$upload', 'OTIUploadService',
    function ($scope, $rootScope, $timeout, $upload, OTIUploadService) {

    /**
     * Clears the uploadProblems dict
     */
    var clearUploadProblems = function () {
        $scope.uploadProblems = {
            warnings: [],
            errors: []
        };
        $scope.setSidebarCheckmark('upload', false);
    };

    /**
     * Function to display problems of a gtfs feed upload
     *
     * @param upload <object> upload object that problems
     *     should be requested for
     */
    var viewProblems = function() {
        var upload = $scope.gtfsUpload;
        if (!(upload && upload.id)) {
            return;
        }

        OTIUploadService.gtfsProblems.query(
            {gtfsfeed: upload.id},
            function(data) {
                $scope.uploadProblems.warnings = _.filter(data, function (problem) {
                    return problem.type === 'war';
                });
                $scope.uploadProblems.errors = _.filter(data, function (problem) {
                    return problem.type === 'err';
                });
            });
    };

    $scope.$on('pollingUpload:pollingFinished', function () {
        viewProblems();
        $scope.setSidebarCheckmark('upload', true);
    });

    $scope.$on('pollingUpload:processingError', function () {
        viewProblems();
    });

    $scope.$on('pollingUpload:uploadCancel', function () {
        clearUploadProblems();
    });

    $scope.$on('pollingUpload:uploadDelete', function () {
        clearUploadProblems();
        $scope.setSidebarCheckmark('upload', false);
    });

    // Set initial scope variables and constants
    $scope.gtfsUpload = null;
    $scope.gtfsOptions = {
        uploadTimeoutMs: 10 * 60 * 1000
    };
    $scope.GTFSUploads = OTIUploadService.gtfsUploads;
    clearUploadProblems();

    $scope.init = function () {
        OTIUploadService.gtfsUploads.query({}, function (uploads) {
            var validUploads = _.filter(uploads, function (upload) {
                return upload.is_valid === true && upload.is_processed === true;
            });
            if (validUploads.length > 0) {
                $scope.gtfsUpload = validUploads[0];
                viewProblems();
                $scope.setSidebarCheckmark('upload', true);
            }
        });
    };

}]);
