'use strict';
angular.module('transitIndicators')
.controller('OTIIndicatorsCalculationController',
            ['$scope', '$timeout', 'OTIEvents', 'OTIIndicatorsService',
            function ($scope, $timeout, OTIEvents, OTIIndicatorsService) {

    // Number of milliseconds to wait between polls for status while a job is processing
    var POLL_INTERVAL_MILLIS = 5000;

    $scope.calculationStatus = null;
    $scope.currentJob = null;

    // Used for hiding messages about calculation status until we know what it is
    $scope.statusFetched = false;

    /**
     * Submits a job for calculating indicators
     */
    $scope.calculateIndicators = function () {
        var job = new OTIIndicatorsService.IndicatorJob({
            city_name: OTIIndicatorsService.selfCityName
        });
        job.$save().then(function (data) {
            $scope.calculationStatus = null;
            $scope.currentJob = null;
            pollForUpdatedStatus();
        });
    };

    /**
     * Sets the current job and calculation status given a list of job results
     */
    var setCurrentJob = function(indicatorJobs) {
        // There should only be one job in this list, but just in case there's multiple,
        // use the one with the highest id (i.e. the most recent one).
        var job = _.max(indicatorJobs, function(j) { return j.id; });
        $scope.currentJob = job;
        $scope.calculationStatus = angular.fromJson(job.calculation_status);
    };

    /**
     * Queries for the most recent calculation status, so it can be displayed
     */
    var pollForUpdatedStatus = function() {
        // First check if there's a job that's currently processing.
        // If there isn't one, instead use the job with the latest version.
        OTIIndicatorsService.IndicatorJob.search({ job_status: 'processing' })
            .$promise.then(function(processingData) {
                if (processingData.length) {
                    $scope.statusFetched = true;
                    setCurrentJob(processingData);

                    // Repeatedly poll for status while an indicator is processing
                    $timeout(pollForUpdatedStatus, POLL_INTERVAL_MILLIS);
                } else {
                    OTIIndicatorsService.IndicatorJob.search({ is_latest_version: true })
                        .$promise.then(function(latestData) {
                            if (latestData.length) {
                                setCurrentJob(latestData);
                            }
                            $scope.statusFetched = true;
                        });
                }
            });
    };
    pollForUpdatedStatus();
}]);
