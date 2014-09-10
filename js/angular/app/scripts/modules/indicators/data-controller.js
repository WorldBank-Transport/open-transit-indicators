'use strict';
angular.module('transitIndicators')
.controller('OTIIndicatorsDataController',
            ['$scope',
            function ($scope) {

    $scope.exampleData = [
        { key: 'Light Rail', y: 5 },
        { key: 'Bus', y: 24 },
        { key: 'Funicular', y: 1 }
    ];

    $scope.xFunction = function () {
        return function (data) {
            return data.key;
        };
    };

    $scope.yFunction = function () {
        return function (data) {
            return data.y;
        };
    };

    $scope.getIndicatorDescriptionTranslationKey = function(key) {
        return 'INDICATOR_DESCRIPTION.' + key;
    };

}]);