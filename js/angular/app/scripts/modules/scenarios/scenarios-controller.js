'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosController',
            ['config', '$scope', '$rootScope', '$state', '$stateParams', 'OTIEvents',
             'OTIIndicatorsMapService', 'samplePeriods',
             'samplePeriodI18N', 'routeTypes',
             function (config, $scope, $rootScope, $state, $stateParams, OTIEvents,
                       OTIIndicatorsMapService, samplePeriods,
                       samplePeriodI18N, routeTypes)
{

    // PRIVATE

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

    var isReverseView = function (fromState, toState) {
        var views = config.scenarioViews;
        var fromIndex = -1;
        var toIndex = -1;
        _.each(views, function(view, index) {
            if (view.id === fromState.name) {
                fromIndex = index;
            }
            if (view.id === toState.name) {
                toIndex = index;
            }
        });
        return fromIndex > toIndex;
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

    // EVENTS

    $scope.$on('$stateChangeSuccess', function (event, to, toParams, from) {
        // $scope.back responsible for determining the direction of the x direction animation
        // From: http://codepen.io/ed_conolly/pen/aubKf
        $scope.back = isReverseView(from, to);

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

    setLegend();

}]);
