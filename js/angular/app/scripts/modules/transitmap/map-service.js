'use strict';

/**

OTIWindshaft -- client side configuration for our windshaft server

*/
angular.module('transitIndicators')
.factory('OTIMapService', ['$location', '$resource', '$rootScope', '$q',
                           'leafletData', 'windshaftConfig',
                           'OTIIndicatorsService', 'OTIMapStyleService',
        function ($location, $resource, $rootScope, $q,
                  leafletData, windshaftConfig,
                  OTIIndicatorsService, OTIMapStyleService) {

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

    var _selectedTransitModes = '';
    var _availableTransitModes = [];

    var module = {};

    module.Events = {
        VisibleModesSelected: 'OTI:MapService:VisibleModesSelected',
        AvailableModesUpdated: 'OTI:MapService:AvailableModesUpdated'
    };

    module.refreshLayers = function () {
        leafletData.getLayers().then(function (layers) {
            _.each(layers, function(layer) {
                _.each(layer, function(sublayer) {
                    if(sublayer.options.modes !== undefined) {
                        sublayer.options.modes = _selectedTransitModes;
                        if(sublayer.redraw) {
                            sublayer.redraw();
                        } else { // utfgrid
                            sublayer._cache = {};
                            sublayer._update();
                        }
                    }
                });
            });
        });
    };

    module.setTransitModes = function (modes) {
        _selectedTransitModes = modes;
        $rootScope.$broadcast(module.Events.VisibleModesSelected, modes);
    };

    module.getTransitModes = function () {
        return _selectedTransitModes;
    };

    module.setAvailableTransitModes = function (modes) {
        _availableTransitModes = modes;
        $rootScope.$broadcast(module.Events.AvailableModesUpdated, modes);
    };

    module.getAvailableTransitModes = function () {
        return _availableTransitModes;
    };

    // retrieves map information from the server
    module.getMapExtent = function() {
        var r = $resource('/gt/utils/map-info');
        var dfd = $q.defer();

        var result = r.get({}, function() {
            if (result && result.extent) {
                dfd.resolve(result.extent);
            } else {
                dfd.reject('No map extent available');
            }
        });
        return dfd.promise;
    };

    /**
     * Return a legend object in the format expected by the
     * oti-legend directive
     * .style must be set after it returns
     */
    module.getLegendData = function () {
        return OTIIndicatorsService.getRouteTypes().then(function (routetypes) {
            var colors = [];
            var labels = [];
            var modes = [];
            var colorRamp = OTIMapStyleService.routeTypeColorRamp();
            _.chain(routetypes)
                .filter(function(route) { return route.is_used; })
                .each(function(route) {
                    colors.push(colorRamp[route.route_type]);
                    labels.push(route.description);
                    modes.push({
                        id: route.route_type,
                        name: route.description
                    });
                });
            module.setAvailableTransitModes(modes);
            return {
                colors: colors,
                labels: labels
            };
        });
    };

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