'use strict';

angular.module('transitIndicators')
.controller('OTIMapController',
        ['config', '$scope', '$state', '$location', 'leafletData',
        function (config, $scope, $state, $location, leafletData) {

    $scope.$state = $state;

    var windshaftHost = $location.protocol() + '://' + $location.host();
    if (config.windshaft && config.windshaft.port) {
        windshaftHost += ':' + config.windshaft.port;
    }

    /* LEAFLET CONFIG */
    var layers = {
        overlays: {
            gtfsshapes: {
                name: 'GTFS Routes',
                type: 'xyz',
                url: windshaftHost + '/tiles/transit_indicators/table/gtfs_shape_geoms/{z}/{x}/{y}.png'
            },
            gtfsstops: {
                name: 'GTFS Stops',
                type: 'xyz',
                url: windshaftHost + '/tiles/transit_indicators/table/gtfs_stops/{z}/{x}/{y}.png',
                visible: true
            },
            stopsutfgrid: {
                name: 'GTFS Stops Interactivity',
                type: 'utfGrid',
                url: windshaftHost + '/tiles/transit_indicators/table/gtfs_stops/{z}/{x}/{y}.grid.json',
                visible: true,
                pluginOptions: { 'useJsonP': false }
            }
            
        }
    };

    var leaflet = {
        layers: angular.extend(config.leaflet.layers, layers)
    };
    $scope.leaflet = angular.extend(config.leaflet, leaflet);
    
    $scope.$on('leafletDirectiveMap.utfgridClick', function(event, leafletEvent) {
        $scope.stopLabel = leafletEvent.data.stop_desc;

        // we need something to bind the popup to, so use a marker with an empty icon
        if (!$scope.leaflet.markers) {
            // use $apply so popup appears right away
            // (otherwise it doesn't show up until the next time the mouse gets moved)
            $scope.$apply(function() {
                $scope.leaflet.markers = { stopMarker : {
                                                            lat: leafletEvent.latlng.lat,
                                                            lng: leafletEvent.latlng.lng,
                                                            message: leafletEvent.data.stop_desc,
                                                            focus: true,
                                                            draggable: false,
                                                            icon: {
                                                                    type: 'div',
                                                                	iconSize: [0, 0],
                                                                	popupAnchor:  [0, 0]
                                                                   }
                                                        }
                                                        
                                          };
            });
        }
        
    });
    
    $scope.$on('leafletDirectiveMap.utfgridMouseout', function(event, leafletEvent) {
        $scope.leaflet.markers = null;
    });
    
}]);

