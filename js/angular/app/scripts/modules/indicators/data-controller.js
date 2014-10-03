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

    $scope.chartData = {};

    var filterDataForChartType = function (data, type, aggregation) {
        var chartType = OTIIndicatorsDataService.getChartTypeForIndicator(type);
        if (chartType === 'nodata') {
            return data;
        }
        return $scope.charts[chartType].filterFunction(data, aggregation);
    };

    var getIndicatorData = function () {
        $scope.updating = true;
        var period = $scope.sample_period;
        if (period) {
            var params = {
                sample_period: period,
                aggregation: 'mode,system'
            };

            OTIIndicatorsService.query('GET', params).then(function (data) {
                var indicators = OTIIndicatorsDataService.transformData(data, $scope.cities);
                $scope.indicatorData = null;
                $scope.chartData = {};
                // Populate $scope.chartData, because filterDataForChartType
                // can't be called inside the view
                for (var indicator in indicators) {
                    if (!$scope.chartData[indicator]) {
                        $scope.chartData[indicator] = {};
                    }
                    for(var city in indicators[indicator].cities) {
                        $scope.chartData[indicator][city] = 
                            filterDataForChartType(
                                indicators[indicator].cities[city], indicator, 'mode');
                    }
                }
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
        var chartType = OTIIndicatorsDataService.getChartTypeForIndicator(type);
        var url = '/scripts/modules/indicators/charts/:charttype-mode-partial.html';
        return url.replace(':charttype', chartType);
    };

    $scope.indicatorConfig = OTIIndicatorsDataService.IndicatorConfig;

    $scope.$on(OTIEvents.Indicators.SamplePeriodUpdated, function () {
        getIndicatorData();
    });

    $scope.$on(OTIEvents.Indicators.CitiesUpdated, function () {
        cache = {};
        getIndicatorData();
    });

    getIndicatorData();
}]);
