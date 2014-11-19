'use strict';
angular.module('transitIndicators')
.controller('OTIIndicatorsCalculationController',
            ['$modal', '$scope', '$state', '$timeout', 'OTIIndicatorJobManager', 'OTIIndicatorJobModel',
            function ($modal, $scope, $state, $timeout, OTIIndicatorJobManager, OTIIndicatorJobModel) {

    // Number of milliseconds to wait between polls for status while a job is processing
    var POLL_INTERVAL_MILLIS = 5000;

    $scope.jobStatus = null;
    $scope.allTimeCalculations = {};
    $scope.periodicCalculations = {};
    $scope.displayStatus = null;
    $scope.currentJob = null;

    // Used for hiding messages about job status until we know what it is
    $scope.statusFetched = false;

    var configParamTranslations = {
        'poverty_line': 'SETTINGS.POVERTY_LINE',
        'nearby_buffer_distance_m': 'SETTINGS.DISTANCE_BUFFER',
        'max_commute_time_s': 'SETTINGS.JOB_TRAVEL_TIME',
        'arrive_by_time_s': 'SETTINGS.JOB_ARRIVE_BY_TIME',
        'avg_fare': 'SETTINGS.AVG_FARE',
        'osm_data': 'CALCULATION.OSM_DATA'
    };

    /**
     * Submits a job for calculating indicators
     */
    $scope.calculateIndicators = function () {
        var job = new OTIIndicatorJobModel({
            city_name: OTIIndicatorJobManager.getCurrentCity()
        });
        job.$save(function (data) {
            $scope.jobStatus = null;
            $scope.allTimeCalculations = {};
            $scope.periodicCalculations = {};
            $scope.displayStatus = null;
            $scope.currentJob = null;
            pollForUpdatedStatus();
        }, function (reason) {
            if (reason.data.error && reason.data.error === 'Invalid configuration') {
                $modal.open({
                    templateUrl: 'scripts/modules/indicators/yes-no-modal-partial.html',
                    controller: 'OTIYesNoModalController',
                    windowClass: 'yes-no-modal-window',
                    resolve: {
                        getMessage: function() {
                            return 'CALCULATION.NOT_CONFIGURED';
                        },
                        getList: function() {
                            return _.map(reason.data.items, function (item) {
                                return configParamTranslations[item] || item;
                            });
                        }
                    }
                }).result.then(function () {
                    $state.go('configuration');
                });
            }
        });
    };

    var reorderKeys = function(periodThenIndicator) {
        var indicatorThenPeriod = {};
        var periods = _.keys(periodThenIndicator);
        var indicators = _.keys(periodThenIndicator.morning);
        _.each(indicators, function(indicator) {
            indicatorThenPeriod[indicator] = {};
            _.each(periods, function(period) {
                indicatorThenPeriod[indicator][period] = periodThenIndicator[period][indicator];
            });
        });
        return indicatorThenPeriod;
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
        var calculationStatus = angular.fromJson(job.calculation_status);
        if (calculationStatus) {
            $scope.allTimeCalculations = calculationStatus.alltime || null;
            delete calculationStatus.alltime;
            $scope.periods = _.keys(calculationStatus);
            $scope.periodicCalculations = reorderKeys(calculationStatus);
            console.log($scope.allTimeCalculations)
            console.log($scope.periodicCalculations)
        }

        if ($scope.jobStatus === 'processing') {
            $scope.displayStatus = 'STATUS.PROCESSING';
        } else if ($scope.jobStatus === 'queued') {
            $scope.displayStatus = 'STATUS.QUEUED';
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
        // First check if there's a job that's currently processing or queued.
        // If there isn't one, instead use the latest calculation job.
        OTIIndicatorJobModel.search({ job_status: 'queued,processing' })
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
                    OTIIndicatorJobModel.latest()
                        .$promise.then(function(latestData) {
                            if (latestData) {
                                setCurrentJob([latestData]);
                            } else {
                                OTIIndicatorJobModel.search({ job_status: 'error' })
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
