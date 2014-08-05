-- Create tables to hold reprojected boundary / demographics data
CREATE TABLE IF NOT EXISTS utm_datasources_demographicdatafeature (
        datafeature_id INT PRIMARY KEY REFERENCES datasources_demographicdatafeature ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS utm_datasources_boundary (
        boundary_id INT PRIMARY KEY REFERENCES datasources_boundary ON DELETE CASCADE
);

-- Add Geometry columns
-- The SRID is just a default; it's going to get changed upon GTFS import.
SELECT AddGeometryColumn('utm_datasources_demographicdatafeature', 'geom', 4326, 'MULTIPOLYGON', 2);
SELECT AddGeometryColumn('utm_datasources_boundary', 'geom', 4326, 'MULTIPOLYGON', 2);

-- Trigger function to automatically reproject demographics as they're inserted
DROP TRIGGER IF EXISTS insert_demographic_utm on datasources_demographicdatafeature;

CREATE OR REPLACE FUNCTION insert_demographic_utm() RETURNS trigger AS $insert_demographic_utm$
    BEGIN
        -- Populate utm_datasources_demographicdatafeature table as rows added to datasources_demographicdatafeature 
        EXECUTE 'INSERT INTO utm_datasources_demographicdatafeature (
            SELECT df.id,
                ST_Transform(df.geom, (SELECT Find_SRID(''public'', ''gtfs_stops'', ''geom'')))
            FROM datasources_demographicdatafeature df
            WHERE df.id = $1
        )' USING NEW.id;

        RETURN NEW;
    END;
$insert_demographic_utm$ LANGUAGE plpgsql;

CREATE TRIGGER insert_demographic_utm AFTER INSERT ON datasources_demographicdatafeature
    FOR EACH ROW EXECUTE PROCEDURE insert_demographic_utm();

-- Boundaries are a bit more complicated because they are inserted without geometries
-- and then updated.
-- Trigger function to automatically create boundary reprojection row
DROP TRIGGER IF EXISTS insert_boundary_utm on datasources_boundary;
DROP TRIGGER IF EXISTS update_boundary_utm on datasources_boundary;

CREATE OR REPLACE FUNCTION insert_boundary_utm() RETURNS trigger AS $insert_boundary_utm$
    BEGIN
        -- Populate utm_datasources_boundary table as rows added to datasources_boundary
        -- It is unlikely that the geom column will have data on insert, but transform it
        -- anyway just in case.
        EXECUTE 'INSERT INTO utm_datasources_boundary (
            SELECT bound.id,
                ST_Transform(bound.geom, (SELECT Find_SRID(''public'', ''gtfs_stops'', ''geom'')))
            FROM datasources_boundary bound
            WHERE bound.id = $1
        )' USING NEW.id;

        RETURN NEW;
    END;
$insert_boundary_utm$ LANGUAGE plpgsql;

-- Boundaries are inserted, then updated to add geometry data, so the real transform 
-- work is done here.
CREATE OR REPLACE FUNCTION update_boundary_utm() RETURNS trigger as $insert_boundary_utm$
    BEGIN
        EXECUTE 'UPDATE utm_datasources_boundary
            SET boundary_id = bound.id,
                geom = (SELECT ST_Transform(bound.geom,
                        (SELECT Find_SRID(''public'', ''gtfs_stops'', ''geom''))))
            FROM datasources_boundary bound
            WHERE bound.id = $1
	    AND boundary_id = $1'
        USING NEW.id;

        RETURN NEW;
    END;
$insert_boundary_utm$ LANGUAGE plpgsql;

CREATE TRIGGER insert_boundary_utm AFTER INSERT ON datasources_boundary
    FOR EACH ROW EXECUTE PROCEDURE insert_boundary_utm();

CREATE TRIGGER update_boundary_utm AFTER UPDATE ON datasources_boundary
    FOR EACH ROW EXECUTE PROCEDURE update_boundary_utm();
