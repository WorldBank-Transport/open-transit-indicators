'use strict';
angular.module('transitIndicators')
.controller('OTIScenariosController',
            ['config', '$scope', '$rootScope', '$state', '$stateParams', 'OTIEvents', 'OTIIndicatorsMapService', 'OTIScenariosService', 'scenarios',
            function (config, $scope, $rootScope, $state, $stateParams, OTIEvents, OTIIndicatorsMapService, OTIScenariosService, scenarios) {

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
        // $scope.back responsible for determining the direction of the x direction animation
        // From: http://codepen.io/ed_conolly/pen/aubKf
        $scope.back = OTIScenariosService.isReverseView(from, to);

        $scope.$broadcast('updateHeight');
    });

    // SCOPE

    /**
     * Switch to a new scenario view. Prefer use of this to directly using ui-sref when
     *  switching between scenario child views.
     */
    $scope.transition = function (stateId, uuid) {
        // Required state param cannot be null or undefined
        // If it is, it will cause two transitions, first to new state with uuid: null, then
        // the router sends a second transition with null param massaged to ''

        // STUB
        // TODO : scenario uuid selection logic when that is figured out
        var uuidParam = uuid || '';
        $state.go(stateId, {'uuid': uuidParam});
    };


    // INIT

    $scope.height = 0;
    $scope.scenarios = scenarios;
    $scope.updateLeafletOverlays(overlays);
    setLegend();

}]);
