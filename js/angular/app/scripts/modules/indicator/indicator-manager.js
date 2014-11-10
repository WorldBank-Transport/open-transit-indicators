'use strict';

angular.module('transitIndicators')
.factory('OTIIndicatorManager', ['$cookieStore', '$rootScope',
        function ($cookieStore, $rootScope) {

    var nullJob = 0;
    var COOKIE_STORE_INDICATOR = 'OTIIndicatorManager:IndicatorConfig';
    var COOKIE_STORE_SAMPLE_PERIOD = 'OTIIndicatorManager:SamplePeriod';

    /**
     * Thin wrapper for Indicator used in the controller for setting the map properties
     */
    function IndicatorConfig () {
        this.calculation_job = nullJob;
        this.type = 'num_stops';
        this.sample_period = 'morning';
        this.aggregation = 'route';
        this.modes = '';
    }

    var _config = $cookieStore.get(COOKIE_STORE_INDICATOR) || new IndicatorConfig();
    var _samplePeriod = $cookieStore.get(COOKIE_STORE_SAMPLE_PERIOD) || 'morning';

    var module = {};

    module.Events = {
        IndicatorConfigUpdated: 'OTI:IndicatorManager:ConfigUpdated',
        SamplePeriodUpdated: 'OTI:IndicatorManager:SamplePeriodUpdated'
    };

    module.getConfig = function () {
        return _config;
    };

    module.setConfig = function (config) {
        _config = angular.extend(_config, config);
        $cookieStore.put(COOKIE_STORE_INDICATOR, _config);
        $rootScope.$broadcast(module.Events.IndicatorConfigUpdated, _config);
    };

    module.getSamplePeriod = function () {
        return _samplePeriod;
    };

    module.setSamplePeriod = function (samplePeriod) {
        _samplePeriod = samplePeriod;
        $cookieStore.put(COOKIE_STORE_SAMPLE_PERIOD, _samplePeriod);
        $rootScope.$broadcast(module.Events.SamplePeriodUpdated, _samplePeriod);
    };

    module.getDescriptionTranslationKey = function (key) {
        return 'INDICATOR_DESCRIPTION.' + key;
    };

    return module;
}]);
