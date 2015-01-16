'use strict';
/**

A small legend directive for the jobs indicator's weighted overlay

*/

angular.module('transitIndicators')
.filter('legendValue', function () {
    return function (value) {
        var floatValue = parseFloat(value);

        // Show a maximum of 2 decimal places and include commas
        return isNaN(floatValue) ? value : parseFloat(floatValue.toFixed(2), 10).toLocaleString();
    };
})
.directive('otiJobLegend', [function () {
    return {
        restrict: 'AE',
        scope: {
            visible: '=', // For tracking visibility
            colors: '=',
            labels: '=',
            title: '=',
            range: '='
        },
        templateUrl: 'scripts/directives/oti-job-legend.html'
    };
}]);
