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
    },

    settingsViews: [
        {
            id: 'overview',
            label: 'Overview',
            hasCheckmark: false
        },
        {
            id: 'upload',
            label: 'GTFS',
            hasCheckmark: true
        }/*,
        {
            id: 'boundary',
            label: 'Boundary',
            hasCheckmark: true
        },
        {
            id: 'demographic',
            label: 'Demographic',
            hasCheckmark: true
        },
        {
            id: 'realtime',
            label: 'Real-Time',
            hasCheckmark: true
        },
        {
            id: 'configuration',
            label: 'Configuration',
            hasCheckmark: true
        },
        {
            id: 'users',
            label: 'Users',
            hasCheckmark: false
        },
        {
            id: 'viewmap',
            label: 'View Map',
            hasCheckmark: false
        }*/
    ]

});
