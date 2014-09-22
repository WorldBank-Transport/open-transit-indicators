'use strict';

angular.module('transitIndicators')
.directive('indicatorChartRow', [function () {
    return {
        restrict: 'E',
        scope: {
            indicator: '@',
            data: '@',
            config: '@'
        },
        templateUrl: 'scripts/modules/indicators/indicator-row-partial.html',
        controller: ['$scope', '$element', '$attrs', function($scope, $element, $attrs){

        }],
        link: function (scope, element, attrs) {

        }
    };
}]);