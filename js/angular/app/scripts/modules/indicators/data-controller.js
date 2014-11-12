'use strict';
angular.module('transitIndicators')
.controller('OTIIndicatorsDataController',
            ['$scope', '$state', '$modal',
             'OTICityManager', 'OTIIndicatorChartService',
             'OTIIndicatorModel', 'OTIIndicatorManager', 'OTIIndicatorJobManager',
            function ($scope, $state, $modal,
                      OTICityManager, OTIIndicatorChartService,
                      OTIIndicatorModel, OTIIndicatorManager, OTIIndicatorJobManager) {

    $scope.updating = false;
    $scope.indicatorDetailKey = OTIIndicatorManager.getDescriptionTranslationKey;
    $scope.charts = OTIIndicatorChartService.Charts;
    $scope.selfCityName = OTIIndicatorJobManager.getCurrentCity();

    $scope.chartData = {};

    var filterDataForChartType = function (data, type, aggregation) {
        var chartType = OTIIndicatorChartService.getChartTypeForIndicator(type);
        if (chartType === 'nodata') {
            return data;
        }
        return $scope.charts[chartType].filterFunction(data, aggregation);
    };

    var getIndicatorData = function () {
        $scope.updating = true;
        var period = $scope.sample_period;
        if (period) {

            OTIIndicatorJobManager.getCurrentJob(function (calcJob) {
                var params = {
                    sample_period: period,
                    aggregation: 'mode,system',
                    calculation_job: calcJob
                };
                OTIIndicatorModel.search(params, function (data) {
                    // If there is no indicator data, ask to redirect to the calculation status page
                    // only if we're still on the data page
                    if (!data.length && $state.is('data') && !OTIIndicatorManager.isModalOpen()) {
                        $modal.open({
                            templateUrl: 'scripts/modules/indicators/yes-no-modal-partial.html',
                            controller: 'OTIYesNoModalController',
                            windowClass: 'yes-no-modal-window',
                            resolve: {
                                getMessage: function() {
                                    return 'CALCULATION.REDIRECT';
                                },
                                getList: function() { return null; }
                            }
                        }).result.then(function() {
                            $state.go('calculation');
                        });
                    }

                    var indicators = OTIIndicatorChartService.transformData(data, $scope.cities);
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
            });
        }
    };

    $scope.displayIndicator = function (citydata, type, aggregation) {
        var config = $scope.indicatorConfig;
        var display = !!(config && config[type] && config[type][aggregation] && citydata[aggregation]);
        return display;
    };

    $scope.getModePartialForIndicator = function (type) {
        var chartType = OTIIndicatorChartService.getChartTypeForIndicator(type);
        var url = '/scripts/modules/indicators/charts/:charttype-mode-partial.html';
        return url.replace(':charttype', chartType);
    };

    $scope.indicatorConfig = OTIIndicatorChartService.IndicatorConfig;

    $scope.$on(OTIIndicatorManager.Events.SamplePeriodUpdated, function () {
        getIndicatorData();
    });

    $scope.$on(OTICityManager.Events.CitiesUpdated, function () {
        getIndicatorData();
    });

    getIndicatorData();
}]);
