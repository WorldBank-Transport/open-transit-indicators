-- Not necessarily intended to be run at gtfs import, but
-- useful for populating tables until we add these commands to the gtfs-parser

--
-- Populate gtfs_stops_routes_join table
--
INSERT INTO gtfs_stops_routes_join (
SELECT DISTINCT st.stop_id, r.route_id
FROM gtfs_stop_times st
LEFT JOIN gtfs_trips t ON st.trip_id = t.trip_id
LEFT JOIN gtfs_routes r on r.route_id = t.route_id
LEFT JOIN gtfs_stops s on s.stop_id = st.stop_id
);

--
-- Select all stops for each route 
--
select r.route_short_name, r.route_type, s.stop_lat, s.stop_lon 
from gtfs_routes as r, gtfs_stops as s, gtfs_stops_routes_join as srj 
where srj.route_id = r.route_id and srj.stop_id = s.stop_id 
order by srj.route_id;

