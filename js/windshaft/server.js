// Runs Windshaft.
// Note, currently to run this server your table must have a column called
// the_geom_webmercator with SRID of 3857.

var Windshaft = require('windshaft');
var _         = require('underscore');
var settings  = require('./settings.json');
var otistyling = require('./oti-styling');

var config = {
    base_url: '/tiles/:dbname/table/:table',
    base_url_notable: '/tiles/:dbname',
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
        var table = req.params.table;
        req.params.style = otistyling.get(table);

        // Example of how to tailor request for different tables
        // TODO: Abstract styles to separate module
        if (table === 'gtfs_stops') {
            req.params.interactivity = 'stop_desc'; //['stop_id', 'stop_name', 'stop_desc'];
        }

        if (table === 'gtfs_shape_geoms') {
            req.params.sql = "(select distinct r.route_id, r.route_type, s.shape_id, s.the_geom as the_geom from gtfs_shape_geoms as s LEFT JOIN gtfs_trips t ON s.shape_id = t.shape_id LEFT JOIN gtfs_routes r ON r.route_id = t.route_id WHERE r.route_id IS NOT NULL) AS gsg";
        }

        // this is in case you want to test sql parameters eg ...png?sql=select * from my_table limit 10
        req.params =  _.extend({}, req.params);
        _.extend(req.params, req.query);

        // send the finished req object on
        callback(null,req);
    }
};

// Initialize tile server on port 4000
var ws = new Windshaft.Server(config);
ws.listen(4000);

console.log('map tiles are now being served out of: http://127.0.0.1:4000' + config.base_url + '/:z/:x/:y');
