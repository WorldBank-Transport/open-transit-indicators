-- Function to clip demographics features to region boundaries.
-- Proportionally allocates values to remaining area of clipped feature.

DROP FUNCTION IF EXISTS ClipDemographics();

CREATE OR REPLACE FUNCTION ClipDemographics()
  RETURNS void AS
$$
DECLARE
  region geometry;
  diff geometry;
  min_feature_id int4;
  max_feature_id int4;
BEGIN
  RAISE INFO 'Going to clip demographics to boundary';
  region := geom FROM datasources_boundary WHERE id = (SELECT region_boundary_id FROM transit_indicators_otiindicatorsconfig);
  min_feature_id := min(id) FROM datasources_demographicdatafeature;
  max_feature_id := max(id) FROM datasources_demographicdatafeature;

  FOR feat_id IN min_feature_id..max_feature_id Loop
    diff := ST_Difference((SELECT geom from datasources_demographicdatafeature where id=feat_id), region);
    IF (ST_isEmpty(diff) IS FALSE) THEN
      RAISE WARNING 'Found demographics feature outside region boundary';
    END IF;
  END Loop;

END;
$$ LANGUAGE plpgsql;

ALTER FUNCTION ClipDemographics() OWNER TO transit_indicators;
