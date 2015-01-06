'use strict';
/**

A small legend directive for the coverage indicator's summary stats

*/

angular.module('transitIndicators')
.directive('otiCoverageLegend', [function () {

    var template = [

        '<div ng-show="visible" class="legend-coverage">',
          '<div class="legend-title" translate="MAP.COVERAGE_STATS"></div>',
          '<div ng-show="configData">',
            '<span translate="MAP.BUFF_SIZE"></span>',
            '<span>: </span>',
            '<span class="value">m</span>',
            '<span class="value">{{ configData }}</span>',
          '</div>',
          '<div ng-show="coverage">',
            '<span translate="MAP.PERCENT_COVERED"></span>',
            '<span>: </span>',
            '<span class="value">%</span>',
            '<span class="value">{{ coverage.toLocaleString() }}</span>',
          '</div>',
          '<div ng-show="access1">',
            '<span translate="MAP.ACCESS_1"></span>',
            '<span>: </span>',
            '<span class="value">%</span>',
            '<span class="value">{{ access1.toLocaleString() }}</span>',
          '</div>',
          '<div ng-show="access2">',
            '<span translate="MAP.ACCESS_2"></span>',
            '<span>: </span>',
            '<span class="value">%</span>',
            '<span class="value">{{ access2.toLocaleString() }}</span>',
          '</div>',
        '</div>'
    ].join('');
    return {
        restrict: 'AE',
        scope: {
            visible: '=', // For tracking visibility
            configData: '=',
            coverage: '=',
            access1: '=',
            access2: '='
        },
        template: template,
        link: function (scope) {
        }
    };
}]);
