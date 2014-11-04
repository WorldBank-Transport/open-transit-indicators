'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosController',
            ['config', '$scope', '$rootScope', '$state', '$stateParams', 'OTIEvents',
             'OTIIndicatorsMapService', 'OTIScenariosService', 'scenarios', 'samplePeriods',
             'samplePeriodI18N', 'routeTypes',
             function (config, $scope, $rootScope, $state, $stateParams, OTIEvents,
                       OTIIndicatorsMapService, OTIScenariosService, scenarios, samplePeriods,
                       samplePeriodI18N, routeTypes)
{

    // PRIVATE

    // Number of scenarios to list at any given time
    var pageSize = 5;

    var overlays = {
        gtfs_shapes: {
            name: 'Transit Routes',
            type: 'xyz',
            url: OTIIndicatorsMapService.getGTFSShapesUrl(),
            visible: true
        },
        gtfs_stops: {
            name: 'Transit Stops',
            type: 'xyz',
            url: OTIIndicatorsMapService.getGTFSStopsUrl('png'),
            visible: true
        },
        gtfs_stops_utfgrid: {
            name: 'Transit Stops Interactivity',
            type: 'utfGrid',
            url: OTIIndicatorsMapService.getGTFSStopsUrl('utfgrid'),
            visible: true,
            pluginOptions: { 'useJsonP': false }
        }
    };

    var setLegend = function () {
        if($rootScope.cache.transitLegend) {
            $scope.leaflet.legend = $rootScope.cache.transitLegend;
            return;
        }
        OTIIndicatorsMapService.getLegendData().then(function (legend) {
            $rootScope.cache.transitLegend = legend;
            $scope.leaflet.legend = legend;
        });
    };

    // Function that gets scenarios for a user
    var getMyScenarios = function () {
        OTIScenariosService.getScenarios($scope.user.id).then(function(results) {
            $scope.myScenarios = _.chain(results).groupBy(function(element, index) {
                return Math.floor(index/pageSize);
            }).toArray().value();
            $scope.$broadcast('updateHeight');
        });
    };

    // Function that gets scenarios for colleagues
    var getColleagueScenarios = function () {
        OTIScenariosService.getScenarios().then(function(results) {
            var filteredResults = _.filter(results, function (scenario) {
		return scenario.created_by != $scope.user.username;
	    });
            $scope.colleagueScenarios = _.chain(filteredResults).groupBy(function(element, index) {
                return Math.floor(index/pageSize);
            }).toArray().value();
            $scope.$broadcast('updateHeight');
        });
    };

    // EVENTS

    $scope.$on('$stateChangeSuccess', function (event, to, toParams, from) {
        // $scope.back responsible for determining the direction of the x direction animation
        // From: http://codepen.io/ed_conolly/pen/aubKf
        $scope.back = OTIScenariosService.isReverseView(from, to);

        $scope.$broadcast('updateHeight');

        if (to.parent.name === 'scenario') {
            $scope.page = to.name;
        }

        // TODO: Add logic to lock navigation out of an edit view if $scope.scenario.id
        //       is not defined
    });

    // INIT

    $scope.height = 0;

    $scope.samplePeriods = samplePeriods;
    $scope.samplePeriodI18N = samplePeriodI18N;
    $scope.routeTypes = routeTypes;
    $scope.page = '';

    $scope.updateLeafletOverlays(overlays);

    $scope.myScenarios = null;
    $scope.colleagueScenarios = null;

    $scope.colleagueScenarioPage = 0;
    $scope.myScenarioPage = 0;

    getMyScenarios();
    getColleagueScenarios();
    setLegend();

}]);
