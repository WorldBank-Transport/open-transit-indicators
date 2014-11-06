'use strict';

/**
 * Service responsible for getting data/problems for boundaries
 */
angular.module('transitIndicators')
.factory('OTIBoundaryService', ['$resource', function($resource) {
    return {
        // boundary data
        boundaryUploads: $resource('/api/boundaries/:id/', {}, {
            update: {
                method: 'PATCH',
                url: '/api/boundaries/:id'
            }
        }, {
            stripTrailingSlashes: false
        }),

        // boundary data problems
        boundaryProblems: $resource('/api/boundary-problems/:id/', null, null, {
            stripTrailingSlashes: false
        })
    };
}
]);
