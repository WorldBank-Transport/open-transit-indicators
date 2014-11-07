'use strict';
/**

A directive for dynically generating popovers on transit system stops

*/

angular.module('transitIndicators').directive('stopup', [function () {

    return {
        restrict: 'AEC',
        templateUrl: 'scripts/modules/scenarios/route-stops/stopup-template.html',
        scope: true
    };
}]);
