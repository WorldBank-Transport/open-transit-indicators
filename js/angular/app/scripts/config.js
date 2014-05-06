angular.module('transitIndicators').constant('config', {

    leaflet: {
        center: {
            lat: 39.95,
            lng: -75.1667,
            zoom: 12
        },
        layers: {
            baselayers: {
                osm: {
                    name: 'OpenStreetMap',
                    url: 'http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
                    type: 'xyz',
                    layerParams: {},
                    layerOptions: {}
                }
            }
        },
        defaults: {
            maxZoom: 16
        }
    },

    windshaft: {
        port: 8080
    }

});
