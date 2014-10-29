'use strict';
angular.module('transitIndicators')
.controller('OTIIndicatorsCalculationController',
            ['$scope', '$timeout', 'OTIEvents', 'OTIIndicatorsService',
            function ($scope, $timeout, OTIEvents, OTIIndicatorsService) {

    // Number of milliseconds to wait between polls for status while a job is processing
    var POLL_INTERVAL_MILLIS = 5000;

    $scope.jobStatus = null;
    $scope.calculationStatus = null;
    $scope.displayStatus = null;
    $scope.currentJob = null;

    // Used for hiding messages about job status until we know what it is
    $scope.statusFetched = false;

    /**
     * Submits a job for calculating indicators
     */
    $scope.calculateIndicators = function () {
        var job = new OTIIndicatorsService.IndicatorJob({
            city_name: OTIIndicatorsService.selfCityName
        });
        job.$save().then(function (data) {
            $scope.jobStatus = null;
            $scope.calculationStatus = null;
            $scope.displayStatus = null;
            $scope.currentJob = null;
            pollForUpdatedStatus();
        });
    };

    /**
     * Sets the current job status given a list of job results
     */
    var setCurrentJob = function(indicatorJobs) {
        // There should only be one job in this list, but just in case there's multiple,
        // use the one with the highest id (i.e. the most recent one).
        var job = _.max(indicatorJobs, function(j) { return j.id; });
        $scope.currentJob = job;
        $scope.jobStatus = job.job_status;
        $scope.calculationStatus = angular.fromJson(job.calculation_status);

        if ($scope.jobStatus === 'processing') {
            $scope.displayStatus = 'STATUS.PROCESSING';
        } else if ($scope.jobStatus === 'complete') {
            $scope.displayStatus = 'STATUS.COMPLETE';
        } else if ($scope.jobStatus === 'error') {
            $scope.displayStatus = 'STATUS.FAILED';
        } else if ($scope.displayStatus === 'submitted') {
            $scope.displayStatus = 'STATUS.SUBMITTED';
        } else {
            console.log('unrecognized job status:');
            console.log($scope.jobStatus);
            $scope.displayStatus = 'STATUS.FAILED';
        }
    };

    /**
     * Queries for the most recent status, so it can be displayed
     */
    var pollForUpdatedStatus = function() {
        // First check if there's a job that's currently processing.
        // If there isn't one, instead use the latest calculation job.
        OTIIndicatorsService.IndicatorJob.search({ job_status: 'processing' })
            .$promise.then(function(processingData) {
                if (processingData.length) {
                    $scope.statusFetched = true;
                    console.log('still processing...');
                    setCurrentJob(processingData);

                    // Repeatedly poll for status while an indicator is processing
                    $timeout(pollForUpdatedStatus, POLL_INTERVAL_MILLIS);
                } else {
                    // filtering DRF booleans requires a ~capitalized~ string:
                    // http://www.django-rest-framework.org/api-guide/filtering
                    OTIIndicatorsService.IndicatorJob.search({ is_latest_version: 'True' })
                        .$promise.then(function(latestData) {
                            if (latestData.length) {
                                setCurrentJob(latestData);
                            } else {
                                OTIIndicatorsService.IndicatorJob.search({ job_status: 'error' })
                                    .$promise.then(function(errorData) {
                                        if (errorData.length) {
                                            setCurrentJob(errorData);
                                        }
                                    });
                                    $scope.statusFetched = true;
                            }
                            $scope.statusFetched = true;
                        });
                }
            });
    };
    pollForUpdatedStatus();
}]);
