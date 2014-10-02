-- Function to generate a regular grid (fishnet) of points (default) or polygons.
-- Taken from here:
-- http://www.spatialdbadvisor.com/postgis_tips_tricks/300/generating-a-grid-fishnet-of-points-or-polygons-for-postgis

-- Create required type
DROP   TYPE IF EXISTS T_Grid CASCADE;
CREATE TYPE T_Grid AS (
  gcol  int4,
  grow  int4,
  geom geometry
);
-- Drop function if exists
DROP FUNCTION IF EXISTS ST_RegularGrid(geometry, NUMERIC, NUMERIC, BOOLEAN);
-- Now create the function
CREATE OR REPLACE FUNCTION ST_RegularGrid(p_geometry   geometry,
                                          p_TileSizeX  NUMERIC,
                                          p_TileSizeY  NUMERIC,
                                          p_point      BOOLEAN DEFAULT TRUE)
  RETURNS SETOF T_Grid AS
$BODY$
DECLARE
   v_mbr   geometry;
   v_srid  int4;
   v_halfX NUMERIC := p_TileSizeX / 2.0;
   v_halfY NUMERIC := p_TileSizeY / 2.0;
   v_loCol int4;
   v_hiCol int4;
   v_loRow int4;
   v_hiRow int4;
   v_grid  T_Grid;
BEGIN
   IF ( p_geometry IS NULL ) THEN
      RETURN;
   END IF;
   v_srid  := ST_SRID(p_geometry);
   v_mbr   := ST_Envelope(p_geometry);
   v_loCol := trunc((ST_XMIN(v_mbr) / p_TileSizeX)::NUMERIC );
   v_hiCol := CEIL( (ST_XMAX(v_mbr) / p_TileSizeX)::NUMERIC ) - 1;
   v_loRow := trunc((ST_YMIN(v_mbr) / p_TileSizeY)::NUMERIC );
   v_hiRow := CEIL( (ST_YMAX(v_mbr) / p_TileSizeY)::NUMERIC ) - 1;
   FOR v_col IN v_loCol..v_hiCol Loop
     FOR v_row IN v_loRow..v_hiRow Loop
         v_grid.gcol := v_col;
         v_grid.grow := v_row;
         IF ( p_point ) THEN
           v_grid.geom := ST_SetSRID(
                             ST_MakePoint((v_col * p_TileSizeX) + v_halfX,
                                          (v_row * p_TileSizeY) + V_HalfY),
                             v_srid);
         ELSE
           v_grid.geom := ST_SetSRID(
                             ST_MakeEnvelope((v_col * p_TileSizeX),
                                             (v_row * p_TileSizeY),
                                             (v_col * p_TileSizeX) + p_TileSizeX,
                                             (v_row * p_TileSizeY) + p_TileSizeY),
                             v_srid);
         END IF;
         RETURN NEXT v_grid;
     END Loop;
   END Loop;
END;
$BODY$
  LANGUAGE plpgsql IMMUTABLE
  COST 100
  ROWS 1000;

-- Assign ownership
ALTER FUNCTION st_regulargrid(geometry, NUMERIC, NUMERIC, BOOLEAN)
  OWNER TO transit_indicators;
