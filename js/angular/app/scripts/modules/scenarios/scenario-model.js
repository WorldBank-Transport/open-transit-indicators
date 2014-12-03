'use strict';

angular.module('transitIndicators')
.factory('OTIScenarioModel', ['$resource', function ($resource) {

    var Statuses = {
        complete: ['complete'],
        processing: ['queued', 'processing'],
        error: ['error']
    };

    var module = $resource('/api/scenarios/:db_name/', {db_name: '@db_name'}, {
        update: {
            method: 'PATCH'
        },
        delete: {
            method: 'DELETE'
        }
    }, {
        stripTrailingSlashes: false
    });

    // Extend resource instance with helpful methods
    angular.extend(module.prototype, {
        isProcessing: function () {
            return (_.indexOf(Statuses.processing, this.job_status) >= 0);
        },
        isError: function () {
            return (_.indexOf(Statuses.error, this.job_status) >= 0);
        },
        isComplete: function () {
            return (_.indexOf(Statuses.complete, this.job_status) >= 0);
        }
    });

    return module;

}]);