// Runs Windshaft.
// Note, currently to run this server your table must have a column called
// the_geom_webmercator with SRID of 3857.

var Windshaft = require('windshaft');
var _         = require('underscore');
var settings  = require('./settings.json');
var oti       = require('./oti-indicator.js');

var config = {
    base_url: '/tiles/:dbname/:version/:type/:sample_period/:aggregation',
    base_url_notable: '/tiles/:dbname/:version/:type/:sample_period/:aggregation',
    grainstore: {
                 datasource: {
                    user: settings.db_user,
                    password: settings.db_pass,
                    host: settings.db_host,
                    port: settings.db_port,
                    geometry_field: 'the_geom',
                    srid: 4326
                 }
    }, //see grainstore npm for other options
    redis: {host: settings.redis_host, port: settings.redis_port},
    enable_cors: true,
    req2params: function(req, callback){

        // Custom actions for the blank routes/stops tables
        // version, sample_period, aggregation can all be 0 for these requests
        if (req.params.type === 'gtfs_shapes') {
            var gtfsShapes = new oti.GTFSShapes();
            req.params.sql = gtfsShapes.getSql();
            req.params.style = gtfsShapes.getStyle();
        } else if (req.params.type === 'gtfs_stops') {
            var gtfsStops = new oti.GTFSStops();
            var filetype = req.query.interactivity ? 'utfgrid' : 'png';
            req.params.sql = gtfsStops.getSql(filetype);
            req.params.style = gtfsStops.getStyle();
        } else if (req.params.type === 'coverage_ratio_stops_buffer') {
            var gtfsStopsBuffers = new oti.GTFSStopsBuffers();
            req.params.sql = gtfsStopsBuffers.getSql();
            req.params.style = gtfsStopsBuffers.getStyle();
        } else if (req.params.type === 'datasources_boundary') {
            var datasourcesBoundary = new oti.datasourcesBoundary();
            req.params.sql = datasourcesBoundary.getSql();
            req.params.style = datasourcesBoundary.getStyle();
        } else {
            // Default to displaying indicators from the url params
            var indicatorOptions = _.extend({}, req.params, { ntiles: req.query.ntiles || 5 });

            var indicator = new oti.Indicator(indicatorOptions);

            // Set style/sql based on indicator config
            req.params.style = indicator.getStyle();
            req.params.sql = indicator.getSql();
        }

        // Interactivity column comes through in our get params since it's optional
        // Forward along to params
        req.params.interactivity = req.query.interactivity;

        // Must set this to equal the tablename used in the custom sql, otherwise, blank tiles
        req.params.table = oti.table;

        // send the finished req object on
        callback(null,req);
    }
};

// Initialize tile server on port 4000
var ws = new Windshaft.Server(config);
ws.listen(4000);

console.log('map tiles are now being served out of: http://127.0.0.1:4000' + config.base_url + '/:z/:x/:y');
