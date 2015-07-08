'use strict';
/**
Directive that allows a user to download job travelshed indicator results as a GeoTIFF
*/

angular.module('transitIndicators')
.directive('otiGeotiffDownload', ['$document', '$modal', '$window', 'OTIIndicatorModel', 'OTITypes',
function ($document, $modal, $window, OTIIndicatorModel, OTITypes) {
    var template = [
        '<a ng-click="downloadGeotiff()" type="button" ',
        'tooltip="{{\'UI.TOOLTIP.DOWNLOAD_JOB_GEOTIFF\'|translate}}" ',
        'class="button button--small button--secondary default glyphicon glyphicon-globe">',
        '</a>'
    ].join('');

    return {
        restrict: 'E',
        scope: {
            job: '='
        },
        template: template,
        link: function (scope) {

            var travelshedIndicators = [];

            // go get the translated names for our jobs travelshed indicators from the API
            function init() {
                OTITypes.getIndicatorTypes().then(function(indicators) {
                    travelshedIndicators = [];
                    _.each(indicators, function(obj, key) {
                        if (key.match(/travelshed$/)) {
                            obj.name = key;
                            travelshedIndicators.push(obj);
                        }
                    });
                });
            }

            function getGeotiff(params, fileName) {
                OTIIndicatorModel.geotiff(params, function (data) {
                    if (window.navigator.msSaveOrOpenBlob) {
                        // IE doesn't support blob URL generation,
                        // but it does have a special API for this
                        window.navigator.msSaveOrOpenBlob(data.geotiff, fileName);
                    } else {
                        // For other browsers, create a temporary element
                        // and simulate a click to download
                        var element = angular.element('<a>');
                        var url = $window.URL || $window.webkitURL;
                        element.attr('href', url.createObjectURL(data.geotiff));
                        element.attr('download', fileName);
                        $document.find('body').append(element);
                        element[0].click();
                        element.remove();
                    }
                });
            }

            /**
             * Download a GeoTIFF of accessiblility results for a given job
             */
            scope.downloadGeotiff = function () {
                $modal.open({
                    templateUrl: 'scripts/modules/indicators/geotiff-modal-partial.html',
                    controller: 'OTIGeotiffModalController',
                    windowClass: 'geotiff-modal-window',
                    resolve: {
                        getIndicators: function() {
                            return travelshedIndicators;
                        },
                        getMessage: function() {
                            return 'CALCULATION.DOWNLOAD_GEOTIFF_MODAL_TEXT';
                        }
                    }
                }).result.then(function(indicator) {
                    var params = {
                        JOBID: scope.job.id,
                        INDICATOR: indicator
                    };
                    var cityName = scope.job.city_name.replace(/ /g, '_').toLowerCase();
                    var fileName = cityName + '_' + indicator + '_' + scope.job.id + '.tiff';

                    getGeotiff(params, fileName);
                });
            };

            init();
        }
    };
}]);
