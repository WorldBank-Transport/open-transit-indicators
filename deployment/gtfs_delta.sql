<<<<<<< Updated upstream
--
-- Name: gtfs_delta; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE public.gtfs_delta (
  id text PRIMARY KEY,
  deltaType integer NOT NULL,
  geom geometry(LineString,4326) NOT NULL,
);

ALTER TABLE public.station_csv OWNER TO transit_indicators;

||||||| merged common ancestors
=======
--
-- Name: gtfs_delta; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE public.gtfs_delta (
  id text PRIMARY KEY,
  deltaType integer NOT NULL,
  geom geometry(LineString,4326) NOT NULL,
);

ALTER TABLE public.gtfs_delta OWNER TO transit_indicators;

>>>>>>> Stashed changes
