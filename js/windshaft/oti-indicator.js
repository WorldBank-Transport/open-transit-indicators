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
    gtfs_shapes: function () {
        var cartocss =  '#' + result_tablename + ' { ' +
            'line-color: #000000;' +
            'line-opacity: 1;' +
            'line-width: 4;' +
            // Not a complete list, not sure what the best way to provide a complete
            // color palette for each route type would be. There are currently 100+
            // route types defined in the gtfs_route_types table.
            '[route_type=0] {line-color: #a6cee3; } ' +
            '[route_type=1] {line-color: #1f78b4; } ' +
            '[route_type=2] {line-color: #b2df8a; } ' +
            '[route_type=3] {line-color: #33a02c; } ' +
            '[route_type=4] {line-color: #fb9a99; } ' +
            '[route_type=5] {line-color: #e31a1c; } ' +
            '[route_type=6] {line-color: #fdbf6f; } ' +
            '[route_type=7] {line-color: #ff7f00; } ' +
            '}';
        return cartocss;
    },
    gtfs_stops: function () {
       var cartocss =  '#' + result_tablename + ' {' +
            'marker-opacity: 1;' +
            'marker-line-color: #FFF;' +
            'marker-line-width: 2.5;' +
            'marker-fill: #B40903;' +
            'marker-width: 12;' +
            'marker-line-opacity: 1;' +
            'marker-placement: point;' +
            'marker-type: ellipse;' +
            'marker-allow-overlap: true;' +
            '}';
        return cartocss;
    }
};

/**
 * Encapsulates windshaft display logic for the
 * gtfs_shapes table
 */
var GTFSShapes = function () {};

GTFSShapes.prototype.getSql = function () {
    var sqlString =
        "(SELECT distinct r.route_id, r.route_type, s.shape_id, s.the_geom as the_geom " +
        "FROM gtfs_shape_geoms AS s LEFT JOIN gtfs_trips t ON s.shape_id = t.shape_id " +
        "LEFT JOIN gtfs_routes r ON r.route_id = t.route_id WHERE r.route_id IS NOT NULL) " +
        "AS " + result_tablename;
    return sqlString;
};

GTFSShapes.prototype.getStyle = function () {
    return styles.gtfs_shapes() || "";
};

/**
 * Encapsulates windshaft display logic for the gtfs_stops and
 * utfgrid gtfs_stops_info tables
 */
var GTFSStops = function () {};

GTFSStops.prototype.getSql = function (filetype) {
    var table = filetype === 'utfgrid' ? 'gtfs_stops_info' : 'gtfs_stops';
    var sqlString =
        "(SELECT * FROM " + table + ") " +
        "AS " + result_tablename;
    return sqlString;
};

GTFSStops.prototype.getStyle = function () {
    return styles.gtfs_stops() || "";
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
        "AND version_id='" + this.version + "' AND sample_period_id=" +
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
exports.GTFSShapes = GTFSShapes;
exports.GTFSStops = GTFSStops;
exports.table = result_tablename;
