'use strict';

angular.module('transitIndicators')
.factory('OTIIndicatorsMapService',
         ['$q', '$http', '$resource', '$location', 'windshaftConfig', 'OTIMapStyleService',
	  'OTIIndicatorsService',
          function ($q, $http, $resource, $location, windshaftConfig, OTIMapStyleService,
		    OTIIndicatorsService) {

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
     * Get indicator display value for map
     */
    otiMapService.getIndicatorFormattedValue = function(indicatorId) {
	var dfd = $q.defer();
	var result = OTIIndicatorsService.Indicator.get({id: indicatorId}, function () {
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
            otiMapService.modes = modes;
            return {
                colors: colors,
                labels: labels
            };
        });
    };

    return otiMapService;
}]);
