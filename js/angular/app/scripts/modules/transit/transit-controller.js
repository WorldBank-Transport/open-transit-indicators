'use strict';
angular.module('transitIndicators')
.controller('OTITransitController',
            ['$scope', '$rootScope', 'OTIEvents', 'OTIMapService', 'OTITransitMapService',
            function ($scope, $rootScope, OTIEvents, OTIMapService, OTITransitMapService) {

    // Every map view should reset to the base scenario on load
    OTIMapService.setScenario();
    var overlays = OTITransitMapService.createTransitMapOverlayConfig();

    var updateLegend = function () {
        OTIMapService.getLegendData().then(function (legend) {
            legend.style = 'stacked';
            $rootScope.cache.transitLegend = legend;
            $scope.leaflet.legend = legend;
        });
    };

    var setLegend = function () {
        if($rootScope.cache.transitLegend) {
            $scope.leaflet.legend = $rootScope.cache.transitLegend;
            return;
        }
        updateLegend();
    };

    var initialize = function () {
        $scope.updateLeafletOverlays(overlays);
        setLegend();
    };

    $scope.$on('leafletDirectiveMap.utfgridClick', function (event, leafletEvent) {
        $scope.leaflet.markers.length = 0;
        if (leafletEvent && leafletEvent.data && leafletEvent.data.stop_routes) {
            $scope.$apply(function () {
                var marker = {
                    lat: leafletEvent.latlng.lat,
                    lng: leafletEvent.latlng.lng,
                    message: leafletEvent.data.stop_routes,
                    focus: true,
                    draggable: false,
                    icon: {
                        type: 'div',
                        iconSize: [0, 0],
                        popupAnchor:  [0, 0]
                    }
                };
                $scope.leaflet.markers.push(marker);
            });
        }
    });

    $rootScope.$on('$translateChangeSuccess', function() {
        updateLegend();
    });

    // This may not be the best place to update the legend on GTFS
    // update, but most of the other legend updating code was here
    $rootScope.$on(OTIEvents.Settings.Upload.GTFSDone, function () {
        OTIMapService.getLegendData().then(function (legend) {
            // only update cache so we don't show legend on the
            // settings view
            $rootScope.cache.transitLegend = legend;
            $rootScope.cache.transitLegend.style = 'stacked';
        });
    });

    initialize();
}]);
