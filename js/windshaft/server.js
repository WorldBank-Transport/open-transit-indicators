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

        // Set indicator options
        var indicatorOptions = _.extend({}, req.params, { ntiles: req.query.ntiles || 5 });

        var indicator = new oti.Indicator(indicatorOptions);

        // Must set this to equal the tablename used in the custom sql, otherwise, blank tiles
        req.params.table = oti.table;
        // Set style/sql based on indicator config
        req.params.style = indicator.getStyle();
        req.params.sql = indicator.getSql();

        // Interactivity column comes through in our get params since it's optional
        // Forward along to params
        req.params.interactivity = req.query.interactivity;

        // send the finished req object on
        callback(null,req);
    }
};

// Initialize tile server on port 4000
var ws = new Windshaft.Server(config);
ws.listen(4000);

console.log('map tiles are now being served out of: http://127.0.0.1:4000' + config.base_url + '/:z/:x/:y');
