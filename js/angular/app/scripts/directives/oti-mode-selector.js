'use strict';

angular.module('transitIndicators')
.directive('otiModeSelector', ['$translate', function ($translate) {

    var allModes = '';
    $translate('TERM.ALL_MODES').then(function(text) {
        allModes = text;
    });

    return {
        restrict: 'AE',
        scope: {
            modes: '=',
            updatemap: '=',
            legendLabels: '='
        },
        templateUrl: 'scripts/directives/oti-mode-selector.html',
        link: function (scope) {
            scope.visible = false;
            scope.choice = allModes;

            scope.pickMode = function (mode) {
                if (mode === null) {
                    scope.updatemap('');
                    scope.choice = allModes;
                    return;
                }
                scope.updatemap(mode.id);
                scope.choice = mode.name;
            };

            scope.dropdown = {
                isopen: false
            };

            scope.$watch('legendLabels', function (newLabels) {
                scope.visible = !!(newLabels && newLabels.length > 0);
            });
        }
    };
}]);
