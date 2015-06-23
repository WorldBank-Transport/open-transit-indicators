--
-- Name: trip_delta; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE public.trip_delta (
  id text PRIMARY KEY,
  geom geometry(LineString,4326) NOT NULL,
  deltaType integer NOT NULL,
);

ALTER TABLE public.trip_delta OWNER TO transit_indicators;


--
-- Name: stop_delta; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE public.stop_delta (
  id text PRIMARY KEY,
  name text NOT NULL,
  description text,
  geom geometry(Point,4326) NOT NULL,
  deltaType integer NOT NULL,
);

ALTER TABLE public.stop_delta OWNER TO transit_indicators;

