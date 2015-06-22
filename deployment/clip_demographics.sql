-- Function to clip demographics features to region boundaries.
-- Proportionally allocates values to remaining area of clipped feature.

DROP FUNCTION IF EXISTS ClipDemographics();

CREATE OR REPLACE FUNCTION ClipDemographics()
  RETURNS void AS
$$
DECLARE
  region geometry;
  diff geometry;
  intersection geometry;
  min_feature_id int4;
  max_feature_id int4;
  feature_area NUMERIC;  -- in SRID units
  intersection_area NUMERIC;
  contained_proportion NUMERIC;
BEGIN
  RAISE INFO 'Going to clip demographics features to region boundary.';
  region := geom FROM datasources_boundary WHERE id = (SELECT region_boundary_id FROM transit_indicators_otiindicatorsconfig);
  min_feature_id := min(id) FROM datasources_demographicdatafeature;
  max_feature_id := max(id) FROM datasources_demographicdatafeature;

  FOR feat_id IN min_feature_id..max_feature_id Loop

    -- find the portion of the feature, if any, not contained in the region
    diff := ST_Difference((SELECT geom FROM datasources_demographicdatafeature WHERE id = feat_id), region);

    IF (ST_IsEmpty(diff) IS FALSE) THEN
      intersection := ST_Intersection((SELECT geom FROM datasources_demographicdatafeature WHERE id = feat_id), region);

      IF (ST_IsEmpty(intersection) IS TRUE) THEN
        RAISE INFO 'Deleting demographics feature % completely outside region.', feat_id;
        DELETE FROM datasources_demographicdatafeature WHERE id=feat_id;
      ELSE
        -- set feature to clipped geom with proportionally allocated metric values by area
        intersection_area := ST_Area(intersection);
        feature_area := ST_Area(geom) FROM datasources_demographicdatafeature WHERE id = feat_id;
        contained_proportion := intersection_area / feature_area;
        RAISE INFO '% percent of area of demographics feature % is within region; clipping.', contained_proportion, feat_id;
        UPDATE datasources_demographicdatafeature
          SET geom = ST_Multi(intersection),
            population_metric_1 = population_metric_1 * contained_proportion,
            population_metric_2 = population_metric_2 * contained_proportion,
            destination_metric_1 = destination_metric_1 * contained_proportion
          WHERE id = feat_id;
      END IF;
    END IF;

  END Loop;

END;
$$ LANGUAGE plpgsql;

ALTER FUNCTION ClipDemographics() OWNER TO transit_indicators;
