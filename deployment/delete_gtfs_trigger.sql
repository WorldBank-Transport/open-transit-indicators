-- Trigger function to delete GTFS data when Django model object with feed metadata is deleted
DROP TRIGGER IF EXISTS empty_gtfs on datasources_gtfsfeed;

CREATE OR REPLACE FUNCTION empty_gtfs() RETURNS trigger AS $empty_gtfs$
    BEGIN
        -- Note:  cannot use TRUNCATE in a trigger, as there are active database connections
        -- (from Windshaft and Django)
        EXECUTE 'DELETE FROM gtfs_agency';
        EXECUTE 'DELETE FROM gtfs_calendar';
        EXECUTE 'DELETE FROM gtfs_calendar_dates';
        EXECUTE 'DELETE FROM gtfs_directions';
        EXECUTE 'DELETE FROM gtfs_fare_attributes';
        EXECUTE 'DELETE FROM gtfs_fare_rules';
        EXECUTE 'DELETE FROM gtfs_feed_info';
        EXECUTE 'DELETE FROM gtfs_frequencies';
        EXECUTE 'DELETE FROM gtfs_location_types';
        EXECUTE 'DELETE FROM gtfs_payment_methods';
        EXECUTE 'DELETE FROM gtfs_pickup_dropoff_types';
        EXECUTE 'DELETE FROM gtfs_routes';
        EXECUTE 'DELETE FROM gtfs_shape_geoms';
        EXECUTE 'DELETE FROM gtfs_shapes';
        EXECUTE 'DELETE FROM gtfs_stop_times';
        EXECUTE 'DELETE FROM gtfs_stops';
        EXECUTE 'DELETE FROM gtfs_stops_info';
        EXECUTE 'DELETE FROM gtfs_transfer_types';
        EXECUTE 'DELETE FROM gtfs_transfers';
        EXECUTE 'DELETE FROM gtfs_trips';
        EXECUTE 'DELETE FROM gtfs_wheelchair_accessibility';
        EXECUTE 'DELETE FROM gtfs_wheelchair_boardings';
        EXECUTE 'ALTER SEQUENCE datasources_gtfsfeed_id_seq RESTART WITH 1';
        EXECUTE 'ALTER SEQUENCE datasources_gtfsfeedproblem_id_seq RESTART WITH 1';
        RETURN NULL;
    END;
$empty_gtfs$ LANGUAGE plpgsql;

CREATE TRIGGER empty_gtfs AFTER DELETE ON datasources_gtfsfeed
    FOR EACH STATEMENT EXECUTE PROCEDURE empty_gtfs();
