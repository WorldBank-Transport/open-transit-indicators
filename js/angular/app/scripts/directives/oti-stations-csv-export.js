'use strict';
/**
Directive that allows a user to download indicator results as a CSV
*/

angular.module('transitIndicators')
.directive('otiStationsCsvExport', ['$document', '$modal', '$window', 'OTIIndicatorModel',
function ($document, $modal, $window, OTIIndicatorModel) {
    var template = [
        '<a ng-click="exportStationsCsv()" type="button" ',
        'tooltip="{{\'UI.TOOLTIP.DOWNLOAD_STATIONS_CSV\'|translate}}" ',
        'class="button button--small button--secondary default glyphicon glyphicon-flag">',
        '</a>'
    ].join('');

    return {
        restrict: 'E',
        scope: {
            job: '='
        },
        template: template,
        link: function (scope) {
            /**
             * Exports station stats for a given city as a CSV and downloads to a user's machine
             */
            scope.exportStationsCsv = function () {
                var params = {'jobId': scope.job.id};
                var cityName = scope.job.city_name.replace(/ /g, '_').toLowerCase();
                var fileName = cityName + '_stations.csv';

                OTIIndicatorModel.stationsCsv(params, function (data) {
                    if (window.navigator.msSaveOrOpenBlob) {
                        // IE doesn't support blob URL generation,
                        // but it does have a special API for this
                        window.navigator.msSaveOrOpenBlob(data.csv, fileName);
                    } else {
                        // For other browsers, create a temporary element
                        // and simulate a click to download
                        var element = angular.element('<a>');
                        var url = $window.URL || $window.webkitURL;
                        element.attr('href', url.createObjectURL(data.csv));
                        element.attr('download', fileName);
                        $document.find('body').append(element);
                        element[0].click();
                        element.remove();
                    }
                });
            };
        }
    };
}]);
