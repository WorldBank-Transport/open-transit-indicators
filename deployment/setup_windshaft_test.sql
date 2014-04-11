-- Provisioning script uses this to set up a test table for Windshaft to read.
-- To view a map of this data, open exampleosm.html on the vagrant host.

DROP TABLE IF EXISTS test_windshaft;

CREATE TABLE test_windshaft (
    updated_at timestamp without time zone DEFAULT now(),
    created_at timestamp without time zone DEFAULT now(),
    cartodb_id serial NOT NULL PRIMARY KEY,
    name character varying,
    address character varying,
    the_geom geometry,
    the_geom_webmercator geometry,
    CONSTRAINT enforce_dims_the_geom CHECK ((st_ndims(the_geom) = 2)),
    CONSTRAINT enforce_dims_the_geom_webmercator CHECK ((st_ndims(the_geom_webmercator) = 2)),
    CONSTRAINT enforce_geotype_the_geom CHECK (((geometrytype(the_geom) = 'POINT'::text) OR (the_geom IS NULL))),
    CONSTRAINT enforce_geotype_the_geom_webmercator CHECK (((geometrytype(the_geom_webmercator) = 'POINT'::text) OR (the_geom_webmercator IS NULL))),
    CONSTRAINT enforce_srid_the_geom CHECK ((st_srid(the_geom) = 4326)),
    CONSTRAINT enforce_srid_the_geom_webmercator CHECK ((st_srid(the_geom_webmercator) = 3857))
);

INSERT INTO test_windshaft (updated_at, created_at, name, address, the_geom)
VALUES
 ('2011-09-21 14:02:21.358706', '2011-09-21 14:02:21.314252', 'Hawai', 'Calle de Pérez Galdós 9, Madrid, Spain', '0101000020E6100000A6B73F170D990DC064E8D84125364440'),
 ('2011-09-21 14:02:21.358706', '2011-09-21 14:02:21.319101', 'El Estocolmo', 'Calle de la Palma 72, Madrid, Spain', '0101000020E6100000C90567F0F7AB0DC0AB07CC43A6364440'),
 ('2011-09-21 14:02:21.358706', '2011-09-21 14:02:21.324', 'El Rey del Tallarín', 'Plaza Conde de Toreno 2, Madrid, Spain', '0101000020E610000021C8410933AD0DC0CB0EF10F5B364440'),
 ('2011-09-21 14:02:21.358706', '2011-09-21 14:02:21.329509', 'El Lacón', 'Manuel Fernández y González 8, Madrid, Spain', '0101000020E6100000BC5983F755990DC07D923B6C22354440'),
 ('2011-09-21 14:02:21.358706', '2011-09-21 14:02:21.334931', 'El Pico', 'Calle Divino Pastor 12, Madrid, Spain', '0101000020E61000003B6D8D08C6A10DC0371B2B31CF364440');
UPDATE test_windshaft SET the_geom_webmercator = ST_Transform(the_geom, 3857);

CREATE INDEX test_windshaft_the_geom_idx ON test_windshaft USING gist (the_geom);
CREATE INDEX test_windshaft_the_geom_webmercator_idx ON test_windshaft USING gist (the_geom_webmercator);

ALTER TABLE test_windshaft OWNER TO transit_indicators;
