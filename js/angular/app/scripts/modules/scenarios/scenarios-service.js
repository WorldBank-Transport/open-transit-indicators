'use strict';

angular.module('transitIndicators')
.factory('OTIScenariosService',
        ['config',
        function (config) {

    // STUB data
    var scenarios = [{
            id: '1',
            name: 'Mutually Assured Destruction',
            description: 'Test me!',
            sample_period: 'morning'
        }, {
            id: '2',
            name: 'Kobayashi Maru',
            description: 'No win...except that one time.',
            sample_period: 'evening'
        }];

    var otiScenariosService = {};

    otiScenariosService.getScenarios = function () {
        // STUB
        // TODO: Replace with resource call
        return scenarios;
    };

    otiScenariosService.getScenario = function(uuid) {
        // STUB
        // TODO: Replace with scenario resource call based on scenario UUID
        if (uuid < 0 || uuid >= scenarios.length) {
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