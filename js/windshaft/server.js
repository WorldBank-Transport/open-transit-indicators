// Runs Windshaft.
// Note, currently to run this server your table must have a column called 
// the_geom_webmercator with SRID of 3857.

var Windshaft = require('windshaft');
var _         = require('underscore');
var settings  = require('./settings.json');

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

        // specify the database column you'd like to interact with
        // TODO: using test table column here
        //req.params.interactivity = ['cartodb_id'];

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
