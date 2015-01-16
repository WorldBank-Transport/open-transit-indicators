'use strict';
/**

A small legend directive to replace the default static legend provided
by the angular-leaflet-directive.

Updates the legend when the labels property changes
and sets the legend to hidden when labels is undefined or empty

Legend can either have one of two styles:
'stacked':
--------------------
|_| label          |
|_| label          |
|_| label          |
--------------------

'flat':
----------------
|  |  |  |  |  |
----------------
|0           10|
 ---------------

*/

angular.module('transitIndicators')
.filter('legendValue', function () {
    return function (value) {
        var floatValue = parseFloat(value);
        return isNaN(floatValue) ? value : floatValue.toFixed(2);
    };
})
.directive('otiLegend', [function () {

    var template = [

        '<div ng-show="visible" class="legend-base" ng-class="{ \'legend-flat\': style === \'flat\', \'legend-stacked\': style === \'stacked\',  }">',
        '<div ng-if="title" class="legend-title">{{ title }}</div>',
        '<div class="legend-scale">',
            '<ul class="legend-labels">',
                '<li ng-repeat="color in colors">',
                    '<span style="background:{{ color }};"></span>',
                    '{{ labels[$index] | legendValue }}{{ labels[$index] ? units : "" }}',
                '</li>',
            '</ul>',
        '</div>',
        '<div ng-if="source" class="legend-source">Source: <a href="{{ source.link }}">{{ source.text }}</a></div>',
        '</div>'
    ].join('');
    return {
        restrict: 'AE',
        scope: {
            colors: '=',
            labels: '=',
            style: '=',
            units: '=' // Optional parameter for adding units to numbers
        },
        template: template,
        link: function (scope) {

            scope.visible = true;

            scope.$watch('labels', function (newLabels) {
                scope.visible = !!(newLabels && newLabels.length > 0);
            });

        }
    };
}]);
