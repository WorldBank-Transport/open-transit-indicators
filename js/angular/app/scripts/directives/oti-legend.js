'use strict';
/**

A small legend directive to replace the default static legend provided
by the angular-leaflet-directive.

Updates the legend when the labels property changes
and sets the legend to hidden when labels is undefined or empty

*/

angular.module('transitIndicators')
.directive('otiLegend', [function () {

    var template = [
        '<div ng-show="visible" class="legend">',
        '  <div ng-repeat="label in labels">',
        '    <div class="outline">',
        '      <i style="background: {{ colors[$index] }}"></i>',
        '    </div>',
        '    <div class="info-label">{{ label }}</div>',
        '  </div>',
        '</div>'
    ].join('');
    return {
        restrict: 'AE',
        scope: {
            colors: '=',
            labels: '='
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