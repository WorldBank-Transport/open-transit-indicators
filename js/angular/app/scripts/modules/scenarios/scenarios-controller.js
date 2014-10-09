'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosController',
            ['config', '$scope', '$rootScope', '$stateParams', 'OTIEvents', 'OTIIndicatorsMapService', 'OTIScenariosService', 'scenarios',
            function (config, $scope, $rootScope, $stateParams, OTIEvents, OTIIndicatorsMapService, OTIScenariosService, scenarios) {

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

    var setLegend = function () {
        if($rootScope.cache.transitLegend) {
            $scope.leaflet.legend = $rootScope.cache.transitLegend;
            return;
        }
        OTIIndicatorsMapService.getRouteTypeLabels().then(function (labels) {
            var legend = {
                colors: config.gtfsRouteTypeColors,
                labels: labels
            };
            $rootScope.cache.transitLegend = legend;
            $scope.leaflet.legend = legend;
        });
    };


    // EVENTS

    $scope.$on('$stateChangeSuccess', function (event, to, toParams, from, fromParams) {
        $scope.$broadcast('updateHeight');

        // TODO: Why, oh why, sir angular, does this handler get called twice on state change?
        // Did I not please you?
        if (to.name !== from.name) {
            // $scope.back responsible for determining the direction of the x direction animation
            // From: http://codepen.io/ed_conolly/pen/aubKf
            $scope.back = OTIScenariosService.isReverseView(from, to);
        }
    });

    // INIT

    $scope.height = 0;
    $scope.scenarios = scenarios;
    $scope.updateLeafletOverlays(overlays);
    setLegend();

}]);
