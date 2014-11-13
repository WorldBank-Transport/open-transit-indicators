'use strict';

angular.module('transitIndicators')
.controller('OTIDemographicController',
        ['$rootScope', '$scope', '$http', '$timeout', '$upload', 'OTIEvents', 'OTISettingsService',
        function ($rootScope, $scope, $http, $timeout, $upload, OTIEvents, OTISettingsService) {

    $scope.loadAlert = null;
    var addLoadAlert = function (alertObj) {
        $scope.loadAlert = alertObj;
    };

    $scope.DemographicUpload = OTISettingsService.demographics;
    $scope.uploadDemographic = {};
    $scope.demographicsOptions = {
        uploadTimeoutMs: 60 * 1000,
        checkComplete: function (upload) {
            return $scope.Status.isWaiting(upload.status) ||
                   $scope.Status.isComplete(upload.status) ||
                   $scope.Status.IMPORTING === upload.status;
        },
        checkContinue: function (upload) {
            return $scope.Status.PENDING === upload.status || $scope.Status.PROCESSING === upload.status;
        }
    };

    /*
     * Continuously polls for changes to the passed upload object until an error
     * is encountered or the upload is valid
     *
     * @param upload: A OTISettingsService.demographics $resource instance
     *
     * @return none
     */
    var pollForAssignments = function () {
        var ASSIGNMENT_TIMEOUT_MS = 60 * 1000;
        var POLLING_TIMEOUT_MS = 2 * 1000;
        var startDatetime = new Date();
        var checkAssignments = function () {
            var nowDatetime = new Date();
            if (nowDatetime.getTime() - startDatetime.getTime() > ASSIGNMENT_TIMEOUT_MS) {
                addLoadAlert({
                    type: 'danger',
                    msg: 'STATUS.TIMEOUT'
                });
            } else if ($scope.Status.isPolling($scope.uploadDemographic.status)) {
                $scope.timeoutId = $timeout(function () {
                    OTISettingsService.demographics.get({id: $scope.uploadDemographic.id}, function (data) {
                        $scope.uploadDemographic = data;
                        checkAssignments();
                    });
                }, POLLING_TIMEOUT_MS);
            } else if ($scope.Status.isError($scope.uploadDemographic.status)) {
                addLoadAlert({
                    type: 'danger',
                    msg: 'STATUS.FAILED'
                });
            } else if ($scope.Status.isWaiting($scope.uploadDemographic.status)) {
                // Log error to bottom of assign fields section
                addLoadAlert({
                    type: 'danger',
                    msg: 'STATUS.INVALID_SELECTIONS: asdf' + $scope.uploadDemographic.status
                });
            } else {
                addLoadAlert({
                    type: 'success',
                    msg: 'STATUS.SELECTIONS_SAVED'
                });
                $scope.setSidebarCheckmark('demographic', true);
                // Send assignment success message
                $rootScope.$broadcast(OTIEvents.Settings.Demographics.AssignmentDone);
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
        $scope.uploadError = null;
    };

    /*
     * Get and display a list of demographic upload problems for the current upload
     */
    var viewProblems = function() {
        var upload = $scope.uploadDemographic;
        if (!(upload && upload.id)) {
            return;
        }

        OTISettingsService.demogProblems.query(
            {datasource: upload.id},
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
     * @param upload: A OTISettingsService.demographics $resource instance
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
        if (!_.isEmpty(upload)) {
            OTISettingsService.demogConfigs.query(function (data) {
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
        $scope.setSidebarCheckmark('demographic', false);
    });

    $scope.$on('pollingUpload:uploadCancel', function () {
        clearUploadProblems();
        $scope.setSidebarCheckmark('demographic', false);
    });

    $scope.$on('pollingUpload:processingError', function () {
        viewProblems();
    });

    $scope.$on('pollingUpload:pollingFinished', function () {
        viewProblems();
    });

    $scope.showSelectionDiv = function () {
        var validStatuses = [
            $scope.Status.WAITING_INPUT,
            $scope.Status.COMPLETE,
            $scope.Status.ERROR,
            $scope.Status.IMPORTING
        ];
        return _.indexOf(validStatuses, $scope.uploadDemographic.status) >= 0;
    };

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
        ).success(function (data) {
            $scope.uploadDemographic.status = $scope.Status.IMPORTING;
            pollForAssignments();
            addLoadAlert({
                type: 'info',
                msg: 'STATUS.SAVING'
            });
        }).error(function (data, status) {
            // TODO: tranlsate
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
        clearUploadProblems();
        OTISettingsService.demographics.query({}, function (uploads) {
            if (uploads.length > 0) {
                var upload = uploads[uploads.length - 1];
                setUpload(upload);
                viewProblems();
            }
        });
    };

    $scope.init();

}]);
