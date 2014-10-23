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
    ['$scope', '$rootScope', '$timeout', '$upload', 'OTIUploadService', 'OTIEvents',
    function ($scope, $rootScope, $timeout, $upload, OTIUploadService, OTIEvents) {

    $scope.cityName = null;

    $scope.saveCityNameButton = {
        text: 'STATUS.SAVE',
        enabled: false // enable save button once city name has been changed
    };

    /**
     * Clears the uploadProblems dict
     */
    var clearUploadProblems = function () {
        $scope.uploadProblems = {
            warnings: [],
            errors: []
        };
    };

    /**
     * Set an osm import error (displays the error UI div)
     * @param msg <string> : An optional string message to display to the user.
     */
    var setOsmImportError = function (msg) {
        if (msg) {
            $scope.osmImportError = msg;
        }
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
            } else if ($scope.Status.isError(osmImport.status)) {
                setOsmImportError();
                viewOSMProblems(osmImport);
            } else if ($scope.Status.isPolling(osmImport.status)) {
                $scope.timeoutIdOSM = $timeout(function () {
                    osmImport = OTIUploadService.osmImport.get({id: osmImport.id}, function (data) {
                        $scope.osmImport = osmImport;
                        checkImport();
                    });
                }, POLLING_TIMEOUT_MS);
            } else {
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
            } else {
                OTIUploadService.osmImport.save({gtfsfeed: $scope.gtfsUpload.id}, function(osmImport) {
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
        if (!_.isEmpty($scope.osmImport)) {
            $scope.osmImport.$delete();
            clearOsmImportProblems();
        }

        OTIUploadService.osmImport.save({gtfsfeed: $scope.gtfsUpload.id}, function(osmImport) {
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
        $rootScope.$broadcast(OTIEvents.Settings.Upload.GTFSDone);
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
        $scope.osmImport = {};
        $scope.osmImportProgress = -1;
        $scope.osmImportProblems = {};
        $rootScope.$broadcast(OTIEvents.Settings.Upload.GTFSDelete);
    });

    $scope.saveCityName = function () {
        $scope.saveCityNameButton.enabled = false;
        $scope.saveCityNameButton.text = 'STATUS.SAVING';
        OTIUploadService.cityName.save({'city_name': $scope.cityName}, function () {
            $scope.saveCityNameButton.enabled = true;
            $scope.saveCityNameButton.text = 'STATUS.SAVE';
        }, function (error) {
            $scope.saveCityNameButton.enabled = true;
            $scope.saveCityNameButton.text = 'STATUS.SAVE';

            // ignore error if attempt to save unchanged city name
            if (error.data.city_name && error.data.city_name[0].indexOf('already exists') === -1) {
                $scope.cityNameError = true;
            } else {
                console.log('error saving city name:');
                console.log(error.data);
            }
            
        });
    };

    // disable save button is city name field is empty
    $scope.checkCityName = function () {
        if ($scope.cityName) {
            $scope.saveCityNameButton.enabled = true;
        } else {
            $scope.saveCityNameButton.enabled = false;
        }
    };

    // Set initial scope variables and constants
    $scope.gtfsUpload = {};
    $scope.gtfsOptions = {
        uploadTimeoutMs: 90 * 60 * 1000
    };
    $scope.GTFSUploads = OTIUploadService.gtfsUploads;
    $scope.osmImport = {};
    $scope.osmImportProblems = {};
    clearUploadProblems();

    $scope.init = function () {
        OTIUploadService.gtfsUploads.query({}, function (uploads) {
            if (uploads.length > 0) {
                $scope.gtfsUpload = uploads.pop();
                viewProblems();
                var valid = $scope.Status.isComplete($scope.gtfsUpload.status);
                $scope.setSidebarCheckmark('upload', valid);

                // Check OSM imports for GTFS Feed
                if (valid) {
                    checkOSMImport($scope.gtfsUpload);
                }
            }
        });

        OTIUploadService.cityName.get({}, function (data) {
            $scope.cityName = data.city_name;
        });
    };
}]);
