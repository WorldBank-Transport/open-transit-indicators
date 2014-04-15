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
 * @param input <object> expects GTFSFeed problem object
 */
.filter('problem_type', function() {
    return function(input) {
        return input.type === 'war' ? 'danger' : 'warning';
    };
})
/**
 * Main controller for GTFS upload page
 */
.controller('GTFSUploadController',
    ['$scope', '$upload', 'GTFSUploadService',
    function ($scope, $upload, GTFSUploadService) {

	// Set initial scope variables and constants
	$scope.metadata = {};
	$scope.uploads = [];
        $scope.uploadProblems = null;
        $scope.files = null;
	$scope.problemsPerPage = 5;

	// Upload functions
        $scope.onFileSelect = function($files) {
            // $files: an array of selected files, each with name, size, and type.
            $scope.files = $files;
            $scope.metadata.source_file = $files[0].name;
        };

	/**
	 * Function to handle uploading of file
	 */
        $scope.upload = function () {
            console.log("Uploading file...");
            if (!$scope.files) {
                return;
            }
            // TODO: Status message here
            var file = $scope.files[0];
            $scope.upload = $upload.upload({
                url: '/api/gtfs-feeds/',
                method: 'POST',
                data: $scope.metadata,
                fileFormDataName: 'source_file',
                file: file
            }).success(function(data, status, headers, config) {
		$scope.uploads.push(data);
	    });

	    ; // TODO: .success() and .error() here.
            console.log("Uploaded!");
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
		    // Proecess results for easy pagination
                    var paginatedResults = _.groupBy(data, function(element, index) {
                        return Math.floor(index/$scope.problemsPerPage);
                    });
                    $scope.problemsLength = data.length; // Set length
                    $scope.uploadProblems = paginatedResults;
                    $scope.updatePagination(1); // Get first page of results
                });
        };

	/**
	 * Function to handle pagination of GTFS problems
	 *
	 * Called when new page is clicked on page, upates
	 * list that is displayed in the table
	 *
	 * @param page <integer> page number that should be displayed
	 */
        $scope.updatePagination = function(page) {
            $scope.currentPage = page; // set scope variable to change display
            var index = $scope.currentPage - 1; // subtract 1 because paginated list starts @ zero
            $scope.uploadProblemsPaginated = $scope.uploadProblems[index]; // update list of problems displayed
        };

	/**
	 * Helper method to get uploads using GTFSUploadService
	 * 
	 * Broken out into a separate method because this is called
	 * when the refresh button is clicked
	 */
        $scope.getUploads = function() {
            $scope.uploads = GTFSUploadService.gtfsUploads.query({}, function(data) {
                return data;
            });
        };

	/**
	 * Get current uploads on page load
	 */
        $scope.init = function() {
            $scope.getUploads();
        };
}]);
