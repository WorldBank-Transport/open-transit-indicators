'use strict';

angular.module('transitIndicators')
.factory('OTIScenarioManager', ['$q', 'OTIScenarioModel',
         function ($q, OTIScenarioModel) {

    var scenario = {};

    var module = {};

    module.create = function () {
        var newScenario = new OTIScenarioModel();
        newScenario.name = '';
        newScenario.description = '';
        newScenario.sample_period = null;
        newScenario.job_status = null;
        newScenario.base_scenario = null;
        scenario = newScenario;
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

    module.delete = function(scenario_db) {
        OTIScenarioModel.delete({'db_name': scenario_db});
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
