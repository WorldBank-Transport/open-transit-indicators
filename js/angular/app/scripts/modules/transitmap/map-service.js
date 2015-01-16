'use strict';

/**

OTIWindshaft -- client side configuration for our windshaft server

*/
angular.module('transitIndicators')
.factory('OTIMapService', ['$cookieStore', '$location', '$resource', '$rootScope', '$q',
                           'leafletData', 'windshaftConfig',
                           'OTIIndicatorManager', 'OTITypes', 'OTIMapStyleService',
        function ($cookieStore, $location, $resource, $rootScope, $q,
                  leafletData, windshaftConfig,
                  OTIIndicatorManager, OTITypes, OTIMapStyleService) {

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

    var BASE_SCENARIO = 'transit_indicators';
    var _selectedTransitModes = '';
    var _availableTransitModes = [];
    var _scenario = BASE_SCENARIO;

    var module = {};

    module.overlays = {};

    module.Events = {
        VisibleModesSelected: 'OTI:MapService:VisibleModesSelected',
        AvailableModesUpdated: 'OTI:MapService:AvailableModesUpdated',
        ScenarioUpdated: 'OTI:MapService:ScenarioUpdated'
    };

    module.closePopup = function () {
        leafletData.getMap().then(function (map) {
            map.closePopup();
        });
    };

    module.BASE_SCENARIO = BASE_SCENARIO;

    module.refreshLayers = function () {
        leafletData.getMap().then(function (map) {
            map.eachLayer(function (layer) {
                if(layer && layer.options) {
                    var redraw = false;
                    if (layer.options.modes !== undefined) {
                        layer.options.modes = _selectedTransitModes;
                        redraw = true;
                    }
                    if (layer.options.type ) {
                        var indicator = OTIIndicatorManager.getConfig();
                        angular.extend(layer.options, indicator);
                        redraw = true;
                    }
                    if (layer.options.scenario) {
                        layer.options.scenario = module.getScenario();
                        redraw = true;
                    }
                    if (layer.wmsParams) {
                        // jobs travelshed WMS layer
                        var job = OTIIndicatorManager.getConfig().calculation_job;
                        layer.options.jobId = job;
                        // tell WMS to redraw by setting its parameters
                        // http://leafletjs.com/reference.html#tilelayer-wms-options
                        layer.setParams(layer.options);
                        redraw = true;
                    }
                    if (redraw) {
                        if (layer.redraw) {         // leaflet tile layer
                            layer.redraw();
                        } else if (layer._update) { // leaflet utfgrid layer
                            layer._cache = {};
                            layer._update();
                        }
                    }
                }
            });
        });
    };

    module.setScenario = function (scenario) {
        _scenario = scenario || BASE_SCENARIO;
        $rootScope.$broadcast(module.Events.ScenarioUpdated, _scenario);
        module.refreshLayers();
    };

    module.getScenario = function () {
        return _scenario;
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

    // retrieves travel shed jobs range (min/max for legend)
    module.getTravelShedRange = function(jobId) {
        var r = $resource('/gt/travelshed/jobs/minmax');
        var dfd = $q.defer();

        var result = r.get({ JOBID: jobId }, function() {
            if (result) {
                dfd.resolve(result);
            } else {
                dfd.reject('No travel shed range available');
            }
        });
        return dfd.promise;
    };

    /**
     * Retrieves demographic range
     *
     * @param demographicName: String, type of demographic to retrieve
     */
    module.getDemographicRange = function(demographicName) {
        var r = $resource('/api/demographics-ranges/');
        var dfd = $q.defer();

        var result = r.get({ type: demographicName }, function() {
            if (result) {
                dfd.resolve(result);
            } else {
                dfd.reject('No demographic range available for: ', demographicName);
            }
        });
        return dfd.promise;
    };

    /**
     * Retrieves population one range
     */
    module.getPopOneRange = function() {
        return module.getDemographicRange('population_metric_1');
    };

    /**
     * Retrieves population two range
     */
    module.getPopTwoRange = function() {
        return module.getDemographicRange('population_metric_2');
    };

    /**
     * Retrieves destination one range
     */
    module.getDestOneRange = function() {
        return module.getDemographicRange('destination_metric_1');
    };

    /**
     * Return a legend object in the format expected by the
     * oti-legend directive
     * .style must be set after it returns
     */
    module.getLegendData = function () {
        return OTITypes.getRouteTypes().then(function (routetypes) {
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
        url += '/tiles/{scenario}/{calculation_job}/{type}/{sample_period}/{aggregation}' +
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
        url += '/tiles/{scenario}/0/gtfs_shapes/morning/route/{z}/{x}/{y}.png?modes={modes}';
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
        url += '/tiles/{scenario}/0/gtfs_stops/morning/route/{z}/{x}/{y}';
        url += (filetype === 'utfgrid') ? '.grid.json?interactivity=stop_routes&modes={modes}'
                : '.png?modes={modes}';
        return url;
    };

    /**
     * Create windshaft url for boundary
     */
    module.boundaryUrl = function () {
        var url = getWindshaftHost();
        url += '/tiles/' + BASE_SCENARIO + '/0/datasources_boundary/morning/route/{z}/{x}/{y}.png';
        return url;
    };

    /**
     * Create windshaft url for demographics data
     */
    module.demographicsUrl = function () {
        var url = getWindshaftHost();
        url += '/tiles/' + BASE_SCENARIO + '/0/datasources_demographics/morning/route/{z}/{x}/{y}.png';
        url += '?metric={metric}';
        return url;
    };

    return module;
}]);
