--
-- Name: station_csv; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE public.station_csv (
  id integer NOT NULL PRIMARY KEY,
  status text NOT NULL,
  buffer_distance real NOT NULL,
  commute_time integer NOT NULL,
  data bytea
);

ALTER TABLE public.station_csv OWNER TO transit_indicators;

