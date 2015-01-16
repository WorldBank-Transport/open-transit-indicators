'use strict';

angular.module('transitIndicators')
.controller('OTICityModalController',
        ['$scope', '$modalInstance', '$rootScope', '$translate', '$upload',
         'OTICityManager', 'OTIIndicatorJobManager', 'completeIndicatorJobs', 'cities',
        function($scope, $modalInstance, $rootScope, $translate, $upload,
                 OTICityManager, OTIIndicatorJobManager, completeIndicatorJobs, cities) {

    $scope.cities = cities;

    $scope.userScenarios = _.filter(completeIndicatorJobs, function(job) {
        return job.created_by === $scope.user.id &&
               job.scenario && !_.find($scope.cities, function(x) { return x === job; });
    });
    $scope.otherScenarios = _.filter(completeIndicatorJobs, function(job) {
        return job.created_by !== $scope.user.id &&
               job.scenario && !_.find($scope.cities, function(x) { return x === job; });
    });

    // Translation strings
    $scope.translations = {
        IMPORTBADFILE: '',
        IMPORTERROR: ''
    };
    $translate('CITYMODAL.IMPORTBADFILE').then(function(text) {
        $scope.translations.IMPORTBADFILE = text;
    });
    $translate('CITYMODAL.IMPORTERROR').then(function(text) {
        $scope.translations.IMPORTERROR = text;
    });

    $scope.ok = function () {
        $modalInstance.close();
    };

    var saveLoadedScenarios = function () {
        OTIIndicatorJobManager.setLoadedScenarios(_.chain($scope.cities)
                                                  .pluck('scenario')
                                                  .without(null).value());
    };

    // City controls

    $scope.remove = function (job) {
        var index = $scope.cities.indexOf(job);
        if(job.scenario !== null) {
            $scope.cities.splice(index, 1);
            if (job.created_by === $scope.user.id) {
                $scope.userScenarios.push(job);
            } else {
                $scope.otherScenarios.push(job);
            }
        } else {
	        OTICityManager.delete(job.city_name).then(function () {
	            var index = $scope.cities.indexOf(job.city_name);
	            $scope.cities.splice(index, 1);
	        });
        }
        $rootScope.$broadcast(OTICityManager.Events.CitiesUpdated, $scope.cities);
        saveLoadedScenarios();
    };

    // Scenario controls

    $scope.addCityDropdownIsOpen = false;

    $scope.addScenario = function (scenario) {
        $scope.userScenarios = _.without($scope.userScenarios, scenario);
        $scope.otherScenarios = _.without($scope.otherScenarios, scenario);
        $scope.cities.push(scenario);
        $rootScope.$broadcast(OTICityManager.Events.CitiesUpdated, $scope.cities);
        saveLoadedScenarios();
    };

    // Import City File Upload

    $scope.alert = null;
    $scope.uploadCity = {};
    $scope.uploadFile = null;
    $scope.uploading = false;
    $scope.uploadProgress = 1;

    $scope.onFileSelect = function ($files) {
        $scope.uploadFile = $files.length > 0 ? $files[0] : null;
    };

    $scope.upload = function () {
        $scope.uploading = true;
        $scope.uploadProgress = 1;
        $scope.alert = null;
        var cityName = $scope.uploadCity.name;

        $upload.upload({
            url: '/api/indicators/',
            file: $scope.uploadFile,
            method: 'POST',
            data: {
                city_name: cityName
            },
            fileFormDataName: 'source_file',
        }).progress(function (evt) {
            $scope.uploadProgress = parseInt(100.0 * evt.loaded / evt.total);
        }).success(function () {
            $scope.uploading = false;
            $scope.uploadProgress = 1;
            $scope.cities.push(cityName);
            $scope.cities.sort();
            $rootScope.$broadcast(OTICityManager.Events.CitiesUpdated, $scope.cities);
        }).error(function (data, status) {
            $scope.uploading = false;
            $scope.uploadProgress = 1;
            var msg = status === 400 ? $scope.translations.IMPORTBADFILE : $scope.translations.IMPORTERROR;
            $scope.alert = {
                type: 'danger',
                msg: msg
            };
            console.error(status, data);
        });
    };

    $scope.closeAlert = function () {
        $scope.alert = null;
    };
}]);
