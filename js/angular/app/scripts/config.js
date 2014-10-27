// for attribution
var esri_attr_open = '<span class="esriAttributionItem" style="display: inline;">';
var esri_attr_close = '</span><img ' +
    'src="https://serverapi.arcgisonline.com/jsapi/arcgis/3.5/js/esri/images/map/logo-med.png" ' +
    'alt="Powered by Esri" class="esri-attribution-logo" style="position: absolute; top: -38px; ' +
    'right: 2px; display: block;">';
var osm_attr = 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
    '<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>';

// ESRI map server
var esri_map_url = 'https://{s}.arcgisonline.com/ArcGIS/rest/services/';

// These colors should match the colors found in
//  js/windshaft/oti-indicators.js:style.gtfs_shapes()
// TODO: Figure out a way to DRY this...
var gtfsRouteTypeColors = [
    '#a6cee3',
    '#1f78b4',
    '#b2df8a',
    '#33a02c',
    '#fb9a99',
    '#e31a1c',
    '#fdbf6f',
    '#ff7f00'
];

angular.module('transitIndicators').constant('config', {

    debug: true,

    languages: {
        'English': 'en',
        'Chinese': 'zh',
        'Vietnamese': 'vi',
        'Spanish': 'es'
    },

    defaultLanguage: 'en',

    worldExtent: {
        southWest: {
          lat: -57.0,
          lng: -168.5
        },
        northEast: {
          lat: 73.25,
          lng: 158.1
        }
    },

    leaflet: {
        center: {
        },
        bounds: {
            southWest: {
                lat: -57.0,
                lng: -168.5
            },
            northEast: {
                lat: 73.25,
                lng: 158.1
            }
        },
        layers: {
            baselayers: {
                stamentonerlite: {
                    name: 'Stamen Toner Lite',
                    type: 'xyz',
                    url: 'https://stamen-tiles-{s}.a.ssl.fastly.net/toner-lite/{z}/{x}/{y}.png',
                    layerOptions: {
                        attribution: 'Map tiles by <a href="http://stamen.com">Stamen Design</a>, ' +
                            '<a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> ' +
                            '&mdash; ' + osm_attr,
                        subdomains: ['a', 'b', 'c', 'd'],
                        minZoom: 3,
                        maxZoom: 20,
                        continuousWorld: true
                    }
                },
                stamentoner: {
                    name: 'Stamen Toner',
                    type: 'xyz',
                    url: 'https://stamen-tiles-{s}.a.ssl.fastly.net/toner/{z}/{x}/{y}.png',
                    layerOptions: {
                        attribution: 'Map tiles by <a href="http://stamen.com">Stamen Design</a>, ' +
                            '<a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> ' +
                            '&mdash; ' + osm_attr,
                        subdomains: ['a', 'b', 'c', 'd'],
                        minZoom: 3,
                        maxZoom: 20,
                        continuousWorld: true
                    }
                },
                topographic: {
                    name: 'Streets',
                    type: 'xyz',
                    url: esri_map_url + 'World_Topo_Map/MapServer/tile/{z}/{y}/{x}.png',
                    layerOptions: {
                        attribution: esri_attr_open + 'DC GIS, DDOT, DRES, OCTO, VITA, ' +
                            'GSA, Esri, DeLorme, HERE, Intermap,  iPC, TomTom, USGS, ' +
                            'METI/NASA, USDA, EPA' + esri_attr_close,
                        subdomains: ['server', 'services'],
                        minZoom: 3,
                        maxZoom: 20,
                        continuousWorld: true
                    }
                },
                esri_imagery: {
                    name: 'Imagery',
                    type: 'xyz',
                    url: esri_map_url + 'World_Imagery/MapServer/tile/{z}/{y}/{x}.png',
                    layerOptions: {
                        attribution: esri_attr_open + 'Esri, DigitalGlobe, GeoEye, ' +
                            'i-cubed, USDA, USGS, AEX, Getmapping, Aerogrid, IGN, IGP, ' +
                            'swisstopo, and the GIS User Community' + esri_attr_close,
                        subdomains: ['server', 'services'],
                        minZoom: 3,
                        maxZoom: 20,
                        continuousWorld: true
                    }
                },
                mapabc: {
                    name: 'mapabc',
                    type: 'xyz',
                    url: 'http://emap{s}.mapabc.com/mapabc/maptile?x={x}&y={y}&z={z}',
                    layerOptions: {
                        attribution: 'MapABC',
                        subdomains: ['1', '2', '3'],
                        minZoom: 3,
                        maxZoom: 20,
                        continuousWorld: true
                    }
                }
            },
            overlays: {}
        },
        markers: [],
        legend: {},
        defaults: {
            minZoom: 3,
            maxZoom: 16,
            zoomControl: true,
            zoomControlPosition: 'bottomleft',
            doubleClickZoom: true,
            scrollWheelZoom: true,
            keyboard: true,
            dragging: true,
            controls: {
                layers: {
                    collapsed: true,
                    position: 'bottomleft',
                    visible: true
                }
            }
        },
        events: {
            layers: {
                enable: ['mouseover', 'mouseout'],
                logic: 'emit'
            }
        }
    },

    gtfsRouteTypeColors: gtfsRouteTypeColors,

    settingsViews: [
        {
            id: 'overview'
        },
        {
            id: 'upload'
        },
        {
            id: 'boundary'
        },
        {
            id: 'demographic'
        },
        {
            id: 'realtime'
        },
        {
            id: 'configuration'
        },
        {
            id: 'users'
        }
    ],

    scenarioViews: [
        {
            id: 'list'
        },
        {
            id: 'new'
        },
        {
            id: 'new-success'
        },
        {
            id: 'routes'
        },
        {
            id: 'route-edit'
        },
        {
            id: 'route-stops'
        },
        {
            id: 'route-shapes'
        },
        {
            id: 'route-times'
        },
        {
            id: 'route-done'
        }
    ]

});
