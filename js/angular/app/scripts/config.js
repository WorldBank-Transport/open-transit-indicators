angular.module('transitIndicators').constant('config', {

    leaflet: {
        center: {
            lat: 39.95,
            lng: -75.1667,
            zoom: 12
        },
        defaults: {
            maxZoom: 16
        }
    },

    windshaft: {
        port: 4040
    }

});
