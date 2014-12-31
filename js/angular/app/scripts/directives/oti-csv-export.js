'use strict';
/**
Directive that allows a user to download indicator results as a CSV
*/

angular.module('transitIndicators')
.directive('otiCsvExport', ['$document', '$modal', '$window', 'OTIIndicatorModel',
function ($document, $modal, $window, OTIIndicatorModel) {
    var template = [
        '<a ng-click="exportCsv()" type="button" ',
        'class="button button--small button--secondary default glyphicon glyphicon-save">',
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
             * Exports indicator results for a given job as a CSV and downloads to a user's machine
             */
            scope.exportCsv = function () {
                var params = {
                    calculation_job: scope.job.id,
                    format: 'csv'
                };
                var cityName = scope.job.city_name.replace(/ /g, '_').toLowerCase();
                var fileName = cityName + '_' + scope.job.id + '.csv';

                $modal.open({
                    templateUrl: 'scripts/modules/indicators/ok-modal-partial.html',
                    controller: 'OTIOKModalController',
                    windowClass: 'ok-modal-window',
                    resolve: {
                        getMessage: function() {
                            return 'CALCULATION.EXPORT_MODAL_TEXT';
                        }
                    }
                });

                OTIIndicatorModel.csv(params, function (data) {
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
