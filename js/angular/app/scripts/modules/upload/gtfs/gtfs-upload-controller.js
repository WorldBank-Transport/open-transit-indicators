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
    ['$scope', '$upload', 'GTFSUploadService',
    function ($scope, $upload, GTFSUploadService) {

    /**
     * Cancels the upload of all active upload processes
     */
    $scope.cancel = function () {
        console.log('Canceling uploads...');

        if ($scope.upload && $scope.upload.abort) {
            $scope.upload.abort();
        }
        $scope.clearUploadProblems();
    };

    /**
     * Set a timeout to query the gtfs-feeds endpoint for
     * upload processing updates. Set UI state once
     * processing errors or returns
     */
    $scope.checkUpload = function (upload) {
        console.log('Check upload:', upload);
        $scope.viewProblems(upload);
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
        $scope.uploadProgress = -1;
    };

	// Upload functions
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
            // Allow upload to be 75% of progress, other 25% is backend processing
            $scope.uploadProgress = parseInt(75.0 * evt.loaded / evt.total, 10);
        }).success(function(data, status, headers, config) {
            $scope.checkUpload(data);
	    }).error(function(data, status, headers, config) {
            console.log('Error: ', data);
        });
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

    /**
     * Helper method to get uploads using GTFSUploadService
     *
     * Broken out into a separate method because this is called
     * when the refresh button is clicked
     */
    $scope.getGTFSUploads = function() {
        $scope.gtfsUploads = GTFSUploadService.gtfsUploads.query({}, function(data) {
            return data;
        });
    };

	// Set initial scope variables and constants
	$scope.gtfsUpload = null;
    $scope.clearUploadProblems();
    $scope.files = null;
	$scope.problemsPerPage = 5;

    $scope.init = function () {

    };

}]);
