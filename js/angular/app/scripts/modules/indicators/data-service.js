'use strict';

angular.module('transitIndicators')
.factory('OTIIndicatorsDataService',
        [
        function () {

    var otiDataService = {};

    otiDataService.IndicatorConfig = {
        'affordability': {
            'mode': 'bar',
            'system': 'number'
        },
        'average_service_freq': {
            'mode': 'bar',
            'system': 'number'
        },
        'coverage_ratio_stops_buffer': {
            'system': 'number'
        },
        'distance_stops': {
            'mode': 'bar',
            'system': 'number'
        },
        'dwell_time': {
            'mode': 'bar',
            'system': 'number'
        },
        'hours_service': {
            'mode': 'bar',
            'system': 'number'
        },
        'length': {
            'mode': 'bar',
            'system': 'number'
        },
        'line_network_density': {
            'system': 'number'
        },
        'lines_roads': {
            'system': 'number'
        },
        'num_routes': {
            'mode': 'pie',
            'system': 'number'
        },
        'num_stops': {
            'mode': 'pie',
            'system': 'number'
        },
        'on_time_perf': {
            'mode': 'bar',
            'system': 'number'
        },
        'ratio_suburban_lines': {
            'system': 'number'
        },
        'regularity_headways': {
            'mode': 'bar',
            'system': 'number'
        },
        'service_freq_weighted': {
            'mode': 'bar',
            'system': 'number'
        },
        'stops_route_length': {
            'mode': 'bar',
            'system': 'number'
        },
        'system_access': {
            'mode': 'bar',
            'system': 'number'
        },
        'system_access_low': {
            'mode': 'bar',
            'system': 'number'
        },
        'time_traveled_stops': {
            'mode': 'bar',
            'system': 'number'
        },
        'travel_time': {
            'mode': 'bar',
            'system': 'number'
        }
    };

    return otiDataService;
}]);