'use strict';

angular.module('transitIndicators')
.factory('OTIIndicatorsMapService',
         ['$q', 'OTIIndicatorModel',
          function ($q, OTIIndicatorModel) {

    var otiMapService = {};


    /**
     * Get indicator display value for map
     */
    otiMapService.getIndicatorFormattedValue = function(indicatorId) {
	var dfd = $q.defer();
	var result = OTIIndicatorModel.get({id: indicatorId}, function () {
	    dfd.resolve(result);
	});
	return dfd.promise;
    };

    return otiMapService;
}]);
