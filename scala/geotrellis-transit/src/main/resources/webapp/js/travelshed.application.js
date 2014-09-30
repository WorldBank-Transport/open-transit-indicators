var APP = (function() {
    var map = (function() {
        var m = L.map('map').setView(GTT.Constants.VIEW_COORDS, 12);
        m.lc = GTT.BaseLayers.addTo(m);

        $('#map').resize(function() {
            m.setView(m.getBounds(),m.getZoom());
        });

    var polygon = L.polygon(GTT.Constants.BORDER_POLY,
      {  color: 'black',
        fillColor: '#f03',
        fillOpacity: 0.0}).addTo(m);


        return m;
    })();

    var requestModel = GTT.createRequestModel();

    var transitShedLayer = (function() {
        var mapLayer = null;
        var opacity = 0.9;

        var update = function() {
            var modes = requestModel.getModesString();
            if(modes != "") {
                var latLng = requestModel.getLatLng();
                var time = requestModel.getTime();
                var direction = requestModel.getDirection();
                var schedule = requestModel.getSchedule();
                var dynamicRendering = requestModel.getDynamicRendering();

                if (mapLayer) {
                    map.lc.removeLayer(mapLayer);
                    map.removeLayer(mapLayer);
                    mapLayer = null;
                }

		if(dynamicRendering) {
                    var url = GTT.Constants.BASE_URL + "/travelshed/wmsdata";
		    mapLayer = new L.TileLayer.WMS2(url, {
                        getValue : function() { return requestModel.getDuration(); },
                        latitude: latLng.lat,
                        longitude: latLng.lng,
                        time: time,
                        duration: GTT.Constants.MAX_DURATION,
                        modes: modes,
                        schedule: schedule,
                        direction: direction,
                        breaks: GTT.Constants.BREAKS,
                        palette: GTT.Constants.COLORS,
                        attribution: 'Azavea'
		    });
		} else {
                    var url = GTT.Constants.BASE_URL + "/travelshed/wms";
		    mapLayer = new L.TileLayer.WMS(url, {
                        latitude: latLng.lat,
                        longitude: latLng.lng,
                        time: time,
                        duration: requestModel.getDuration(),
                        modes: modes,
                        schedule: schedule,
                        direction: direction,
                        breaks: GTT.Constants.BREAKS,
                        palette: GTT.Constants.COLORS,
                        attribution: 'Azavea'
		    });
                }
		
		mapLayer.setOpacity(opacity);
		mapLayer.addTo(map);
		map.lc.addOverlay(mapLayer, "Travel Times");
            }
        };

        requestModel.onChange(update);

        return {
            setOpacity : function(o) {
                opacity = o;
                if(mapLayer) { 
                    mapLayer.setOpacity(opacity); 
                }
            },
        };
    })();
    
    var vectorLayer = (function() {
        var vectorLayer = null;

        var update = function() {
            if (vectorLayer) {
                map.lc.removeLayer(vectorLayer);
                map.removeLayer(vectorLayer);
                vectorLayer = null; 
            }

            if(requestModel.getVector()) {
                var modes = requestModel.getModesString();
                if(modes != "") {
                    var latLng = requestModel.getLatLng();
                    var time = requestModel.getTime();
                    var duration = requestModel.getDuration();
                    var direction = requestModel.getDirection();
                    var schedule = requestModel.getSchedule();

                    $.ajax({
                        url: GTT.Constants.BASE_URL + '/travelshed/json',
                        dataType: "json",
                        data: { 
                            latitude: latLng.lat,
                            longitude: latLng.lng,
                            time: time,
                            durations: duration,
                            modes: modes,
                            schedule: schedule,
                            direction: direction,
			    cols: 200,
			    rows: 200
                        },
                        success: function(data) {
                            if (vectorLayer) {
                                map.lc.removeLayer(vectorLayer);
                                map.removeLayer(vectorLayer);
                                vectorLayer = null; 
                            }

                            var geoJsonOptions = {
                                style: function(feature) {
                                    return {
                                        weight: 2,
                                        color: "#774C4A",
                                        opacity: 1,
                                        fillColor: "#9EFAE2",
                                        fillOpacity: 0.2
                                    };
                                }
                            };

                            vectorLayer = 
                                L.geoJson(data, geoJsonOptions)
                                 .addTo(map);
                        }
                    })
                }
            }
        }

        requestModel.onChange(update);

        return { update : update };
    })();
                          
    var startMarker = (function() {
        var lat = GTT.Constants.START_LAT;
        var lng = GTT.Constants.START_LNG;

        var marker = L.marker([lat,lng], {
            draggable: true 
        }).addTo(map);
        
        marker.on('dragend', function(e) { 
            lat = marker.getLatLng().lat;
            lng = marker.getLatLng().lng;
            requestModel.setLatLng(lat,lng);
        } );

        return {
            getMarker : function() { return marker; },
            getLat : function() { return lat; },
            getLng : function() { return lng; },
            setLatLng : function(newLat,newLng) {
                lat = newLat;
                lng = newLng;
                marker.setLatLng(new L.LatLng(lat, lng));
                requestModel.setLatLng(lat,lng);
            }
        }
    })();

    return {
        onLoadGoogleApiCallback : GTT.Geocoder.onLoadGoogleApiCallback,
        onReady : function() {
            GTT.Geocoder.setup();
            UI.wireUp(requestModel);
            UI.wireUpDynamicRendering(requestModel);
            UI.createOpacitySlider("#opacity-slider",transitShedLayer);
            UI.createDurationSlider(requestModel,vectorLayer.update);
            UI.createAddressSearch("address", function(data) {
                data = {results: data};

                if (data.results.length != 0) {
                    var lat = data.results[0].geometry.location.lat();
                    var lng = data.results[0].geometry.location.lng();
                    startMarker.setLatLng(lat,lng);
                } else {
                    alert("Address not found!");
                }
            });
            requestModel.notifyChange();
        }
    };
})();

// On page load
$(document).ready(function() {
    APP.onReady();
});
