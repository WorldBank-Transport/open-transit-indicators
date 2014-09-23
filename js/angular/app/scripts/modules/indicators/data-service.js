'use strict';

angular.module('transitIndicators')
.factory('OTIIndicatorsDataService',
        [
        function () {

    var otiDataService = {};

    otiDataService.IndicatorConfig = {
        'affordability': {
            'mode': 'bar'
        },
        'average_service_freq': {
            'mode': 'bar'
        },
        'distance_stops': {
            'mode': 'bar'
        },
        'dwell_time': {
            'mode': 'bar'
        },
        'hours_service': {
            'mode': 'bar'
        },
        'length': {
            'mode': 'bar'
        },
        'num_routes': {
            'mode': 'pie',
        },
        'num_stops': {
            'mode': 'pie',
        },
        'on_time_perf': {
            'mode': 'bar'
        },
        'regularity_headways': {
            'mode': 'bar'
        },
        'service_freq_weighted': {
            'mode': 'bar'
        },
        'stops_route_length': {
            'mode': 'bar'
        },
        'system_access': {
            'mode': 'bar'
        },
        'system_access_low': {
            'mode': 'bar'
        },
        'time_traveled_stops': {
            'mode': 'bar'
        },
        'travel_time': {
            'mode': 'bar'
        }
    };

    return otiDataService;
}]);