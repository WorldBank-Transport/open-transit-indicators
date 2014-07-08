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
        },
        events: {
            layers: {
                enable: ['mouseover', 'mouseout'],
                logic: 'emit'
            }
        }
    },

    windshaft: {
        port: 8067
    },

    settingsViews: [
        {
            id: 'overview',
            label: 'Overview',
        },
        {
            id: 'upload',
            label: 'GTFS',
        },
        {
            id: 'boundary',
            label: 'Boundary',
        },
        {
            id: 'demographic',
            label: 'Demographic',
        },
        {
            id: 'realtime',
            label: 'Real-Time',
        },
        {
            id: 'configuration',
            label: 'Configuration',
        },
        {
            id: 'users',
            label: 'Users',
        }
    ]

});
