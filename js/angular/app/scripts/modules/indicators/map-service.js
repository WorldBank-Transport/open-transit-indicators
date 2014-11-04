'use strict';

angular.module('transitIndicators')
.factory('OTIIndicatorsMapService',
        ['$q', '$http', '$resource', '$location', 'windshaftConfig', 'OTIMapStyleService',
        function ($q, $http, $resource, $location, windshaftConfig, OTIMapStyleService) {

    var otiMapService = {};

    // retrieves map information from the server
    otiMapService.getMapInfo = function() {
        var r = $resource('/gt/utils/map-info');
        var dfd = $q.defer();

        var result = r.get({}, function() {
            dfd.resolve(result);
        });
        return dfd.promise;
    };

    /**
     * Get the route types from the API and return
     */
    otiMapService.getRouteTypes = function () {
        var r = $resource('/api/gtfs-route-types/');
        var dfd = $q.defer();
        var result = r.query({}, function () {
            dfd.resolve(result);
        });
        return dfd.promise;
    };

    /**
     * Return a legend object in the format expected by the
     * oti-legend directive
     * .style must be set after it returns
     */
    otiMapService.getLegendData = function () {
        return otiMapService.getRouteTypes().then(function (routetypes) {
            var colors = [];
            var labels = [];
            var colorRamp = OTIMapStyleService.routeTypeColorRamp();
            _.chain(routetypes)
                .filter(function(route) { return route.is_used; })
                .each(function(route) {
                    colors.push(colorRamp[route.route_type]);
                    labels.push(route.description);
                });
            return {
                colors: colors,
                labels: labels,
            };
        });
    };

    /**
     * Create windshaft urls for leaflet map
     *
     * @param filetype: String, either png or utfgrid
     */
    otiMapService.getIndicatorUrl = function (filetype) {
        var url = otiMapService.getWindshaftHost();
        url += '/tiles/transit_indicators/{calculation_job}/{type}/{sample_period}/{aggregation}' +
               '/{z}/{x}/{y}';
        url += (filetype === 'utfgrid') ? '.grid.json?interactivity=value' : '.png';
        return url;
    };

    /**
     * Create windshaft url for gtfs shapes overlay -- always png
     * The calculation_job, sample_period, aggregation do not matter
     */
    otiMapService.getGTFSShapesUrl = function () {
        var url = otiMapService.getWindshaftHost();
        url += '/tiles/transit_indicators/0/gtfs_shapes/morning/route/{z}/{x}/{y}.png';
        return url;
    };

    /**
     * Create windshaft url for gtfs stops overlay
     * The calculation_job, sample_period, aggregation do not matter
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
     * Create windshaft url for boundary
     */
     otiMapService.getBoundaryUrl = function () {
        var url = otiMapService.getWindshaftHost();
        url += '/tiles/transit_indicators/0/datasources_boundary/morning/route/{z}/{x}/{y}.png';
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
