'use strict';

angular.module('transitIndicators')
.factory('OTIScenariosService',
        ['config',
        function (config) {

    // Temp function to create uuids, remove when STUBS are complete
    var createUUID = function () {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = Math.random()*16|0;
            var v=c=='x'?r:r&0x3|0x8;
            return v.toString(16);
        });
    };

    // STUB data
    var routes = [{
        routeId: createUUID(),
        routeShortName: 'Route 1',
        routeType: 1,
        headway: 10,
        stops: [{
            stopId: createUUID(),
            stopName: 'Stop 1',
            stopDesc: 'Stop 1 Description',
            stopLat: 39.95,
            stopLon: -75.1667
        }, {
            stopId: createUUID(),
            stopName: 'Stop 2',
            stopDesc: 'Stop 2 Description',
            stopLat: 39.98,
            stopLon: -75.2667
        }],
        shapes: [{
            shapeId: createUUID(),
            shapePtLat: 39.95,
            shapePtLon: -75.1667,
            shapePtSequence: 1,
            shapeDistTraveled: 0
        }, {
            shapeId: createUUID(),
            shapePtLat: 39.98,
            shapePtLon: -75.2667,
            shapePtSequence: 2,
            shapeDistTraveled: 10
        }]
    }, {
        routeId: createUUID(),
        routeShortName: 'Route 2',
        routeType: 3,
        headway: 15,
        stops: [{
            stopId: createUUID(),
            stopName: 'Stop 3',
            stopDesc: 'Stop 3 Description',
            stopLat: 39.92,
            stopLon: -75.1667
        }, {
            stopId: createUUID(),
            stopName: 'Stop 4',
            stopDesc: 'Stop 4 Description',
            stopLat: 39.93,
            stopLon: -75.2667
        }],
        shapes: [{
            shapeId: createUUID(),
            shapePtLat: 39.92,
            shapePtLon: -75.1667,
            shapePtSequence: 1,
            shapeDistTraveled: 0
        }, {
            shapeId: createUUID(),
            shapePtLat: 39.93,
            shapePtLon: -75.2667,
            shapePtSequence: 2,
            shapeDistTraveled: 10
        }]

    }];

    var scenarios = [
        {
            id: '1',
            name: 'Mutually Assured Destruction',
            description: 'Test me!',
            samplePeriod: 'morning',
            routes: []
        },
        {
            id: '2',
            name: 'Kobayashi Maru',
            description: 'No win...except that one time.',
            samplePeriod: 'evening',
            routes: []
        }
    ];

    var otiScenariosService = {};

    otiScenariosService.otiScenario = {};

    otiScenariosService.otiRoute = {};

    otiScenariosService.getScenarios = function () {
        // STUB
        // TODO: Replace with resource call
        return scenarios;
    };

    otiScenariosService.getRoutes = function () {
        return routes;
    };

    otiScenariosService.upsertScenario = function (scenario) {
        var found = false;
        _.each(scenarios, function (s, index) {
            if (s.id === scenario.id) {
                scenarios[index] = scenario;
                found = true;
            }
        });
        if (!found) {
            scenarios.push(scenario);
        }
    };

    otiScenariosService.upsertRoute = function (route) {
        var found = false;
        _.each(routes, function (r, index) {
            if (r.routeId === route.routeId) {
                routes[index] = route;
                found = true;
            }
        });
        if (!found) {
            routes.push(route);
        }
    };

    otiScenariosService.Scenario = function () {

        this.id = createUUID();
        this.name = '';
        this.description = '';
        this.samplePeriod = '';
        this.routes = [];
    };

    otiScenariosService.Route = function () {
        this.routeId = createUUID();
        this.routeShortName = '';
        this.routeDesc = '';
        this.routeType = 0;
        this.headway = 0;   // Minutes
        this.stops = [];
        this.shapes = [];
        this.stopTimes = [];
    };

    // Sort by shapePtSequence ascending
    otiScenariosService.Route.prototype.sortShapes = function () {
        this.shapes.sort(function (a, b) {
            return a.shapePtSequence - b.shapePtSequence;
        });
    };

    otiScenariosService.StopTime = function () {
        this.stopId = createUUID();
        this.arrivalTime = '';
        this.departureTime = '';
    };

    otiScenariosService.Shape = function (shape_pt_sequence) {
        this.shapeId = createUUID();
        this.shapePtLat = -999;
        this.shapePtLon = -999;
        this.shapePtSequence = shape_pt_sequence || 0;
        this.shapeDistTraveled = 0;
    };

    otiScenariosService.Stop = function () {
        this.stopId = createUUID();
        this.stopName = '';
        this.stopDesc = '';
        this.stopLat = -999;
        this.stopLon = -999;
    };

    otiScenariosService.stopFromMarker = function (marker) {
        var stop = new otiScenariosService.Stop();
        var latLon = marker.getLatLng();
        stop.stopName = 'Stop ' + (otiScenariosService.otiRoute.stops.length + 1);
        stop.stopLat = latLon.lat;
        stop.stopLon = latLon.lng;
        return stop;
    };

    otiScenariosService.getScenario = function(uuid) {
        // STUB
        // TODO: Replace with scenario resource call based on scenario UUID
        if (uuid < 0 || uuid >= scenarios.length || uuid === undefined) {
            return {};
        }
        return scenarios[uuid];
    };

    otiScenariosService.isReverseView = function (fromState, toState) {
        var views = config.scenarioViews;
        var fromIndex = -1;
        var toIndex = -1;
        _.each(views, function(view, index) {
            if (view.id === fromState.name) {
                fromIndex = index;
            }
            if (view.id === toState.name) {
                toIndex = index;
            }
        });
        return fromIndex > toIndex;
    };

    return otiScenariosService;
}]);