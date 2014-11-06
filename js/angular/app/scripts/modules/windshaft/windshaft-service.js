'use strict';

/**

OTIWindshaft -- client side configuration for our windshaft server

*/
angular.module('transitIndicators')
.factory('OTIWindshaftService', ['$location', 'windshaftConfig',
        function ($location, windshaftConfig) {

    /**
     * Return windshaft hostname, including port, if configured in windshaftConfig.port
     * @return String hostname
     */
    var getWindshaftHost = function () {
        var windshaftHost = $location.protocol() + '://' + $location.host();
        if (windshaftConfig && windshaftConfig.port) {
            windshaftHost += ':' + windshaftConfig.port;
        }
        return windshaftHost;
    };

    var module = {};

    /**
     * Create windshaft urls for leaflet map
     *
     * @param filetype: String, either png or utfgrid
     */
    module.indicatorUrl = function (filetype) {
        var url = getWindshaftHost();
        url += '/tiles/transit_indicators/{calculation_job}/{type}/{sample_period}/{aggregation}' +
               '/{z}/{x}/{y}';
        url += (filetype === 'utfgrid') ? '.grid.json?interactivity=indicator_id&modes={modes}' : '.png?modes={modes}';
        return url;
    };

    /**
     * Create windshaft url for gtfs shapes overlay -- always png
     * The calculation_job, sample_period, aggregation do not matter
     */
    module.gtfsShapesUrl = function () {
        var url = getWindshaftHost();
        url += '/tiles/transit_indicators/0/gtfs_shapes/morning/route/{z}/{x}/{y}.png?modes={modes}';
        return url;
    };

    /**
     * Create windshaft url for gtfs stops overlay
     * The calculation_job, sample_period, aggregation do not matter
     * Uses the stop_routes column for utfgrid interactivity
     *
     * @param filetype: String either png or utfgrid
     */
    module.gtfsStopsUrl = function (filetype) {
        var url = getWindshaftHost();
        url += '/tiles/transit_indicators/0/gtfs_stops/morning/route/{z}/{x}/{y}';
        url += (filetype === 'utfgrid') ? '.grid.json?interactivity=stop_routes&modes={modes}'
                : '.png?modes={modes}';
        return url;
    };

    /**
     * Create windshaft url for boundary
     */
    module.boundaryUrl = function () {
        var url = getWindshaftHost();
        url += '/tiles/transit_indicators/0/datasources_boundary/morning/route/{z}/{x}/{y}.png';
        return url;
    };

    return module;
}]);