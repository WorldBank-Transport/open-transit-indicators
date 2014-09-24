var UI = (function() {
    // TIME AND DURATION
    var wireUpTimePicker = function (requestModel) {
        var now = new Date();
        requestModel.setTime(now.getSeconds() + now.getMinutes()*60 + now.getHours()*60*60);
        $('#time-picker').timepicker({ 'scrollDefaultNow': true }).timepicker('setTime', now);
        $('#time-picker').on('changeTime', function() {
            var value = $(this).timepicker('getSecondsFromMidnight');
	    requestModel.setTime(value);
        });
        
        return {
            setTime: function(o) {
                $('#time-picker').timepicker('getSecondsFromMidnight');
            }
        }
    };

    var createDurationSlider = function(requestModel,onDynamicChange) {
        var slider = $("#duration-slider").slider({
            value: GTT.Constants.MAX_DURATION,
            min: 0,
            max: GTT.Constants.MAX_DURATION,
            step: 1,
            change: function( event, ui ) {
                if(!requestModel.getDynamicRendering()) {
                    requestModel.setDuration(ui.value);
                } else {
                    if(onDynamicChange) {
                        onDynamicChange();
                    }
	        }
            },
	    slide: function (event, ui) {
                if(requestModel.getDynamicRendering()) {
                    requestModel.setDuration(ui.value);
	        }
	    }
        });

        return {
            setDuration: function(o) {
                slider.slider('value', o);
            }
        }
    };

    // OTHER CONTROLS

    var createOpacitySlider = function(name,layer) {
        var opacitySlider = $(name).slider({
            value: 0.9,
            min: 0,
            max: 1,
            step: .02,
            slide: function( event, ui ) {
                layer.setOpacity(ui.value);
            }
        });
        return {
            setOpacity: function(o) {
                opacitySlider.slider('value', o);
            }
        }
    };

    var setupEvents = function(requestModel) {
        $("#schedule-dropdown-menu li a").click(function(){
            var selText = $(this).text();
            $(this).parents('.dropdown').find('.dropdown-toggle').html(selText+' <span class="caret"></span>');
            requestModel.setSchedule(selText.toLowerCase());
        });

        $("#direction-dropdown-menu li a").click(function(){
            var selText = $(this).text();
            $(this).parents('.dropdown').find('.dropdown-toggle').html(selText+' <span class="caret"></span>');
            requestModel.setDirection(selText.toLowerCase());
        });
        
        $('.scenicRouteBtn').on('click', function() {
            $('body').toggleClass('scenic-route');
        });

        $('#transit-types').find('label').tooltip({
            container: 'body',
            placement: 'bottom'
        });
        
        $('#toggle-sidebar-advanced').on('click', function() {
            $(this).toggleClass('active').next().slideToggle();
        });

        $('#vector_checkbox').click(function() {
            requestModel.setVector($('#vector_checkbox').is(':checked'));
        });
    };

    var wireUpDynamicRendering = function(requestModel) {
        $('#rendering_checkbox').click(function() {
	    if($('#rendering_checkbox').is(':checked')) {
                requestModel.setDynamicRendering(true);
	    } else {
                requestModel.setDynamicRendering(false);
	    }
        });
    };

    var setupTransitModes = function(requestModel) {
        $.each($("input[name='anytime-mode']"), function () {
            $(this).change(function() {
                if($(this).val() == 'walking') {
                    requestModel.removeMode("biking");
                    requestModel.addMode("walking");
                } else {
                    requestModel.removeMode("walking");
                    requestModel.addMode("biking");
                }
            });
        });

        $.each($("input[name='public-transit-mode']"), function () {
            $(this).change(function() {
                var val = $(this).val();
                if($(this).is(':checked')) {
                    requestModel.addMode(val);
                } else {
                    requestModel.removeMode(val);
                }
            });
        });
    };

    var createAddressSearch = function(name,callback) {
        $.each($("input[name='" + name + "']"), function() {
            var input = $(this);
            $(this).keypress(function (e) {
                if (e.which == 13) {
                    GTT.Geocoder.geocode(input.val(),callback);
                }
            });
            $(this).next().find('button').on('click',function (e) {
                GTT.Geocoder.geocode(input.val(),callback);
            });
        });
    };

    return {
        wireUp : function(requestModel) {
            setupEvents(requestModel);
            setupTransitModes(requestModel);
            wireUpTimePicker(requestModel);
        },
        createOpacitySlider : createOpacitySlider,
        createDurationSlider : createDurationSlider,
        wireUpDynamicRendering : wireUpDynamicRendering,
        createAddressSearch : createAddressSearch
    };
})();
