'use strict';

angular.module('transitIndicators')
.controller('OTIBoundaryController',
    ['$scope', '$rootScope', '$timeout', '$upload', 'OTIConfigurationService', 'OTISettingsService',
    function ($scope, $rootScope, $timeout, $upload, OTIConfigurationService, OTISettingsService) {

    var isCityUpload = function (upload) {
        return $scope.uploadCity && upload.id === $scope.uploadCity.id;
    };

    var isRegionUpload = function (upload) {
        return $scope.uploadRegion && upload.id === $scope.uploadRegion.id;
    };

    var displayBoundaryUploadProblems = function (upload) {
        var uploadProblems = isCityUpload(upload) ?
                             $scope.uploadProblemsCity : $scope.uploadProblemsRegion;

        OTISettingsService.boundaryProblems.query({ boundary: upload.id }, function(data) {
            uploadProblems.warnings = _.filter(data, function (problem) {
                return problem.type === 'war' && problem.boundary === upload.id;
            });
            uploadProblems.errors = _.filter(data, function (problem) {
                return problem.type === 'err' && problem.boundary === upload.id;
            });
        });
    };

    var clearBoundaryUploadProblems = function (upload) {
        if (isCityUpload(upload)) {
            $scope.uploadProblemsCity = {
                warnings: [],
                errors: []
            };
        } else if (isRegionUpload(upload)) {
            $scope.uploadProblemsRegion = {
                warnings: [],
                errors: []
            };
        }
        setSidebarCheckmark();
    };

    $scope.$on('pollingUpload:pollingFinished', function (event, upload) {
        if (isCityUpload(upload)) {
            $scope.config.city_boundary = upload.id;
        } else if (isRegionUpload(upload)) {
            $scope.config.region_boundary = upload.id;
        }
        $scope.config.$update({ id: $scope.config.id }).then(function () {
            setSidebarCheckmark();
        });
    });

    $scope.$on('pollingUpload:processingError', function (event, upload) {
        displayBoundaryUploadProblems(upload);
    });

    $scope.$on('pollingUpload:uploadCancel', function (event, upload) {
        clearBoundaryUploadProblems(upload);
    });

    $scope.$on('pollingUpload:uploadDelete', function () {
        $scope.setSidebarCheckmark('boundary', false);
    });

    /*
     * Sets the sidebar checkmark if both city and region are uploaded
     */
    var setSidebarCheckmark = function () {
        var config = $scope.config;
        var checked = config ? (config.city_boundary && config.region_boundary) : false;
        $scope.setSidebarCheckmark('boundary', checked);
    };

    $scope.cityOptions = {
        uploadTimeoutMs: 5 * 60 * 1000,
        pollingTimeoutMs: 1000
    };

    $scope.boundaryOptions = {
        uploadTimeoutMs: 5 * 60 * 1000,
        pollingTimeoutMs: 1000
    };

    $scope.BoundaryUploads = OTISettingsService.boundaryUploads;
    $scope.uploadProblemsCity = {
        warnings: [],
        errors: []
    };
    $scope.uploadProblemsRegion = {
        warnings: [],
        errors: []
    };

    /*
     * Initialize the view on page load, setting valid boundary uploads if they exist
     */
     $scope.uploadCity = {};
     $scope.uploadRegion = {};
    $scope.init = function () {
        // get the global configuration object
        OTIConfigurationService.Config.query({}, function (configs) {
            if (configs.length !== 1) {
                console.error('Expected a single configuration, but found: ', configs);
                return;
            }
            var config = $scope.config = configs[0];
            var cityId = config.city_boundary;
            var regionId = config.region_boundary;

            // check for boundaries
            if (cityId) {
                OTISettingsService.boundaryUploads.get({ id: cityId }, function (upload) {
                    $scope.uploadCity = upload;
                });
            }
            if (regionId) {
                OTISettingsService.boundaryUploads.get({ id: regionId }, function (upload) {
                    $scope.uploadRegion = upload;
                });
            }
            setSidebarCheckmark();
        });
    };
}]);
