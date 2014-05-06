'use strict';

angular.module('transitIndicators')
.controller('GTFSMapController',
        ['config', '$scope', '$state', '$location',
        function (config, $scope, $state, $location) {

    var windshaft_host = $location.protocol() + '://' + $location.host() + ':' + config.windshaft.port;
    var leaflet = {
        layers: {
            'baselayers': {
                'osm': {
                    'name': 'OpenStreetMap',
                    'url': 'http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
                    'type': 'xyz',
                    'layerParams': {},
                    'layerOptions': {}
                }
            },
            overlays: {
                'gtfs_stops': {
                    name: 'Philly GTFS Stops',
                    type: 'xyz',
                    url: windshaft_host + '/database/transit_indicators/table/gtfs_stops/{z}/{x}/{y}.png'
                }
            }
        }
    };
    $scope.$state = $state;

    $scope.leaflet = angular.extend(config.leaflet, leaflet);

}]);

