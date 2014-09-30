var GTT = (function() { 
    var Constants = (function() {
        var MAX_DURATION = 60 * 60;
        var d = new Date();
        var INITIAL_TIME = d.getTime() - d.setHours(0,0,0,0);

        var baseUrl = "http://localhost:9999/api";
//        var baseUrl = baseUrl || "http://transit.geotrellis.com/api";

//Extent(112.72662842219742, 34.26084468393921, 114.21564888769542, 34.98327924830207)
    var borderPoly = [
        [34.26084468393921, 112.72662842219742],
        [34.26084468393921, 114.21564888769542],
        [34.98327924830207, 114.21564888769542],
        [34.98327924830207, 112.72662842219742]
    ];

  var startLat = 34.739981;
  var startLng = 113.680349;

  var viewCoords = [startLat, startLng];


//        var viewCoords = [39.9886950160466,-75.1519775390625];
        var geoCodeLowerLeft = { lat: 39.7353312333975, lng: -75.4468831918069 };
        var geoCodeUpperRight = { lat: 40.1696687666025, lng: -74.8802888081931 };

//        var startLat = 39.950510086014404;   
//        var startLng = -75.1640796661377;

        // For scenic route
        var destLat = 39.970929;
        var destLng = -75.142708;



        var breaks = 
            _.reduce(_.map([10,15,20,30,40,50,60,75,90,120], function(minute) { return minute*60; }),
                     function(s,i) { return s + "," + i.toString(); })

        var colors = "0xF68481,0xFDB383,0xFEE085," + 
            "0xDCF288,0xB6F2AE,0x98FEE6,0x83D9FD,0x81A8FC,0x8083F7,0x7F81BD"

        return {
            MAX_DURATION : MAX_DURATION,
            INITIAL_TIME : INITIAL_TIME,
            COLORS : colors,
            BREAKS : breaks,
            START_LAT : startLat,
            START_LNG : startLng,
            END_LAT : destLat,
            END_LNG : destLng,
            VIEW_COORDS : viewCoords,
            GEOCODE_LOWERLEFT : geoCodeLowerLeft,
            GEOCODE_UPPERRIGHT: geoCodeUpperRight,
            BASE_URL : baseUrl,
            BORDER_POLY : borderPoly
        };
    })();

    var BaseLayers = (function() {
        var getLayer = function(url,attrib) {
            return L.tileLayer(url, { maxZoom: 18, attribution: attrib });
        };        

        var layers = {
            stamen: { 
                toner:  'http://{s}.tile.stamen.com/toner/{z}/{x}/{y}.png',   
                terrain: 'http://{s}.tile.stamen.com/terrain/{z}/{x}/{y}.png',
                watercolor: 'http://{s}.tile.stamen.com/watercolor/{z}/{x}/{y}.png',
                attrib: 'Map data &copy;2013 OpenStreetMap contributors, Tiles &copy;2013 Stamen Design'
            },
            mapBox: {
                azavea:     'http://{s}.tiles.mapbox.com/v3/azavea.map-zbompf85/{z}/{x}/{y}.png',
                wnyc:       'http://{s}.tiles.mapbox.com/v3/jkeefe.map-id6ukiaw/{z}/{x}/{y}.png',
                worldGlass:     'http://{s}.tiles.mapbox.com/v3/mapbox.world-glass/{z}/{x}/{y}.png',
                worldBlank:  'http://{s}.tiles.mapbox.com/v3/mapbox.world-blank-light/{z}/{x}/{y}.png',
                worldLight: 'http://{s}.tiles.mapbox.com/v3/mapbox.world-light/{z}/{x}/{y}.png',
                attrib: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery &copy; <a href="http://mapbox.com">MapBox</a>'
            }
        };

        var baseLayers = {
            "Azavea" : getLayer(layers.mapBox.azavea,layers.mapBox.attrib),
            "WNYC" : getLayer(layers.mapBox.wnyc,layers.mapBox.attrib),
            "World Light" : getLayer(layers.mapBox.worldLight,layers.mapBox.attrib),
            "Terrain" : getLayer(layers.stamen.terrain,layers.stamen.attrib),
            "Watercolor" : getLayer(layers.stamen.watercolor,layers.stamen.attrib),
            "Toner" : getLayer(layers.stamen.toner,layers.stamen.attrib),
            "Glass" : getLayer(layers.mapBox.worldGlass,layers.mapBox.attrib),
            "Blank" : getLayer(layers.mapBox.worldBlank,layers.mapBox.attrib)
        };

        var defaultLayer = baseLayers["Azavea"];

        return {
            addTo : function(m) {
                defaultLayer.addTo(m);
                return L.control.layers(baseLayers).addTo(m);                
            }
        };
    })();

    var Geocoder = (function(){
        var geocoder = null;
        return {
            onLoadGoogleApiCallback : function() {
                geocoder = new google.maps.Geocoder();
                document.body.removeChild(document.getElementById('load_google_api'));
            },
            setup : function() {
                var url = 
                    "https://maps.googleapis.com/maps/api/js?" + 
                    "v=3&callback=APP.onLoadGoogleApiCallback&sensor=false";
                var script = document.createElement('script');
                script.id = 'load_google_api';
                script.type = "text/javascript";
                script.src = url;
                document.body.appendChild(script);
            },
            geocode : function(address,callback) {
                var lowerLeft = new google.maps.LatLng(Constants.GEOCODE_LOWERLEFT.lat, 
                                                       Constants.GEOCODE_LOWERLEFT.lng);
                var upperRight = new google.maps.LatLng(Constants.GEOCODE_UPPERRIGHT.lat, 
                                                        Constants.GEOCODE_UPPERRIGHT.lng);
                var bounds = new google.maps.LatLngBounds(lowerLeft, upperRight);

                var parameters = {
                    address: address,
                    bounds: bounds
                };

                var results = geocoder.geocode(parameters, callback);
            }
        };
    })();

    var createRequestModel = function() {
        var listeners = [];
        var time = Constants.INITIAL_TIME;
        var duration = Constants.MAX_DURATION;
        var travelModes = ['walking'];
        var schedule = "weekday";
        var direction = "departing";
        var lat = Constants.START_LAT;
        var lng = Constants.START_LNG;
        var dynamicRendering = false;
        var vector = false;

        var notifyChange = function() { 
            _.each(listeners, function(f) { f(); });
        }

        return {
            notifyChange: notifyChange,
            onChange : function(f) {
                listeners.push(f);
            },
            setLatLng : function(newLat,newLng) {
                lat = newLat;
                lng = newLng;
                notifyChange();
            },
            getLatLng : function() {
                return { lat: lat, lng: lng };
            },
            setTime: function(newTime) {
                time = newTime;
                notifyChange();
            },
            getTime: function() {
                return time;
            },
            setDuration: function(newDuration) {
                duration = newDuration;
                if(!dynamicRendering) {
                    notifyChange();
                };
            },
            getDuration: function() {
                return duration;
            },
            addMode: function(mode) {
                if(!_.contains(travelModes,mode)) {
                    travelModes.push(mode);
                    notifyChange();
                };
            },
            removeMode: function(mode) {
                if(_.contains(travelModes,mode)) {
                    var i = travelModes.indexOf(mode);
                    travelModes.splice(i,1);
                    notifyChange();
                };
            },
            getModes: function() {
                return travelModes;
            },
            getModesString : function() {
                if(travelModes.length == 0) { return ""; }
                else {
                    return _.reduce(travelModes, 
                                    function(s,v) { return s + "," + v; });
                };
            },
            setSchedule: function(newSchedule) {
                schedule = newSchedule;
                notifyChange();
            },
            getSchedule: function() { 
                return schedule;
            },
            setDirection: function(newDirection) {
                direction = newDirection;
                notifyChange();
            },
            getDirection: function() { 
                return direction;
            },
            setDynamicRendering: function(newDynamicRendering) {
                dynamicRendering = newDynamicRendering;
                notifyChange();
            },
            getDynamicRendering: function() { 
                return dynamicRendering;
            },
            setVector: function(newVector) {
                vector = newVector;
                notifyChange();
            },
            getVector: function() { 
                return vector;
            }
        }
    };

    return {
        Constants : Constants,
        BaseLayers : BaseLayers,
        Geocoder : Geocoder,
        createRequestModel : createRequestModel
    }
})();
