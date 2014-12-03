'use strict';

angular.module('transitIndicators')
.controller('OTISettingsController',
        ['$scope', 'OTISettingsService', 'config', 'OTIUploadStatus',
        function ($scope, OTISettingsService, config, OTIUploadStatus) {

    $scope.Status = OTIUploadStatus;

    var setSidebarHighlight = function (viewId) {
        $scope.activeView = viewId;
    };

    $scope.setSidebarCheckmark = function (viewId, isVisible) {
        $scope.checkmarks[viewId] = !!(isVisible);
    };


    $scope.STATUS = OTISettingsService.STATUS;
    $scope.views = config.settingsViews;
    $scope.checkmarks = {};
    _.each($scope.views, function (view) {
        $scope.checkmarks[view.id] = false;
    });

    var initialize = function() {
        /**
         * FIXME: Use of these functions is not fully utilized.
                  Due to time constraints, switched tasks before fixing.
           TODO:
                - Remove resource definitions in the child services
                - Make all resources load as resolve of the settings controller
                - Remove references to child resources in child controller init methods
                - Actually call this function
         */
        var gtfsData = OTISettingsService.gtfsUploads.query();
        var boundaryData = OTISettingsService.boundaryUploads.query();
        var demographicData = OTISettingsService.demographics.query();
        var configData = OTISettingsService.configs.query();
        var realtimeData = OTISettingsService.realtimes.query();
        var samplePeriodData = OTISettingsService.samplePeriods.query();

        // GTFS
        $scope.gtfsData = gtfsData.$promise.then(function(response) {
            var validGtfs = _.filter(response, function(upload) {
                return upload.status === 'complete';
            });
            $scope.checkmarks.upload = validGtfs.length > 0;
            return response[0];
        });

        // DEMOGRAPHICS
        $scope.demographicData = demographicData.$promise.then(function(response){
            if (!(response && response.length)) {
                return;
            }
            var demographics = response[0];
            $scope.assign = {
                pop_metric_1: demographics.pop_metric_1_field || null,
                pop_metric_2: demographics.popmetric_2_field || null,
                dest_metric_1: demographics.destmetric_1_field || null
            };
            $scope.checkmarks.demographic = true;
            return demographics;
        });

        // REALTIME
        $scope.realtimeData = realtimeData.$promise.then(function(response){
            var validRealtime = _.filter(response, function(upload) {
                return upload.status === 'complete';
            });
            $scope.checkmarks.realtime = validRealtime.length > 0;
            return response[0];
        });

        $scope.configData = configData.$promise.then(function(configResponse) {
            // BOUNDARY
            var cityId = configResponse[0].city_boundary;
            var regId = configResponse[0].region_boundary;
            $scope.checkmarks.boundary = typeof(cityId) === 'number' && typeof(regId) === 'number';

            return configResponse[0];
        });

        // SAMPLE PERIODS
        $scope.samplePeriod = samplePeriodData.$promise.then(function(samplePeriods) {
            configData = configData.$promise.then(function(configResponse) {
                $scope.checkmarks.configuration = samplePeriods.length > 0 && configResponse[0].nearby_buffer_distance_m > 0;
            });
            return samplePeriods.slice(0,-1);
        });
    };

    $scope.$on('$stateChangeSuccess', function (event, toState) {
       setSidebarHighlight(toState.name);
    });

    initialize();
}]);
