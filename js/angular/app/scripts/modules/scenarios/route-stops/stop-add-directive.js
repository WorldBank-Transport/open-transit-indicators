'use strict';
/**

A directive for dynamically generating popovers on transit system stops

*/

angular.module('transitIndicators').directive('stopAdd', [function () {

    return {
        restrict: 'AEC',
        templateUrl: 'scripts/modules/scenarios/route-stops/stop-add-template.html',
        scope: true
    };
}]);
