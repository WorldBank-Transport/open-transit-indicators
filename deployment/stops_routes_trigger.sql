-- Trigger function to set stops interactivity dialog with routes served
DROP TRIGGER IF EXISTS stops_routes on gtfs_stops;

CREATE OR REPLACE FUNCTION stops_routes() RETURNS trigger AS $stops_routes$
    BEGIN
        -- Repopulate gtfs_stops_routes_join table
        
        EXECUTE 'INSERT INTO gtfs_stops_routes_join (
            SELECT DISTINCT st.stop_id, r.route_id
            FROM gtfs_stop_times st
            LEFT JOIN gtfs_trips t ON st.trip_id = t.trip_id
            LEFT JOIN gtfs_routes r on r.route_id = t.route_id
            LEFT JOIN gtfs_stops s on s.stop_id = st.stop_id
            WHERE s.stop_id = $1
        )' USING NEW.stop_id;

        -- Populate routes_desc column on stops table for UTFGrid interactivity;
        -- show stop description and the routes it serves.
        EXECUTE 'INSERT INTO gtfs_stops_info (SELECT s.stop_id, s.the_geom,
            CONCAT(''<strong>'', s.stop_name, ''</strong><br />'',
                array_to_string(array(
                    SELECT r.route_short_name
                    FROM gtfs_routes AS r INNER JOIN gtfs_stops_routes_join AS srj
                    ON (srj.route_id = r.route_id)
                    WHERE srj.stop_id = $1
                ), ''<br />'')
            )
            FROM gtfs_stops s
            WHERE s.stop_id=$1)'
        USING NEW.stop_id;

        RETURN NEW;
    END;
$stops_routes$ LANGUAGE plpgsql;

CREATE TRIGGER stops_routes AFTER INSERT ON gtfs_stops
    FOR EACH ROW EXECUTE PROCEDURE stops_routes();

