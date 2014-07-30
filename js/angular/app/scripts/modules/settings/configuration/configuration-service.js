'use strict';

/**
 * Service responsible for getting/updating global configuration
 */
angular.module('transitIndicators')
.factory('OTIConfigurationService', ['$resource', function($resource) {
    return {
        configs: $resource('/api/config/:id', {}, {
            update: {
                method: 'PATCH',
                url: '/api/config/:id'
            }
        })
    };
}
]);
