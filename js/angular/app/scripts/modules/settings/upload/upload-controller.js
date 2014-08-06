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
     * Set an osm import error (displays the error UI div)
     * @param msg <string> : An optional string message to display to the user.
     */
    var setOsmImportError = function (msg) {
        if (msg) {
            $scope.osmImportError = msg;
        }
        $scope.osmImportProgress = $scope.STATUS.UPLOADERROR;
    };

    /**
     * Function to display problems of OSM import
     *
     * @param osmImport <object> angular resource for OSM import
     */
    var viewOSMProblems = function(osmImport) {
        if (!(osmImport && osmImport.id)) {
            return;
        }

        OTIUploadService.osmImportProblems.query(
            {osmdata: osmImport.id},
            function(data) {
                $scope.osmImportProblems.warnings = _.filter(data, function (problem) {
                    return problem.type === 'war';
                });
                $scope.osmImportProblems.errors = _.filter(data, function (problem) {
                    return problem.type === 'err';
                });
            });
    };

    /**
     * Clears the OSM import problems obj
     */
    var clearOsmImportProblems = function () {
        $scope.osmImportProblems = {
            warnings: [],
            errors: []
        };
    };


    /**
     * Continuously poll for OSM import status
     *
     * @param osmImport <object> angular resource for an osm import
     *
     */
    var pollForOSMImport = function (osmImport) {
        var OSMIMPORT_TIMEOUT_MS = 30 * 60 * 1000;
        var POLLING_TIMEOUT_MS = 2 * 1000;
        var startDatetime = new Date();
        var checkImport = function () {
            var nowDatetime = new Date();
            if (nowDatetime.getTime() - startDatetime.getTime() > OSMIMPORT_TIMEOUT_MS) {
                setOsmImportError('OpenStreetMap import timeout');
                $scope.osmImportProgress = $scope.STATUS.UPLOADERROR;
            } else if (osmImport.is_valid === false) {
                setOsmImportError();
                viewOSMProblems(osmImport);
            } else if (!(osmImport.is_valid && osmImport.is_processed)) {
                $scope.timeoutIdOSM = $timeout(function () {
                    osmImport = OTIUploadService.osmImport.get({id: osmImport.id}, function (data) {
                        $scope.osmImport = osmImport;
                        checkImport();
                    });
                }, POLLING_TIMEOUT_MS);
            } else {
                $scope.osmImportProgress = $scope.STATUS.DONE;
                viewOSMProblems(osmImport);
            }
        };
        checkImport();
    };

    /**
     * Helper function to check for valid OSM import
     *
     * @param gtfsfeed <object> angular resource for gtfs feed
     */
    var checkOSMImport = function(gtfsfeed) {
        OTIUploadService.osmImport.query({gtfsfeed: gtfsfeed.id}, function(osmimports) {
            // If there is an OSM import, display it status, else create one
            if (osmimports.length > 0) {
                viewOSMProblems(osmimports[0]);
                $scope.osmImport = osmimports[0];
                $scope.osmImportProgress = ($scope.osmImport.is_valid) ? $scope.STATUS.DONE : $scope.STATUS.UPLOADERROR;
            } else {
                OTIUploadService.osmImport.save({gtfsfeed: $scope.gtfsUpload.id}, function(osmImport) {
                    $scope.osmImportProgress = 0;
                    pollForOSMImport(osmImport);
                });
            }
        });
    };

    /**
     * Retry importing of OpenStreetMap data
     *
     */
    $scope.retryOSMImport = function() {
        if ($scope.osmImport) {
            $scope.osmImport.$delete();
            clearOsmImportProblems();
        }

        OTIUploadService.osmImport.save({gtfsfeed: $scope.gtfsUpload.id}, function(osmImport) {
            $scope.osmImportProgress = 0;
            $scope.osmImport = osmImport;
            pollForOSMImport(osmImport);
        });

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
        $scope.retryOSMImport();
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

        // OSM imports get deleted since they have a foreign key
        // to the GTFSFeed, but we need to reset the UI
        $scope.osmImport = null;
        $scope.osmImportProgress = -1;
        $scope.osmImportProblems = {};
    });

    // Set initial scope variables and constants
    $scope.gtfsUpload = null;
    $scope.gtfsOptions = {
        uploadTimeoutMs: 10 * 60 * 1000
    };
    $scope.GTFSUploads = OTIUploadService.gtfsUploads;
    $scope.osmImport = null;
    $scope.osmImportProblems = {};
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
                $scope.uploadProgress = $scope.STATUS.DONE;

                // Check OSM imports for GTFS Feed
                checkOSMImport($scope.gtfsUpload);
            }
        });

    };

}]);
