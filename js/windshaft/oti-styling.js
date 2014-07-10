/*
 * Small utility object used to return CartoCSS styles for
 * the various OTI table names
 *
 * NOTE: This is not currently complete or well thought out
 *       It is here simply to serve as an example of how to
 *       make a module and break functionality out of the 
 *       node server.
 */
var styles = {
    gtfs_stops: function () {
        var gtfs_stops_style =  '#gtfs_stops {';
            gtfs_stops_style += 'marker-opacity: 1;';
            gtfs_stops_style += 'marker-line-color: #FFF;';
            gtfs_stops_style += 'marker-line-width: 2.5;';
            gtfs_stops_style += 'marker-fill: #B40903;';
            gtfs_stops_style += 'marker-width: 12;';
            gtfs_stops_style += 'marker-line-opacity: 1;';
            gtfs_stops_style += 'marker-placement: point;';
            gtfs_stops_style += 'marker-type: ellipse;';
            gtfs_stops_style += 'marker-allow-overlap: true;';
            gtfs_stops_style += '} ';
        return gtfs_stops_style;
    },
    gtfs_stops_info: function () {
        var gtfs_stops_info_style =  '#gtfs_stops_info {';
            gtfs_stops_info_style += 'marker-opacity: 1;';
            gtfs_stops_info_style += 'marker-line-color: #FFF;';
            gtfs_stops_info_style += 'marker-line-width: 2.5;';
            gtfs_stops_info_style += 'marker-fill: #B40903;';
            gtfs_stops_info_style += 'marker-width: 12;';
            gtfs_stops_info_style += 'marker-line-opacity: 1;';
            gtfs_stops_info_style += 'marker-placement: point;';
            gtfs_stops_info_style += 'marker-type: ellipse;';
            gtfs_stops_info_style += 'marker-allow-overlap: true;';
            gtfs_stops_info_style += '} ';
        return gtfs_stops_info_style;
    },
    gtfs_shape_geoms: function () {
        var gtfs_shapes_style =  '#gtfs_shape_geoms {';
            gtfs_shapes_style += 'line-color: #000000;';
            gtfs_shapes_style += 'line-opacity: 1;';
            gtfs_shapes_style += 'line-width: 4;';
            // Not a complete list, not sure what the best way to provide a complete
            // color palette for each route type would be. There are currently 100+
            // route types defined in the gtfs_route_types table.
            gtfs_shapes_style += '[route_type=0] {line-color: #a6cee3; } ';
            gtfs_shapes_style += '[route_type=1] {line-color: #1f78b4; } ';
            gtfs_shapes_style += '[route_type=2] {line-color: #b2df8a; } ';
            gtfs_shapes_style += '[route_type=3] {line-color: #33a02c; } ';
            gtfs_shapes_style += '[route_type=4] {line-color: #fb9a99; } ';
            gtfs_shapes_style += '[route_type=5] {line-color: #e31a1c; } ';
            gtfs_shapes_style += '[route_type=6] {line-color: #fdbf6f; } ';
            gtfs_shapes_style += '[route_type=7] {line-color: #ff7f00; } ';
            gtfs_shapes_style += '[route_type=8] {line-color: #cab2d6; } ';
            gtfs_shapes_style += '}';
        return gtfs_shapes_style;
    }
};

exports.get = function (tablename) {
    return styles[tablename] ? styles[tablename]() : {};
};
