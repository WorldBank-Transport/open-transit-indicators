'use strict';

angular.module('transitIndicators')
.factory('OTIMapService',
        ['$q', '$resource', '$location', 'config',
        function ($q, $resource, $location, config) {

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

    otiMapService.Indicator = $resource('/api/indicators/:id/ ', {id: '@id'}, {
        'update': {
            method: 'PATCH',
            url: '/api/indicators/:id/ '
        }
    });

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

    return otiMapService;
}]);
