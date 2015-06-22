-- Function to create regular grid of points with proportionately allocated demographic values.

DROP FUNCTION IF EXISTS CreateGrid();

CREATE OR REPLACE FUNCTION CreateGrid()
  RETURNS void AS
$$
BEGIN
    DROP TABLE IF EXISTS demographic_grid;

    -- get grid points with SRID (ST_Extent returns object without geometry)
    CREATE TABLE demographic_grid AS
    SELECT (pts.grid).geom AS geom
    FROM (SELECT ST_RegularGrid(geom, 500, 500) AS grid
    FROM (SELECT ST_SetSRID(ST_Extent(geom),
    find_srid('public', 'datasources_demographicdatafeature', 'geom')) AS geom
    FROM datasources_demographicdatafeature) AS box) AS pts;

    ALTER TABLE demographic_grid ADD COLUMN feature_id integer;
    ALTER TABLE demographic_grid ADD COLUMN population_metric_1 double precision;
    ALTER TABLE demographic_grid ADD COLUMN population_metric_2 double precision;
    ALTER TABLE demographic_grid ADD COLUMN destination_metric_1 double precision;
    CREATE INDEX demographic_grid_feature_id ON demographic_grid(feature_id);

    -- create FK, so the demographic_grid values will be deleted on related features deletion
    ALTER TABLE demographic_grid ADD CONSTRAINT demographic_grid_feature_id_fk
                FOREIGN KEY (feature_id)
                REFERENCES datasources_demographicdatafeature(id)
                ON DELETE CASCADE;

    UPDATE demographic_grid g SET feature_id = (SELECT id FROM datasources_demographicdatafeature f
    WHERE ST_Intersects(f.geom, g.geom));

    DELETE FROM demographic_grid WHERE feature_id IS NULL;

    -- create extra table for more efficient update
    CREATE TEMP TABLE demographic_point_values AS
    SELECT f.id, f.population_metric_1 / COUNT(g.feature_id) as population_metric_1,
    f.population_metric_2 / COUNT(g.feature_id) as population_metric_2,
    f.destination_metric_1 / COUNT(g.feature_id) as destination_metric_1
    FROM datasources_demographicdatafeature f
    INNER JOIN demographic_grid g ON (g.feature_id=f.id)
    GROUP BY f.id;

    UPDATE demographic_grid SET population_metric_1 = v.population_metric_1,
    population_metric_2 = v.population_metric_2,
    destination_metric_1 = v.destination_metric_1
    FROM demographic_point_values v
    WHERE v.id = feature_id;

    -- Reproject to UTM
    UPDATE demographic_grid SET geom = ST_Transform(geom, (SELECT Find_SRID('public', 'gtfs_stops', 'geom')));
    PERFORM UpdateGeometrySRID('public', 'demographic_grid', 'geom', (SELECT Find_SRID('public', 'gtfs_stops', 'geom')));

    -- Reproject original polygons to UTM
--    ALTER TABLE datasources_demographicdatafeature DROP COLUMN utm_geom;

    ALTER TABLE datasources_demographicdatafeature DROP COLUMN IF EXISTS utm_geom;
    ALTER TABLE datasources_demographicdatafeature ADD COLUMN utm_geom geometry(MultiPolygon, 4326);
    PERFORM UpdateGeometrySRID('datasources_demographicdatafeature','utm_geom', (SELECT Find_SRID('public', 'gtfs_stops', 'geom')));
    UPDATE datasources_demographicdatafeature SET utm_geom = ST_Transform(geom, (SELECT Find_SRID('public', 'gtfs_stops', 'geom')));

END;
$$ LANGUAGE plpgsql;

ALTER FUNCTION CreateGrid() OWNER TO transit_indicators;
