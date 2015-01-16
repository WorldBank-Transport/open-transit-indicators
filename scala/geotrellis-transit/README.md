GeoTrellis Transit
===========

GeoTrellis Transit is a data loader and set of web services run in an embedded Jetty server that 
answer questions about travel times and transit sheds. The project also includes a demo client application
that hits these endpoints, and running example of which can be found at [transit.geotrellis.com](http://transit.geotrellis.com).

This project was done in collaboration with TechImpact and with support from the William Penn Foundation.

To generate travelsheds from OSM &amp; GTFS data:

The 'buildgraph' command, along with the appropriate json config file, will create serialized graph files that represent a geotrellis.network.graph.TransitGraph object and related information:

```bash
./sbt "run buildgraph geotrellis-transit.json"
```

where the configuration json looks like this:

```javascript
{
    "loader": {
        "gtfs": [
            {
                "name": "bus",
                "path": "/var/data/philly/gtfs/google_bus" 
            },
            {
                "name": "train",
                "path": "/var/data/philly/gtfs/google_rail"
            }
        ],
        "osm": [
            {
                "name": "Philadelphia",
                "path": "/var/data/philly/osm/philadelphia.osm"
            }
        ]
    },
    "graph": {
        "data": "/var/data/transit/graph/"
    }
}
```

The data can then be used with the 'server' command to start up a GeoTrellis server that exposes transit information API endpoints. 

```bash
./sbt "run server geotrellis-transit.json"
```

After running the server, you can go to http://localhost:9999/ for an example client application.
