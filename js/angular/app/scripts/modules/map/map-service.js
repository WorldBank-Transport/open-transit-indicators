'use strict';

angular.module('transitIndicators')
.factory('OTIMapService',
        ['$q', '$http', '$resource', '$location', 'config',
        function ($q, $http, $resource, $location, config) {

    var otiMapService = {};
    var nullVersion = 0;

    // retrieves map information from the server
    otiMapService.getMapInfo = function() {
        var r = $resource('/gt/map-info');
        var dfd = $q.defer();

        var result = r.get({}, function() {
            dfd.resolve(result);
        });
        return dfd.promise;
    };

    otiMapService.Indicator = $resource('/api/indicators/:id/ ', {id: '@id'}, {
        'update': {
            method: 'PATCH',
            url: '/api/indicators/:id/ '
        }
    });

    /**
     * Thin wrapper for Indicator used in the controller for setting the map properties
     */
    otiMapService.IndicatorConfig = function (config) {
        this.version = config.version || nullVersion;
        this.type = config.type;
        this.sample_period = config.sample_period;
        this.aggregation = config.aggregation;
    };

    /**
     * Return windshaft hostname, including port, if configured in config.windshaft.port
     * @return String hostname
     */
    otiMapService.getWindshaftHost = function () {
        var windshaftHost = $location.protocol() + '://' + $location.host();
        if (config.windshaft && config.windshaft.port) {
            windshaftHost += ':' + config.windshaft.port;
        }
        return windshaftHost;
    };

    /**
     * Create windshaft urls for leaflet map
     *
     * @param indicator: otiMapService.Indicator instance
     * @param filetype: String, either png or utfgrid
     */
    otiMapService.getIndicatorUrl = function (filetype) {
        var url = otiMapService.getWindshaftHost();
        url += '/tiles/transit_indicators/{version}/{type}/{sample_period}/{aggregation}' +
               '/{z}/{x}/{y}';
        url += (filetype === 'utfgrid') ? '.grid.json?interactivity=value' : '.png';
        return url;
    };

    /**
     * Get the current indicator version
     *
     * @param callback: function to call after request is made, has a single argument 'version'
     */
    otiMapService.getIndicatorVersion = function (callback) {
        $http.get('/api/indicators_version/').success(function (data) {
            var version = data.current_version || nullVersion;
            callback(version);
        }).error(function (error) {
            callback(nullVersion);
        });
    };

    return otiMapService;
}]);
