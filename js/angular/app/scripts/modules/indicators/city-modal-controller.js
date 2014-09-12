'use strict';

angular.module('transitIndicators')
.controller('OTICityModalController',
        ['$scope', '$rootScope', '$modalInstance', '$upload', '$translate', 'OTIEvents', 'OTIIndicatorsService', 'cities', 'userScenarios', 'otherScenarios',
        function($scope, $rootScope, $modalInstance, $upload, $translate, OTIEvents, OTIIndicatorsService, cities, userScenarios, otherScenarios) {

    $scope.cities = cities;

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

    // City controls

    $scope.remove = function (city) {
        OTIIndicatorsService.query('DELETE', {
            city_name: city
        }).then(function () {
            var index = $scope.cities.indexOf(city);
            $scope.cities.splice(index, 1);
            $rootScope.$broadcast(OTIEvents.Indicators.CitiesUpdated, $scope.cities);
        });
    };

    // Scenario controls

    $scope.addCityDropdownIsOpen = false;
    // Arrays of 'scenario' objects, current stub in partial only uses a 'name' property
    $scope.userScenarios = userScenarios || [];
    $scope.otherScenarios = otherScenarios || [];

    $scope.addScenario = function (scenario) {
        // TODO: Implement once scenarios are implemented
        console.log('Selected scenario:', scenario.name);
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
            $rootScope.$broadcast(OTIEvents.Indicators.CitiesUpdated, $scope.cities);
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
