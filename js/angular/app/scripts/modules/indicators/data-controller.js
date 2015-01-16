'use strict';
angular.module('transitIndicators')
.controller('OTIIndicatorsDataController',
            ['$scope', '$state', '$modal', '$q',
             'OTICityManager', 'OTIIndicatorChartService',
             'OTIIndicatorModel', 'OTIIndicatorManager', 'OTIIndicatorJobManager',
            function ($scope, $state, $modal, $q,
                      OTICityManager, OTIIndicatorChartService,
                      OTIIndicatorModel, OTIIndicatorManager, OTIIndicatorJobManager) {

    $scope.updating = false;
    $scope.refetch = false;
    $scope.indicatorDetailKey = OTIIndicatorManager.getDescriptionTranslationKey;
    $scope.charts = OTIIndicatorChartService.Charts;
    $scope.selfCityName = OTIIndicatorJobManager.getCurrentCity();
    $scope.city_names = [];

    $scope.chartData = {};

    var citiesUpdated = false;

    var getIndicatorData = function () {
        if(!$scope.sample_period) {
            return;
        }
        if($scope.updating === true) { // lock, because bad things happen when multiple updates happen at once
            $scope.refetch = true;
            return;
        }

        // Don't fetch data if the cities aren't available yet
        if(!citiesUpdated && !$scope.cities.length) {
            return;
        }

        $scope.updating = true;
        $scope.city_names = _.pluck($scope.cities, 'city_name');
        $scope.indicatorData = null;
        $scope.chartData = {};
        var period = $scope.sample_period;
        var indicatorjobs = $scope.cities;
        var promises = _.map(indicatorjobs, function(indicatorjob) { // fetch data
            var deferred = $q.defer();
            var params = {
                sample_period: period,
                aggregation: 'mode,system',
                calculation_job: indicatorjob.id
            };
            OTIIndicatorModel.search(params, function (data) {
                deferred.resolve(data);
            }, function () {
                deferred.reject(null);
            });
            return deferred.promise;
        });

        $q.all(promises).then(function (results) { // process data
            var data = _.chain(results).without(null).flatten(true).value();

            if (!data.length && $state.is('data') && !OTIIndicatorManager.isModalOpen()) {
                OTIIndicatorManager.setModalStatus(true);
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
                    OTIIndicatorManager.setModalStatus(false);
                    $state.go('calculation');
                }, function() {
                    OTIIndicatorManager.setModalStatus(false);
                });
            }

            var indicators = OTIIndicatorChartService.transformData(data, indicatorjobs);
            // Populate $scope.chartData, because filterDataForChartType
            // can't be called inside the view
            if ($scope.refetch === true) {
                $scope.updating = false;
                $scope.refetch = false;
                getIndicatorData();
                return;
            }
            $scope.indicatorData = null;
            $scope.chartData = {};
            for (var indicator in indicators) {
                if (!$scope.chartData[indicator]) {
                    $scope.chartData[indicator] = {};
                }
                for(var i in indicators[indicator].indicator_jobs) {
                    $scope.chartData[indicator][i] =
                        filterDataForChartType(
                            indicators[indicator].indicator_jobs[i], indicator, 'mode');
                }
            }
            $scope.indicatorData = indicators;
            $scope.updating = false;
        });
    };

    var filterDataForChartType = function (data, type, aggregation) {
        var chartType = OTIIndicatorChartService.getChartTypeForIndicator(type);
        if (chartType === 'nodata') {
            return data;
        }
        return $scope.charts[chartType].filterFunction(data, aggregation);
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
        citiesUpdated = true;
        getIndicatorData();
    });

    getIndicatorData();
}]);
