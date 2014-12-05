'use strict';

angular.module('transitIndicators')
.factory('OTIScenarioManager', ['$q', 'OTIScenarioModel', '$rootScope',
         function ($q, OTIScenarioModel, $rootScope) {

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

    module.copy = function (base_scenario) {
        var newScenario = new OTIScenarioModel();
        newScenario.name = base_scenario.name;
        newScenario.description = base_scenario.description;
        newScenario.sample_period = base_scenario.sample_period;
        newScenario.job_status = null;
        newScenario.base_scenario = base_scenario.db_name;
        newScenario.create_date = Date.now();
        newScenario.created_by = $rootScope.user.username;
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
