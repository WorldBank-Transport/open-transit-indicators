'use strict';

angular.module('transitIndicators')
.factory('OTIMapStyleService',
        [
        function () {

    var defaultColor = '#ffffbf';
    var gtfsRouteTypeColorRamp = [
        '#a6cee3',
        '#1f78b4',
        '#b2df8a',
        '#33a02c',
        '#fb9a99',
        '#e31a1c',
        '#fdbf6f',
        '#ff7f00'
    ];

    var otiMapStyleService = {};

    // Helper function to create labels for a given indicator color ramp
    // Only displays the first and last values for the legend
    var mapLabelsForColorRamp = function (colorRamp, first, last) {
        var len = colorRamp.length;
        var labels = [];
        for (var i = 0; i < len; i++) {
            if (i === 0) {
                labels[i] = first;
            } else if (i === len - 1) {
                labels[i] = last;
            } else {
                labels[i] = '';
            }
        }
        return labels;
    };

    /**
     *  Return an array of colors to display in the flat legend, based on indicator.type
     */
    // TODO: Return color ramps depending on indicator type
    otiMapStyleService.getColorRampForIndicator = function (indicatorType) {
        // All blue color ramp, light to dark
        //return ['#f1eef6', '#bdc9e1', '#74a9cf', '#2b8cbe', '#045a8d'];
        // Divergent color ramp, similar to the one used for route types
        return ['#d7191c', '#fdae61', '#ffffbf', '#abdda4', '#2b83ba'];
    };

    otiMapStyleService.routeTypeColorRamp = function () {
        return gtfsRouteTypeColorRamp;
    };

    otiMapStyleService.defaultColor = defaultColor;

    /**
     * Return a leaflet legend object customized for the passed indicator type and data
     *
     *  @param indicatorType String The indicator type , e.g. 'num_stops'
     *  @param indicatorData Array[Indicator] Array of indicator objects for the indicatorType,
     *                                    filtered by aggregation, sample_period, calculation_job.
     *                                    Used to get min/max values to display on legend
     */
    otiMapStyleService.getLegend = function (indicatorType, indicatorData) {
        var legend = {};
        if (indicatorData && indicatorData.length) {
            var first = _.first(indicatorData);
            var last = _.last(indicatorData);
            if (first && last) {
                var colors = otiMapStyleService.getColorRampForIndicator(indicatorType);
                var labels = mapLabelsForColorRamp(colors, first.value, last.value);
                legend = {
                    colors: colors,
                    labels: labels,
                    style: 'flat'
                };
            }
        }
        return legend;
    };

    return otiMapStyleService;
}]);
