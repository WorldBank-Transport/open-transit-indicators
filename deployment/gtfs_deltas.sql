--
-- Name: trip_deltas; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE public.trip_deltas (
  id text PRIMARY KEY,
  geom geometry(LineString,4326) NOT NULL,
  delta_type integer NOT NULL
);

ALTER TABLE public.trip_deltas OWNER TO transit_indicators;


--
-- Name: stop_deltas; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE public.stop_deltas (
  id text PRIMARY KEY,
  name text NOT NULL,
  description text,
  geom geometry(Point,4326) NOT NULL,
  delta_type integer NOT NULL
);

ALTER TABLE public.stop_deltas OWNER TO transit_indicators;

