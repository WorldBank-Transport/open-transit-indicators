-- There is no way to clear out the GTFS data yet,
-- so this is here to make it easier to test.
-- Within the VM, run:
-- psql -h localhost -U transit_indicators -f truncate_gtfs.sql
TRUNCATE TABLE gtfs_agency;
TRUNCATE TABLE gtfs_calendar;
TRUNCATE TABLE gtfs_calendar_dates;
TRUNCATE TABLE gtfs_directions;
TRUNCATE TABLE gtfs_fare_attributes;
TRUNCATE TABLE gtfs_fare_rules;
TRUNCATE TABLE gtfs_feed_info;
TRUNCATE TABLE gtfs_frequencies;
TRUNCATE TABLE gtfs_location_types;
TRUNCATE TABLE gtfs_payment_methods;
TRUNCATE TABLE gtfs_pickup_dropoff_types;
TRUNCATE TABLE gtfs_routes;
TRUNCATE TABLE gtfs_stops_routes_join;
TRUNCATE TABLE gtfs_shape_geoms;
TRUNCATE TABLE gtfs_shapes;
TRUNCATE TABLE gtfs_stop_times;
TRUNCATE TABLE gtfs_stops;
TRUNCATE TABLE gtfs_stops_info;
TRUNCATE TABLE gtfs_transfer_types;
TRUNCATE TABLE gtfs_transfers;
TRUNCATE TABLE gtfs_trips;
TRUNCATE TABLE gtfs_wheelchair_accessibility;
TRUNCATE TABLE gtfs_wheelchair_boardings;
TRUNCATE TABLE datasources_gtfsfeed RESTART IDENTITY CASCADE;

-- keep this one around: it's manually populated
--TRUNCATE TABLE gtfs_route_types;
