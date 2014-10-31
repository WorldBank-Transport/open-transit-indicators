'use strict';

angular.module('transitIndicators')
.factory('OTIScenarioManager', ['$q', 'OTIScenarioModel',
         function ($q, OTIScenarioModel) {

    var scenario = {};

    var module = {};

    module.create = function () {
        scenario = new OTIScenarioModel();
    };

    module.get = function() {
        return scenario;
    };

    module.set = function (newScenario) {
        scenario = newScenario;
    };

    module.clear = function () {
        scenario = {};
    };

    module.list = function (createdBy) {
        var queryParams = {};
        if (createdBy) {
            queryParams.created_by = createdBy;
        }
        var dfd = $q.defer();
        OTIScenarioModel.query(queryParams,function (result) {
            dfd.resolve(result);
        }, function (error) {
            console.error('OTIScenarioModel.list(): ', error);
            dfd.resolve([]);
        });
        return dfd.promise;
    };

    return module;

}]);
