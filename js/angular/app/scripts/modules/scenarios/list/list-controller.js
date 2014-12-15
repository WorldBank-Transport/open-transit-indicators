'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosListController',
            ['$interval', '$scope', '$state',
            'OTIIndicatorJobManager', 'OTIMapService', 'OTIScenarioManager',
             function ($interval, $scope, $state,
             OTIIndicatorJobManager, OTIMapService, OTIScenarioManager)
{

    // Number of scenarios to list at any given time
    var pageSize = 5;

    var indicatorJobsPollTimer;

    // Update IndicatorJob status
    var getIndicatorJobs = function () {
        var indexed = _.chain($scope.myScenarios)
                      .flatten(true)
                      .indexBy(function(scenario) { return scenario.id; })
                      .value();
        OTIIndicatorJobManager.getJobs($scope.user.id).then(function (result) {
            _.chain(result)
            .sortBy(function (job) { return job.id; })
            .each(function(job) {
                if (indexed[job.scenario]) {
                    indexed[job.scenario].indicator_job_status = job.job_status;
                }
            });
        });
    };

    // Function that gets scenarios for a user
    var getMyScenarios = function () {
        OTIScenarioManager.list($scope.user.id).then(function(results) {
            $scope.myScenarios = _.chain(results).groupBy(function(element, index) {
                return Math.floor(index/pageSize);
            }).toArray().value();
            $scope.$emit('updateHeight');
            getIndicatorJobs();
            indicatorJobsPollTimer = $interval(getIndicatorJobs, 10000); // 10 sec
        });
    };

    $scope.$on('$destroy', function () {
        if (indicatorJobsPollTimer !== undefined) {
            $interval.cancel(indicatorJobsPollTimer);
            indicatorJobsPollTimer = undefined;
        }
    });

    // Function that gets scenarios for colleagues
    var getColleagueScenarios = function () {
        OTIScenarioManager.list().then(function(results) {
            var filteredResults = _.filter(results, function (scenario) {
                return scenario.created_by !== $scope.user.username;
            });
            $scope.colleagueScenarios = _.chain(filteredResults).groupBy(function(element, index) {
                return Math.floor(index/pageSize);
            }).toArray().value();
            $scope.$emit('updateHeight');
        });
    };

    $scope.create = function () {
        OTIScenarioManager.create();
        $state.go('new');
    };

    $scope.deleteScenario = function (scenario) {
        // delete django scenario object, which will trigger deletion of scenario database
        OTIScenarioManager.delete(scenario.db_name);

        // now refresh list
        var pageIndex = $scope.myScenarioPage; // need to look within the correct sub-array in the following line
        $scope.myScenarios[pageIndex].splice(_.indexOf($scope.myScenarios[pageIndex], _.find($scope.myScenarios[pageIndex], function(obj) {
            return (obj.db_name === scenario.db_name);
            })), 1);
    };

    $scope.edit = function (scenario) {
        OTIScenarioManager.set(scenario);
        $state.go('new');
    };

    $scope.copy = function (scenario) {
        OTIScenarioManager.copy(scenario);
        $state.go('new');
    };

    $scope.updateHeight = function () {
        $scope.$emit('updateHeight');
    };

    // Init
    $scope.myScenarios = null;
    $scope.colleagueScenarios = null;

    $scope.colleagueScenarioPage = 0;
    $scope.myScenarioPage = 0;

    getMyScenarios();
    getColleagueScenarios();
    OTIMapService.setScenario();
}]);
