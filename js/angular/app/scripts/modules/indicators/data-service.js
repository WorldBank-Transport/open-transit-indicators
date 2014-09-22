'use strict';

angular.module('transitIndicators')
.factory('OTIIndicatorsDataService',
        [
        function () {

    var otiDataService = {};

    otiDataService.IndicatorConfig = {
        'num_stops': {
            'mode': 'pie',
        },
        'distance_stops': {
            'mode': 'bar'
        }
    };

    return otiDataService;
}]);