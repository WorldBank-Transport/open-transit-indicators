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
            '[ ntiles_bin > 0 ] { line-color: #d7191c; } ' +
            '[ ntiles_bin > 1 ] { line-color: #fdae61; } ' +
            '[ ntiles_bin > 2 ] { line-color: #ffffbf; } ' +
            '[ ntiles_bin > 3 ] { line-color: #abdda4; } ' +
            '[ ntiles_bin > 4 ] { line-color: #2b83ba; } ' +
            '} ';
        return cartocss;
    },
    polyntiles_5: function (color) {
        // TODO: Use https://github.com/harthur/color ?
        var PALETTES = { GREEN: ['#deeddb', '#b5cdb1', '#8cad88', '#638d5e', '#3a6d35'],
                         BLUE: ['#dbe6ed', '#afcde0', '#83b4d3', '#579bc6', '#2b83ba'],
                         RED: ['#eddbdb', '#e7aaab', '#e27a7b', '#dc494b', '#d7191c'],
                         ORANGE: ['#ede3db', '#e7c4aa', '#e2a67a', '#dc8849', '#d76a19']
        };
        var NUM_BINS = 5;
        var cartocss = '#' + result_tablename + ' { ' +
            //'polygon-opacity: 0.33; ' +
            'line-color: #EFF3FF; ' +
            'line-width: 0; ';
        for (i = 0; i < NUM_BINS; i++) {
            cartocss += '[ ntiles_bin > ' + i + ' ] { polygon-fill: ' + PALETTES[color][i] + '; } ';
        }
        cartocss += '} ';
        return cartocss;
    },
    gtfs_shapes: function () {
        var cartocss =  '#' + result_tablename + ' { ' +
            'line-color: #000000;' +
            'line-opacity: 1;' +
            'line-width: 2;' +
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
            '[delta_type=1] {line-color: #f3f315; line-width: 5;} ' +
            '}';
        return cartocss;
    },
    gtfs_stops: function () {
       var cartocss =  '#' + result_tablename + ' {' +
            'marker-opacity: 1;' +
            'marker-line-color: #CCC;' +
            'marker-line-width: 0.5;' +
            'marker-fill: #000;' +
            'marker-width: 8;' +
            'marker-line-opacity: 1;' +
            'marker-placement: point;' +
            'marker-type: ellipse;' +
            'marker-allow-overlap: true;' +
            '}';
        return cartocss;
    },
    gtfs_stops_buffers: function () {
       var cartocss =  '#' + result_tablename + ' {' +
            'polygon-opacity: 0.7;' +
            'polygon-fill: #1f78b4;' +
            'line-color: #CCC;' +
            'line-width: 0.5;' +
            'line-opacity: 0.7;' +
            '}';
        return cartocss;
    },
    datasources_boundary: function () {
        var cartocss = '#' + result_tablename + ' {' +
            'line-color: #00f;' +
            'line-width:3;' +
            'line-dasharray:3,3;'+
            '}';
        return cartocss;
    }
};

/*
 * Escape for SQL
 */
function sqle(value, type) {
    if (!type) {
        type = 'text';
    }
    return "convert_from(decode('" + Buffer(value).toString('base64') +
            "','base64'), getdatabaseencoding())::"+type;
}

/**
 * Encapsulates windshaft display logic for the
 * gtfs_shapes table
 */
var GTFSShapes = function () {};

GTFSShapes.prototype.getSql = function (modes) {
    var modestr = '';
    if (modes) {
        modestr = 'and r.route_type in (';
        modestr += _.map(modes.split(','), function(m) { return sqle(m,'int'); }).join();
        modestr += ')';
    }
    var sqlString =
        "(SELECT * FROM ((SELECT distinct r.route_id, r.route_type, s.shape_id, 0 AS delta_type, s.the_geom as the_geom " +
        "FROM gtfs_shape_geoms AS s LEFT JOIN gtfs_trips t ON s.shape_id = t.shape_id " +
        "LEFT JOIN gtfs_routes r ON r.route_id = t.route_id WHERE r.route_id IS NOT NULL " +
        modestr + ") UNION " +
        "(SELECT null AS route_id, null AS route_type, null AS shape_id, delta_type, geom AS the_geom " +
        "FROM trip_deltas)) AS STUFF" +
        " ORDER BY CASE delta_type WHEN 1 THEN 1 WHEN -1 THEN 2 ELSE 3 END) AS " + result_tablename
    ;
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

GTFSStops.prototype.getSql = function (filetype, modes) {
    var modestr = '';
    if (modes) {
        modestr = 'AND r.route_type in (';
        modestr += _.map(modes.split(','), function(m) { return sqle(m,'int'); }).join();
        modestr += ')';
    }
    var table = 'gtfs_stops';
    var sqlString = "(SELECT distinct s.stop_id, s.the_geom as the_geom";
    if (filetype === 'utfgrid') {
        table = 'gtfs_stops_info';
        sqlString += ', stop_routes';
    }
    sqlString += " FROM " + table + " AS s " +
        "LEFT JOIN gtfs_stops_routes_join j ON j.stop_id = s.stop_id " +
        "LEFT JOIN gtfs_routes r on r.route_id = j.route_id " +
        "WHERE s.the_geom && !bbox! " +
        modestr + ") " +
        "AS " + result_tablename;
    return sqlString;
};

GTFSStops.prototype.getStyle = function () {
    return styles.gtfs_stops() || "";
};

/**
 * Encapsulates windshaft display logic for the datasources_boundary table
 */

var datasourcesBoundary = function () {};

datasourcesBoundary.prototype.getSql = function () {
    var sqlString =
        "(SELECT ST_Transform(geom, 4326) as the_geom FROM datasources_boundary) AS " + result_tablename;
    return sqlString;
};

datasourcesBoundary.prototype.getStyle = function () {
    return styles.datasources_boundary() || "";
};

/**
 * Display logic for datasources_demographicdatafeature table
 */
var DatasourcesDemographics = function(metric, num_ntiles, colorMap) {
    this.metricColors = { population_metric_1: 'RED',
                          population_metric_2: 'ORANGE',
                          destination_metric_1: 'BLUE'
    };
    _.extend(this.metricColors, colorMap);
    this.metricTypes = ['population_metric_1', 'population_metric_2', 'destination_metric_1'];
    if (this.metricTypes.indexOf(metric) < 0) {
        throw "Invalid metric type; acceptable values are \"population_metric_<1,2>\", \"destination_metric_1\"";
    }
    this.metric = metric;
    this.ntiles = parseInt(num_ntiles, 10);
};

DatasourcesDemographics.prototype.getSql = function() {
    var sqlString =
        "(SELECT ST_Transform(geom, 4326) as the_geom, " +
        "ntile(" + this.ntiles + ") over (order by " + this.metric + ") as ntiles_bin " +
        "FROM datasources_demographicdatafeature) AS " + result_tablename;
    return sqlString;
};

DatasourcesDemographics.prototype.getStyle = function() {
    var cssKey = 'polyntiles_' + this.ntiles;
    return styles[cssKey](this.metricColors[this.metric]) || "";
};

/**
 * Encapsulates windshaft display logic for the gtfs_stops_buffers table
 */

var GTFSStopsBuffers = function (options) {
    this.calculation_job = options.calculation_job;
    this.type = 'coverage_ratio_stops_buffer';
    this.aggregation = 'system';
    this.sample_period = options.sample_period;
};

GTFSStopsBuffers.prototype.getSql = function () {
    var sqlString =
        "(SELECT id AS indicator_id, value, " +
        "the_geom " +
        "FROM transit_indicators_indicator " +
        "WHERE type='" + this.type + "' AND aggregation='" + this.aggregation + "' " +
        "AND calculation_job_id='" + this.calculation_job + "' AND sample_period_id=" +
        "(SELECT id from transit_indicators_sampleperiod WHERE type='" + this.sample_period + "')" +
        ") as " + result_tablename;
    return sqlString;
};

GTFSStopsBuffers.prototype.getStyle = function () {
    return styles.gtfs_stops_buffers() || "";
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
    this.calculation_job = options.calculation_job;
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
Indicator.prototype.getSql = function (modes) {
    var modestr = '';
    if (modes) {
        modestr = 'AND r.route_type in (';
        modestr += _.map(modes.split(','), function(m) { return sqle(m,'int'); }).join();
        modestr += ')';
    }
    var sqlString =
        "(SELECT id as indicator_id, value, " +
        "ntile(" + this.options.ntiles + ") over (order by value) as ntiles_bin, " +
        "the_geom " +
        "FROM transit_indicators_indicator " +
        "LEFT JOIN gtfs_routes r ON transit_indicators_indicator.route_id = r.route_id " +
        "WHERE type=" + sqle(this.type) + " AND aggregation=" + sqle(this.aggregation) + " " +
        "AND calculation_job_id=" + sqle(this.calculation_job,'int') + " AND sample_period_id=" +
        "(SELECT id from transit_indicators_sampleperiod WHERE type=" + sqle(this.sample_period) + ")" +
        modestr +
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
exports.GTFSStopsBuffers = GTFSStopsBuffers;
exports.datasourcesBoundary = datasourcesBoundary;
exports.DatasourcesDemographics = DatasourcesDemographics;
exports.table = result_tablename;
