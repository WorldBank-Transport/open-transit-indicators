--
-- Name: gtfs_agency; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_agency (
    agency_id text NOT NULL,
    agency_name text NOT NULL,
    agency_url text NOT NULL,
    agency_timezone text NOT NULL,
    agency_lang text,
    agency_phone text,
    agency_fare_url text
);


ALTER TABLE public.gtfs_agency OWNER TO transit_indicators;

--
-- Name: gtfs_calendar; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_calendar (
    service_id text NOT NULL,
    monday integer NOT NULL,
    tuesday integer NOT NULL,
    wednesday integer NOT NULL,
    thursday integer NOT NULL,
    friday integer NOT NULL,
    saturday integer NOT NULL,
    sunday integer NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL
);


ALTER TABLE public.gtfs_calendar OWNER TO transit_indicators;

--
-- Name: gtfs_calendar_dates; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_calendar_dates (
    service_id text,
    date date,
    exception_type integer
);


ALTER TABLE public.gtfs_calendar_dates OWNER TO transit_indicators;

--
-- Name: gtfs_directions; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_directions (
    direction_id integer NOT NULL,
    description text
);


ALTER TABLE public.gtfs_directions OWNER TO transit_indicators;

--
-- Name: gtfs_fare_attributes; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_fare_attributes (
    fare_id text NOT NULL,
    price double precision NOT NULL,
    currency_type text NOT NULL,
    payment_method integer,
    transfers integer,
    transfer_duration integer,
    agency_id text
);


ALTER TABLE public.gtfs_fare_attributes OWNER TO transit_indicators;

--
-- Name: gtfs_fare_rules; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_fare_rules (
    fare_id text,
    route_id text,
    origin_id text,
    destination_id text,
    contains_id text,
    service_id text
);


ALTER TABLE public.gtfs_fare_rules OWNER TO transit_indicators;

--
-- Name: gtfs_feed_info; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_feed_info (
    feed_publisher_name text,
    feed_publisher_url text,
    feed_timezone text,
    feed_lang text,
    feed_version text,
    feed_start_date text,
    feed_end_date text
);


ALTER TABLE public.gtfs_feed_info OWNER TO transit_indicators;

--
-- Name: gtfs_frequencies; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_frequencies (
    trip_id text,
    start_time text NOT NULL,
    end_time text NOT NULL,
    headway_secs integer NOT NULL,
    exact_times integer,
    start_time_seconds integer,
    end_time_seconds integer
);


ALTER TABLE public.gtfs_frequencies OWNER TO transit_indicators;

--
-- Name: gtfs_location_types; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_location_types (
    location_type integer NOT NULL,
    description text
);


ALTER TABLE public.gtfs_location_types OWNER TO transit_indicators;

--
-- Name: gtfs_payment_methods; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_payment_methods (
    payment_method integer NOT NULL,
    description text
);


ALTER TABLE public.gtfs_payment_methods OWNER TO transit_indicators;

--
-- Name: gtfs_pickup_dropoff_types; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_pickup_dropoff_types (
    type_id integer NOT NULL,
    description text
);


ALTER TABLE public.gtfs_pickup_dropoff_types OWNER TO transit_indicators;

--
-- Name: gtfs_route_types; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_route_types (
    route_type integer NOT NULL,
    description text
);


ALTER TABLE public.gtfs_route_types OWNER TO transit_indicators;

--
-- Name: gtfs_routes; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_routes (
    route_id text NOT NULL,
    agency_id text,
    route_short_name text DEFAULT ''::text,
    route_long_name text DEFAULT ''::text,
    route_desc text,
    route_type integer,
    route_url text,
    route_color text,
    route_text_color text
);


ALTER TABLE public.gtfs_routes OWNER TO transit_indicators;

--
-- Name: gtfs_shape_geoms; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
-- Description: Table holding the polylines for each shape in the gtfs feed
--

CREATE TABLE gtfs_shape_geoms (
    shape_id text,
    the_geom geometry(LineString,4326),
    geom geometry(LineString,32616)
);


ALTER TABLE public.gtfs_shape_geoms OWNER TO transit_indicators;

--
-- Name: gtfs_shapes; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_shapes (
    shape_id text NOT NULL,
    shape_pt_lat double precision NOT NULL,
    shape_pt_lon double precision NOT NULL,
    shape_pt_sequence integer NOT NULL,
    shape_dist_traveled double precision,
    spcs geometry(Point,3529),
    geom geometry(Point,32616)
);


ALTER TABLE public.gtfs_shapes OWNER TO transit_indicators;

--
-- Name: gtfs_stop_times; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_stop_times (
    trip_id text,
    arrival_time text,
    departure_time text,
    stop_id text,
    stop_sequence integer NOT NULL,
    stop_headsign text,
    pickup_type integer,
    drop_off_type integer,
    shape_dist_traveled double precision,
    timepoint integer,
    arrival_time_seconds integer,
    departure_time_seconds integer,
    CONSTRAINT times_arrtime_check CHECK ((arrival_time ~~ '__:__:__'::text)),
    CONSTRAINT times_deptime_check CHECK ((departure_time ~~ '__:__:__'::text))
);


ALTER TABLE public.gtfs_stop_times OWNER TO transit_indicators;

--
-- Name: gtfs_stops; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_stops (
    stop_id text NOT NULL,
    stop_name text NOT NULL,
    stop_desc text,
    stop_lat double precision,
    stop_lon double precision,
    zone_id text,
    stop_url text,
    stop_code text,
    stop_street text,
    stop_city text,
    stop_region text,
    stop_postcode text,
    stop_country text,
    location_type integer,
    parent_station text,
    stop_timezone text,
    wheelchair_boarding integer,
    direction text,
    "position" text,
    the_geom geometry(Point,4326),
    geom geometry(Point,32616)
);


ALTER TABLE public.gtfs_stops OWNER TO transit_indicators;

--
-- Name: gtfs_transfer_types; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_transfer_types (
    transfer_type integer NOT NULL,
    description text
);


ALTER TABLE public.gtfs_transfer_types OWNER TO transit_indicators;

--
-- Name: gtfs_transfers; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_transfers (
    from_stop_id text,
    to_stop_id text,
    transfer_type integer,
    min_transfer_time integer,
    from_route_id text,
    to_route_id text,
    service_id text
);


ALTER TABLE public.gtfs_transfers OWNER TO transit_indicators;

--
-- Name: gtfs_trips; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_trips (
    route_id text,
    service_id text,
    trip_id text NOT NULL,
    trip_headsign text,
    direction_id integer,
    block_id text,
    shape_id text,
    trip_short_name text,
    wheelchair_accessible integer,
    trip_type text,
    direction text,
    schd_trip_id text
);


ALTER TABLE public.gtfs_trips OWNER TO transit_indicators;

--
-- Name: gtfs_wheelchair_accessibility; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_wheelchair_accessibility (
    wheelchair_accessibility integer NOT NULL,
    description text
);


ALTER TABLE public.gtfs_wheelchair_accessibility OWNER TO transit_indicators;

--
-- Name: gtfs_wheelchair_boardings; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
--

CREATE TABLE gtfs_wheelchair_boardings (
    wheelchair_boarding integer NOT NULL,
    description text
);


ALTER TABLE public.gtfs_wheelchair_boardings OWNER TO transit_indicators;

--
-- Name: gtfs_stops_routes_join; Type: TABLE; Schema: public; Owner: transit_indicators; Tablespace:
-- Description: Intermediate table populated with a many-to-many relationship of routes and stops
--

CREATE TABLE gtfs_stops_routes_join (
    stop_id text NOT NULL,
    route_id text NOT NULL
);
ALTER TABLE public.gtfs_stops_routes_join OWNER TO transit_indicators;

