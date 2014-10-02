'use strict';

/**
 * Service responsible for getting data/problems for boundaries
 */
angular.module('transitIndicators')
.factory('OTIBoundaryService', ['$resource', function($resource) {
    return {
        // boundary data
        boundaryUploads: $resource('/api/boundaries/:id', {}, {
            update: {
                method: 'PATCH',
                url: '/api/boundaries/:id'
            }
        }),

        // boundary data problems
        boundaryProblems: $resource('/api/boundary-problems/:id')
    };
}
]);
