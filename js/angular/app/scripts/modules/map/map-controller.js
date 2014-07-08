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
    
    /* The directive approach (using layer instead)
    var utfgrid = {
          name: 'GTFS Stops Interactivity',
          type: 'utfGrid',
          url: windshaftHost + '/tiles/transit_indicators/table/gtfs_stops/{z}/{x}/{y}.grid.json',
          pluginOptions: { 'useJsonP': false }
    }; 
    */
    
    var leaflet = {
        layers: angular.extend(config.leaflet.layers, layers)
        //utfgrid: utfgrid
    };
    $scope.leaflet = angular.extend(config.leaflet, leaflet);
    
    $scope.stopLabel = "";
    
    $scope.$on('leafletDirectiveMap.utfgridMouseover', function(event, leafletEvent) {
        /*
        console.log(event);
        console.log(leafletEvent);
        */
        $scope.stopLabel = leafletEvent.data.stop_desc;
    });
    
    /* Logging some things that might be useful to look at in future
    $scope.myMap = leafletData.getMap().then(function(map) {
      console.log(map);
    });
    
    $scope.$on('leafletDirectiveMap.mouseover', function(event, leafletEvent) {
        console.log("moused over map!");
        console.log(event);
        console.log(leafletEvent);
    });
    
    $scope.myUTFGrid = leafletData.getUTFGrid().then(function(utf) {
      console.log('got utf...');
      console.log(utf);
      
      utf.on('mouseover', function(e) {
          console.log(e);
      });
    });
    
    $scope.myLayers = leafletData.getLayers().then(function(layers) {
        console.log(layers);
    });

    */
    
}]);

