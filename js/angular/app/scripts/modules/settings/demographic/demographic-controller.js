'use strict';

angular.module('transitIndicators')
.controller('OTIDemographicController',
        ['$rootScope', '$scope', '$http', '$timeout', '$upload', 'OTIDemographicService',
        function ($rootScope, $scope, $http, $timeout, $upload, OTIDemographicService) {

    $scope.loadAlert = null;
    var addLoadAlert = function (alertObj) {
        $scope.loadAlert = alertObj;
    };

    $scope.DemographicUpload = OTIDemographicService.demographicUpload;
    $scope.demographicsOptions = {
        uploadTimeoutMs: 60 * 1000
    };

    /*
     * Continuously polls for changes to the passed upload object until an error
     * is encountered or the condition of is_valid === true && is_loaded === true
     * is satisfied
     *
     * @param upload: A OTIDemographicService.demographicUpload $resource instance
     *
     * @return none
     */
    var pollForAssignments = function (upload) {
        var ASSIGNMENT_TIMEOUT_MS = 30 * 1000;
        var POLLING_TIMEOUT_MS = 2 * 1000;
        var startDatetime = new Date();
        var checkAssignments = function () {
            var nowDatetime = new Date();
            if (nowDatetime.getTime() - startDatetime.getTime() > ASSIGNMENT_TIMEOUT_MS) {
                addLoadAlert({
                    type: 'danger',
                    msg: 'Job timed out. Try again later.'
                });
            } else if (!(upload.is_valid && upload.is_loaded)) {
                $scope.timeoutId = $timeout(function () {
                    upload = OTIDemographicService.demographicUpload.get({id: upload.id}, function () {
                        checkAssignments();
                    });
                }, POLLING_TIMEOUT_MS);
            } else if (!upload.is_valid) {
                // Log error to bottom of assign fields section
                addLoadAlert({
                    type: 'danger',
                    msg: 'Invalid selections.'
                });
            } else {
                addLoadAlert({
                    type: 'success',
                    msg: 'Selections saved!'
                });
                $scope.setSidebarCheckmark('demographic', true);
                // Send assignment success message
                $rootScope.$broadcast('demographics-controller:assignment-done');
            }
        };
        checkAssignments();
    };

    /*
     * Clear the UI of errors/warnings and reset to initial state
     */
    var clearUploadProblems = function () {
        $scope.uploadProblems = {
            warnings: [],
            errors: []
        };
        $scope.uploadProgress = -1;
        $scope.uploadError = null;
        $scope.setSidebarCheckmark('demographic', false);
    };

    /*
     * Get and display a list of demographic upload problems for the current upload
     */
    var viewProblems = function() {
        var upload = $scope.uploadDemographic;
        if (!(upload && upload.id)) {
            return;
        }

        OTIDemographicService.demographicsProblems.query(
            {id: upload.id},
            function(data) {
                $scope.uploadProblems.warnings = _.filter(data, function (problem) {
                    return problem.type === 'war';
                });
                $scope.uploadProblems.errors = _.filter(data, function (problem) {
                    return problem.type === 'err';
                });
            });
    };

    /*
     * Setter for the $scope.upload property. Do not set $scope.upload directly.
     *
     * @param upload: A OTIDemographicService.demographicUpload $resource instance
     *
     * @return none
     */
    var setUpload = function (upload) {
        $scope.uploadDemographic = upload;
        // Add ng-select model bindings as object on scope
        // http://stackoverflow.com/questions/19408883/angularjs-select-not-2-way-binding-to-model
        $scope.assign = {
            pop_metric_1: null,
            pop_metric_2: null,
            dest_metric_1: null
        };

        // if we're setting a new upload object, query the demographics config endpoint
        // to update the selected options
        if (upload) {
            OTIDemographicService.demographicsConfig.query(function (data) {
                if (!(data && data.length)) {
                    return;
                }
                var config = data[0];
                $scope.assign = {
                    pop_metric_1: config.pop_metric_1_field || null,
                    pop_metric_2: config.pop_metric_2_field || null,
                    dest_metric_1: config.dest_metric_1_field || null
                };
                $scope.setSidebarCheckmark('demographic', true);
            });
        }
    };

    $scope.$on('pollingUpload:uploadDelete', function () {
        clearUploadProblems();
    });

    $scope.$on('pollingUpload:uploadCancel', function () {
        clearUploadProblems();
    });

    $scope.$on('pollingUpload:processingError', function () {
        viewProblems();
    });

    $scope.$on('pollingUpload:pollingFinished', function () {
        viewProblems();
    });

    /*
     * Asynchronously Save user selections to the demographics field assignments
     * Pulls assignments from the selections, which are saved in the $scope.assign object
     */
    $scope.save = function () {
        var data = {};
        if ($scope.assign.pop_metric_1) {
            data.pop_metric_1_field = $scope.assign.pop_metric_1;
        }
        if ($scope.assign.pop_metric_2) {
            data.pop_metric_2_field = $scope.assign.pop_metric_2;
        }
        if ($scope.assign.dest_metric_1) {
            data.dest_metric_1_field = $scope.assign.dest_metric_1;
        }
        $http.post('/api/demographics/' + $scope.uploadDemographic.id + '/load/', data
        ).success(function () {
            pollForAssignments($scope.uploadDemographic);
            addLoadAlert({
                type: 'info',
                msg: 'Saving...'
            });
        }).error(function (data, status) {
            addLoadAlert({
                type: 'danger',
                msg: _.values(data)[0] || (status + ': Unknown Error')
            });
        });
    };

    /*
     * Initialize the view on page load, getting a valid demographic upload if it
     * exists and setting the currently assigned demographic config if it exists via
     * setUpload
     */
    $scope.init = function () {
        setUpload(null);
        clearUploadProblems();
        OTIDemographicService.demographicUpload.query({}, function (uploads) {
            var validUploads = _.filter(uploads, function (upload) {
                return upload.is_valid === true && upload.is_processed === true;
            });
            if (validUploads.length > 0) {
                setUpload(validUploads[0]);
                viewProblems();
            }
        });
    };

    $scope.init();

}]);
