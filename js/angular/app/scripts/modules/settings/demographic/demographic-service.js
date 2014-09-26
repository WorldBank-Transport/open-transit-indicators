'use strict';

/**
 * Service responsible for getting data on demographics settings page
 */
angular.module('transitIndicators')
.factory('OTIDemographicService', ['$resource', function($resource) {
    var otidemographicservice = {};

    // Demographic data loading has multiple steps
    // 1. On a post to the endpoint, returns 201 Created, and will set
    // 		is_valid == true and is_processed == true when shapefile successfully imports
    // 2. POST to the load action with a demographicsConfig object, returns 201 Created,
    //		 and will set is_valid == true and is_loaded == true when values are loaded
    //		 successfully from the specified fields
    // 3. Errors in either step will be saved to the demographicsProblems endpoint,
    //		Can make GET requests to retrieve details.

    otidemographicservice.demographicUpload = $resource('/api/demographics/:id/ ', {}, {
        'update': {
            method: 'PATCH',
            url: '/api/demographics/:id/ '
        },
        // After a load POST, is_valid == true and is_loaded == true if success, otherwise
        //	is_valid reverts to false
        'load': {
        	method: 'POST',
        	url: '/api/demographics/:id/load/ ',
        	params: {
        		population_metric_1: '@population_metric_1',
        		population_metric_2: '@population_metric_2'
        	}
        }
    });

    // Data problems
    otidemographicservice.demographicsProblems = $resource('/api/demographics-problems/:id/ ');

    // Read-Only -- config object used for displaying active fields
    otidemographicservice.demographicsConfig = $resource('/api/config-demographic/ ');

    return otidemographicservice;
}
]);