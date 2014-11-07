'use strict';

angular.module('transitIndicators')
.factory('OTIIndicatorsMapService',
         ['$q', '$http', '$resource', '$location', 'windshaftConfig', 'OTIMapStyleService',
	  'OTIIndicatorsService',
          function ($q, $http, $resource, $location, windshaftConfig, OTIMapStyleService,
		    OTIIndicatorsService) {

    var otiMapService = {};


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

    return otiMapService;
}]);
