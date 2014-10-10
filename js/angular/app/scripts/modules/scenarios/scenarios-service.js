'use strict';

angular.module('transitIndicators')
.factory('OTIScenariosService',
        ['config',
        function (config) {

    // STUB data
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

    // Temp function to create uuids, remove when STUBS are complete
    var createUUID = function () {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = Math.random()*16|0;
            var v=c=='x'?r:r&0x3|0x8;
            return v.toString(16);
        });
    };

    var otiScenariosService = {};

    otiScenariosService.otiScenario = {};

    otiScenariosService.getScenarios = function () {
        // STUB
        // TODO: Replace with resource call
        return scenarios;
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

    otiScenariosService.Scenario = function () {

        this.id = createUUID();
        this.name = '';
        this.description = '';
        this.samplePeriod = '';
        this.routes = [];
    };

    otiScenariosService.Route = function () {
        this.routeId = createUUID(),
        this.routeShortName = '';
        this.routeDesc = '';
        this.routeType = 0;
        this.headway = 0;   // Minutes
        this.stops = [];
        this.shapes = [];
        this.stopTimes = [];
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