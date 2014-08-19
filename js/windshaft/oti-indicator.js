var _ = require('underscore');

/**
 * Windshaft logic encapsulating generating SQL and CartoCSS
 * for the OTI indicators
 */

var result_tablename = "indicator_result";

// TODO: Styles for more ntiles
// TODO: Styles for geom types other than lines
var styles = {
    ntiles_5: function () {
        var cartocss = '#' + result_tablename + ' { ' +
            'line-color: #EFF3FF; ' +
            'line-width: 5; ' +
            '[ ntiles_bin > 0 ] { line-color: #c6dbef; } ' +
            '[ ntiles_bin > 1 ] { line-color: #9ecae1; } ' +
            '[ ntiles_bin > 2 ] { line-color: #6baed6; } ' +
            '[ ntiles_bin > 3 ] { line-color: #3182bd; } ' +
            '[ ntiles_bin > 4 ] { line-color: #08519c; } ' +
            '} ';
        return cartocss;
    },
};

/**
 * Defaults for the IndicatorConfig object
 * IndicatorConfig has the following properties:
 * TODO: Docstring once finalized
 */
var IndicatorDefaults = {
    city_name: null,
    city_bounded: false,
    ntiles: 5
};

/**
 * Create new Indicator object, pass an IndicatorConfig object to initialize
 *
 * @param options IndicatorConfig
 *
 * TODO: Allow ntiles other than 5
 */
var Indicator = function (options) {
    this.version = options.version;
    this.type = options.type;
    this.aggregation = options.aggregation;
    this.sample_period = options.sample_period;
    this.options = _.extend(IndicatorDefaults, options);
};

/**
 *  Generate WindShaft sql for an indicator based on an IndicatorConfig object
 *
 * @return String SQL string to be passed to Windshaft
 */
Indicator.prototype.getSql = function () {
    var sqlString =
        "(SELECT value, " +
        "ntile(" + this.options.ntiles + ") over (order by value) as ntiles_bin, " +
        "the_geom " +
        "FROM transit_indicators_indicator " +
        "WHERE type='" + this.type + "' AND aggregation='" + this.aggregation + "' " +
        "AND version='" + this.version + "' AND sample_period_id=" +
        "(SELECT id from transit_indicators_sampleperiod WHERE type='" + this.sample_period + "')" +
        ") as " + result_tablename;
    return sqlString;
};

/**
 * Generate CartoCSS Style for this indicator
 * TODO: Expand the CartoCSS options and finalize API
 */
Indicator.prototype.getStyle = function () {
    var cssKey = 'ntiles_' + this.options.ntiles;
    return styles[cssKey]() || "";
};

exports.Indicator = Indicator;
exports.table = result_tablename;