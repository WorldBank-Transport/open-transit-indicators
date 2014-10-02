'use strict';

angular.module('transitIndicators')
.factory('OTIIndicatorsMapService',
        ['$q', '$http', '$resource', '$location', 'windshaftConfig',
        function ($q, $http, $resource, $location, windshaftConfig) {

    var otiMapService = {};

    // retrieves map information from the server
    otiMapService.getMapInfo = function() {
        var r = $resource('/gt/map-info');
        var dfd = $q.defer();

        var result = r.get({}, function() {
            dfd.resolve(result);
        });
        return dfd.promise;
    };

    /**
     * Get the route type labels from the API and return in order as an array
     */
    otiMapService.getRouteTypeLabels = function () {
        var r = $resource('/api/gtfs-route-types/');
        var dfd = $q.defer();
        var result = r.query({}, function () {
            dfd.resolve(_.pluck(result, 'description'));
        });
        return dfd.promise;
    };

    /**
     * Create windshaft urls for leaflet map
     *
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
     * Create windshaft url for gtfs shapes overlay -- always png
     * The version, sample_period, aggregation do not matter
     */
    otiMapService.getGTFSShapesUrl = function () {
        var url = otiMapService.getWindshaftHost();
        url += '/tiles/transit_indicators/0/gtfs_shapes/morning/route/{z}/{x}/{y}.png';
        return url;
    };

    /**
     * Create windshaft url for gtfs stops overlay
     * The version, sample_period, aggregation do not matter
     * Uses the stop_routes column for utfgrid interactivity
     *
     * @param filetype: String either png or utfgrid
     */
    otiMapService.getGTFSStopsUrl = function (filetype) {
        var url = otiMapService.getWindshaftHost();
        url += '/tiles/transit_indicators/0/gtfs_stops/morning/route/{z}/{x}/{y}';
        url += (filetype === 'utfgrid') ? '.grid.json?interactivity=stop_routes' : '.png';
        return url;
    };

    /**
     * Return windshaft hostname, including port, if configured in windshaftConfig.port
     * @return String hostname
     */
    otiMapService.getWindshaftHost = function () {
        var windshaftHost = $location.protocol() + '://' + $location.host();
        if (windshaftConfig && windshaftConfig.port) {
            windshaftHost += ':' + windshaftConfig.port;
        }
        return windshaftHost;
    };

    return otiMapService;
}]);
