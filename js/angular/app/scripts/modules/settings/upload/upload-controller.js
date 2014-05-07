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
        return input ? 'Finished Validating GTFS Data' : 'Validating GTFS Data';
    };
})
/**
 * Angular filter to set problem type class for rows
 * in the problem table. Each class has a color associated
 * with it via bootstrap
 *
 * @param input <string> expects GTFSFeed problem type string
 *     that should either be 'war' or 'err' depending on the
 *     type of problem
 */
.filter('problem_type', function() {
    return function(input) {
        return input === 'war' ? 'warning' : 'danger';
    };
})
/**
 * Main controller for GTFS upload page
 */
.controller('GTFSUploadController',
    ['$scope', '$timeout', '$upload', 'GTFSUploadService',
    function ($scope, $timeout, $upload, GTFSUploadService) {

    // Milliseconds timeout for the upload status
    var POLLING_MILLIS = 1000;
    // Number of milliseconds to timeout the upload check
    var TIMEOUT_MILLIS = 1000 * 60;

    /**
     * Cancels the upload of all active upload processes
     */
    $scope.cancel = function () {
        console.log('Canceling uploads...');

        if ($scope.upload && $scope.upload.abort) {
            console.log('Aborting: ', $scope.upload);
            $scope.upload.abort();
        }
        $timeout.cancel($scope.timeoutId);
        $scope.clearUploadProblems();
    };

    /**
     * Clears the uploadProblems dict
     */
    $scope.clearUploadProblems = function () {
        $scope.uploadProblems = {
            warnings: [],
            errors: []
        };
        $scope.metadata = {};
        $scope.uploadProgress = $scope.STATUS.START;
        $scope.uploadError = null;
        $scope.setSidebarCheckmark('upload', false);
    };

    /**
     * Set is_valid on current gtfsfeed to false
     * so that a new feed can be uploaded
     */
    $scope.delete = function () {
        // TODO: Implement once permissions/authentication is solved
        //$scope.gtfsUpload.is_valid = false;
        //$scope.gtfsUpload.$update().then(function () {
        $scope.gtfsUpload = {};
        $scope.uploadProgress = $scope.STATUS.START;
        $scope.setSidebarCheckmark('upload', false);
        //});
    };

    /*
     * Set an uploader error (displays the error UI div)
     * @param msg: An optional string message to display to the user.
     */
    $scope.setUploadError = function (msg) {
        if (msg) {
            $scope.uploadError = msg;
        }
        $scope.uploadProgress = $scope.STATUS.UPLOADERROR;
        $scope.setSidebarCheckmark('upload', false);
    };

	/*
     * Prep file for upload
     */
    $scope.onFileSelect = function($files) {

        $scope.cancel();

        // $files: an array of selected files, each with name, size, and type.
        var $file = $files[0];
        $scope.metadata.source_file = $file.name;
        $scope.start($file);
    };

    /**
     * Function to handle uploading of file
     */
    $scope.start = function ($file) {
        console.log('Uploading file...');
        if (!$file) {
            return;
        }


        $scope.uploadProgress = 0;
        var file = $file;
        $scope.upload = $upload.upload({
            url: '/api/gtfs-feeds/',
            method: 'POST',
            data: $scope.metadata,
            fileFormDataName: 'source_file',
            file: file
        }).progress(function (evt) {

            if ($scope.uploadProgress === $scope.STATUS.UPLOADERROR) {
                return;
            }
            $scope.uploadProgress = parseInt(evt.loaded / evt.total, 10);

        }).success(function(data, status, headers, config) {

            $scope.uploadProgress = 100;
            onUploadSuccess(data);

	    }).error(function(data, status, headers, config) {

            var msg = status;
            if (data.source_file) {
                msg += ' -- ' + (data.source_file[0] || 'Unknown');
            }
            $scope.setUploadError(msg);

        });
    };

    var onUploadSuccess = function (upload) {
        var startDatetime = new Date();
        var checkUpload = function () {
            var nowDatetime = new Date();
            if (nowDatetime.getTime() - startDatetime.getTime() > TIMEOUT_MILLIS) {
                $scope.setUploadError('Upload timeout');
            } else if (upload.is_valid === null) {
                $scope.timeoutId = $timeout(function () {
                    upload = GTFSUploadService.gtfsUploads.get({id: upload.id}, function () {
                        checkUpload();
                    });
                }, POLLING_MILLIS);
            } else if (upload.is_valid && !upload.is_processed) {
                $scope.setUploadError('Geotrellis unavailable');
            } else if (!upload.is_valid) {
                $scope.viewProblems(upload);
                $scope.setUploadError();
            } else {
                $scope.gtfsUpload = upload;
                $scope.uploadProgress = $scope.STATUS.DONE;
                $scope.setSidebarCheckmark('upload', true);
            }
        };
        checkUpload();
    };

    /**
     * Function to display problems of a gtfs feed upload
     *
     * @param upload <object> upload object that problems
     *     should be requested for
     */
    $scope.viewProblems = function(upload) {
        GTFSUploadService.gtfsProblems.query(
            {gtfsfeed: upload.id},
            function(data) {
                console.log('Upload problems: ', data);
                $scope.uploadProblems.warnings = _.filter(data, function (problem) {
                    return problem.type === 'war';
                });
                $scope.uploadProblems.errors = _.filter(data, function (problem) {
                    return problem.type === 'err';
                });
                console.log($scope.uploadProblems);
            });
    };

	// Set initial scope variables and constants
    $scope.gtfsUpload = null;
    $scope.clearUploadProblems();
    $scope.files = null;
    $scope.timeoutId = null;

    $scope.init = function () {
        $scope.gtfsUploads = GTFSUploadService.gtfsUploads.query({}, function (uploads) {
            var validUploads = _.filter(uploads, function (upload) {
                return upload.is_valid === true && upload.is_processed === true;
            });
            if (validUploads.length > 0) {
                $scope.gtfsUpload = validUploads[0];
                $scope.setSidebarCheckmark('upload', true);
                $scope.uploadProgress = $scope.STATUS.DONE;
            } else {
                $scope.gtfsUpload = {};
            }
            return validUploads;
        });
    };

}]);
