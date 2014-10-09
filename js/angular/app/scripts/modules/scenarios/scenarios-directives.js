'use strict';
/**

Helper directives for the scenario pages

*/

angular.module('transitIndicators')

/**
    Sets .scenario element height and then updates height whenever
    an 'updateHeight' event is broadcast on the scope, best fired from $stateChangeSuccess
*/
.directive('autoHeight', ['$timeout', function ($timeout) {

    return {
        restrict: 'A',
        scope: true,
        link: function(scope, element) {

            var updateScenarioHeight = function () {
                var pageHeight = element.find('.scenario-page').outerHeight();
                var headerHeight = element.find('.scenario-headings').outerHeight();
                var height = pageHeight + headerHeight;
                element.css({
                    'height': (height)
                });
            };

            $timeout(updateScenarioHeight);

            // TODO: Is there a better way to update the height on a view switch?
            scope.$on('updateHeight', updateScenarioHeight);
        }
    };
}]);
