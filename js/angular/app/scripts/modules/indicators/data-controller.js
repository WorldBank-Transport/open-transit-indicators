'use strict';
angular.module('transitIndicators')
.controller('OTIIndicatorsDataController',
            ['$scope', 'OTIEvents', 'OTIIndicatorsService', 'OTIIndicatorsDataService',
            function ($scope, OTIEvents, OTIIndicatorsService, OTIIndicatorsDataService) {

    $scope.updating = false;
    $scope.indicatorDetailKey = OTIIndicatorsService.getIndicatorDescriptionTranslationKey;
    $scope.charts = OTIIndicatorsDataService.Charts;
    $scope.selfCityName = OTIIndicatorsService.selfCityName;
    var colors = OTIIndicatorsDataService.Colors;

    var getIndicatorData = function () {
        $scope.updating = true;
        var period = $scope.sample_period;
        if (period) {
            var params = {
                sample_period: period
            };

            OTIIndicatorsService.query('GET', params).then(function (data) {
                var indicators = OTIIndicatorsDataService.transformData(data, $scope.cities);
                $scope.indicatorData = null;
                $scope.indicatorData = indicators;
                $scope.updating = false;
            }, function (error) {
                console.error('Error getting indicator data:', error);
                $scope.updating = false;
            });
        }
    };

    $scope.displayIndicator = function (type, aggregation) {
        var config = $scope.indicatorConfig;
        var display = !!(config && config[type] && config[type][aggregation]);
        return display;
    };

    $scope.getModePartialForIndicator = function (type) {
        var config = $scope.indicatorConfig;
        var chartType = config && config[type] && config[type].mode ? config[type].mode : 'nodata';
        var url = '/scripts/modules/indicators/charts/:charttype-mode-partial.html';
        return url.replace(':charttype', chartType);
    };

    $scope.indicatorConfig = OTIIndicatorsDataService.IndicatorConfig;

    $scope.getIndicatorDescriptionTranslationKey = function(key) {
        return 'INDICATOR_DESCRIPTION.' + key;
    };

    $scope.$on(OTIEvents.Indicators.SamplePeriodUpdated, function () {
        getIndicatorData();
    });

    $scope.$on(OTIEvents.Indicators.CitiesUpdated, function () {
        cache = {};
        getIndicatorData();
    });

    getIndicatorData();
}]);
