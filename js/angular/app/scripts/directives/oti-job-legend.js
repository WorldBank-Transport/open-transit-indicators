'use strict';
/**

A small legend directive for the jobs indicator's weighted overlay

*/

angular.module('transitIndicators')
.filter('legendValue', function () {
    return function (value) {
        var floatValue = parseFloat(value);
        return isNaN(floatValue) ? value : floatValue.toFixed(2);
    };
})
.directive('otiJobLegend', [function () {

    var template = [

        '<div ng-show="jobsLegend" class="legend legend-jobs">',
          '<div class="legend-title" translate="MAP.JOBS_INDICATOR_TITLE"></div>',
          '<div class="legend-scale">',
            '<span class="min" translate="MAP.JOBS_INDICATOR_FEWER"></span>',
            '<ul class="legend-labels">',
              '<li ng-repeat="color in colors">',
                '<span class="legend-value" style="background:{{ color }};"></span>',
                '{{ labels[$index] | legendValue }}',
              '</li>',
            '</ul>',
            '<span class="max" translate="MAP.JOBS_INDICATOR_MORE"></span>',
          '</div>',
          '<div ng-if="source" class="legend-source">Source: <a href="{{ source.link }}">{{ source.text }}</a></div>',
        '</div>'
    ].join('');
    return {
        restrict: 'AE',
        scope: {
            jobsLegend: '=' // For tracking visibility
        },
        template: template,
        link: function (scope) {

            scope.visible = true;

            scope.colors = ['#E8EDDB', '#DCE8D4', '#BEDBAD', '#A0CF88',
                            '#81C561', '#4BAF48', '#1CA049', '#3A6D35'];

        }
    };
}]);
