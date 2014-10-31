'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosListController',
            ['$scope', '$state', 'OTIScenarioManager',
             function ($scope, $state, OTIScenarioManager)
{

    // Number of scenarios to list at any given time
    var pageSize = 5;

    // Function that gets scenarios for a user
    var getMyScenarios = function () {
        OTIScenarioManager.list($scope.user.id).then(function(results) {
            $scope.myScenarios = _.chain(results).groupBy(function(element, index) {
                return Math.floor(index/pageSize);
            }).toArray().value();
            $scope.$broadcast('updateHeight');
        });
    };

    // Function that gets scenarios for colleagues
    var getColleagueScenarios = function () {
        OTIScenarioManager.list().then(function(results) {
            var filteredResults = _.filter(results, function (scenario) {
                return scenario.created_by !== $scope.user.username;
            });
            $scope.colleagueScenarios = _.chain(filteredResults).groupBy(function(element, index) {
                return Math.floor(index/pageSize);
            }).toArray().value();
            $scope.$broadcast('updateHeight');
        });
    };

    $scope.create = function () {
        OTIScenarioManager.create();
        $state.go('new');
    };

    $scope.edit = function (scenario) {
        OTIScenarioManager.set(scenario);
        $state.go('new');
    };

    // Init
    $scope.myScenarios = null;
    $scope.colleagueScenarios = null;

    $scope.colleagueScenarioPage = 0;
    $scope.myScenarioPage = 0;

    getMyScenarios();
    getColleagueScenarios();
}]);
