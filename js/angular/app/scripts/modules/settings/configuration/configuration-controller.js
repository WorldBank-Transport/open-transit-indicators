'use strict';

angular.module('transitIndicators')
.controller('OTIConfigurationController',
        ['$scope', '$q', 'OTIConfigurationService',
        function ($scope, $q, OTIConfigurationService) {

    // Datepicker Options
    $scope.datepickerFormat = 'dd MMMM yyyy';
    $scope.dateOptions = {
        startingDay: 1,
        showWeeks: false
    };

    // Initialize defaults
    $scope.weekdayDate = null;
    $scope.weekendDate = null;
    $scope.hours = OTIConfigurationService.getHours();

    $scope.config = null;
    $scope.samplePeriods = null;
    $scope.serviceStart = null;
    $scope.serviceEnd = null;

    $scope.savePeriodsButton = {
        text: 'STATUS.SAVE',
        enabled: true
    };
    $scope.saveConfigButton = {
        text: 'STATUS.SAVE',
        enabled: true
    };
    $scope.samplePeriodsError = false;
    $scope.configError = false;

    /**
     * Helper setter for $scope.saveConfigButton
     * @param enabled true/false to toggle button state
     */
    var setSaveConfigButton = function (enabled) {
        if (enabled) {
            $scope.saveConfigButton = {
                text: 'STATUS.SAVE',
                enabled: true
            };
        } else {
            $scope.saveConfigButton = {
                text: 'STATUS.SAVING',
                enabled: false
            };
        }
    };

    /**
     * Helper setter for $scope.savePeriodsButton
     * @param enabled true/false to toggle button state
     */
    var setSavePeriodsButton = function (enabled) {
        if (enabled) {
            $scope.savePeriodsButton = {
                text: 'STATUS.SAVE',
                enabled: true
            };
        } else {
            $scope.savePeriodsButton = {
                text: 'STATUS.SAVING',
                enabled: false
            };
        }
    };

    /**
     * Setter for $scope.config
     */
    var setConfig = function (config) {
        $scope.config = config;
        if (config) {
            $scope.max_commute_time_min = config.max_commute_time_s / 60 || 0;
            $scope.max_walk_time_min = config.max_walk_time_s / 60 || 0;
        }
    };

    /**
     * For a given weekday period, a weekday datetime is always on the hour
     *
     * @param date object
     * @param hour int hour > 0 to set the returned date hour to
     *
     * @return date object with hour set to hour and min/sec zeroed
     */
    var createWeekdayDateTime = function (date, hour) {
        var newDate = new Date(date.getTime());
        newDate.setHours(hour);
        newDate.setMinutes(0);
        newDate.setSeconds(0);
        return newDate;
    };

    /**
     * For a given date, a weekend period is always the 24 hours for that day
     *
     * @param date object
     *
     * @return array with two date objects, [0] is start, [1] is end
     */
    var createWeekendDateTimes = function (date) {
        var start = new Date(date.getTime());
        start.setHours(0);
        start.setMinutes(0);
        start.setSeconds(0);

        var end = new Date(date);
        end.setHours(23);
        end.setMinutes(59);
        end.setSeconds(59);
        return [start, end];
    };

    /**
     * If no SamplePeriod objects are saved in the API, create this default configuration
     *  and POST it to the API
     *
     * @return a $q.all() promise that succeeds when all five types are POST to the API
     */
    var createDefaultPeriods = function () {
        var promises = [];
        var SamplePeriod = OTIConfigurationService.SamplePeriod;

        var date = new Date(2014, 0, 1);
        var morning = new SamplePeriod({type: 'morning'});
        morning.period_start = createWeekdayDateTime(date, 7);
        morning.period_end = createWeekdayDateTime(date, 9);
        promises.push(morning.$update());

        var midday = new SamplePeriod({type: 'midday'});
        midday.period_start = createWeekdayDateTime(date, 9);
        midday.period_end = createWeekdayDateTime(date, 16);
        promises.push(midday.$update());

        var evening = new SamplePeriod({type: 'evening'});
        evening.period_start = createWeekdayDateTime(date, 16);
        evening.period_end = createWeekdayDateTime(date, 18);
        promises.push(evening.$update());

        var night = new SamplePeriod({type: 'night'});
        night.period_start = createWeekdayDateTime(date, 18);
        night.period_end = createWeekdayDateTime(date, 24 + 7);
        promises.push(night.$update());

        var weekend = new SamplePeriod({type: 'weekend'});
        var weekendDates = createWeekendDateTimes(new Date(2014, 0, 4));
        weekend.period_start = weekendDates[0];
        weekend.period_end = weekendDates[1];
        promises.push(weekend.$update());

        return $q.all(promises);
    };

    /**
     * Creates the $scope.samplePeriods dictionary with a key for each
     * OTIConfigurationService.SamplePeriodTypes
     *
     * @param samplePeriods: array of SamplePeriod resources
     */
    var setSamplePeriods = function (samplePeriods) {
        $scope.samplePeriods = {};
        _.each(OTIConfigurationService.SamplePeriodTypes, function (type) {
            $scope.samplePeriods[type] = _.find(samplePeriods, function (period) { return period.type === type; });
        });
        setSamplePeriodsUI();
    };

    var setSamplePeriodsUI = function () {
        var morningStart = new Date($scope.samplePeriods.morning.period_start);
        var morningEnd = new Date($scope.samplePeriods.morning.period_end);
        var eveningStart = new Date($scope.samplePeriods.evening.period_start);
        var eveningEnd = new Date($scope.samplePeriods.evening.period_end);
        var weekendStart =  new Date($scope.samplePeriods.weekend.period_start);
        $scope.morningPeakStart = morningStart.getHours();
        $scope.morningPeakEnd = morningEnd.getHours();
        $scope.eveningPeakStart = eveningStart.getHours();
        $scope.eveningPeakEnd = eveningEnd.getHours();
        $scope.weekdayDate = morningStart;
        $scope.weekendDate = weekendStart;
    };

    var setSidebarCheckmark = function () {
        var isValid = $scope.configForm.$valid && $scope.samplePeriodsForm.$valid;
        $scope.setSidebarCheckmark('configuration', isValid);
    };

    $scope.saveConfig = function () {
        $scope.configError = false;
        setSaveConfigButton(false);
        $scope.config.$update($scope.config, function (data) {
            setSaveConfigButton(true);
        }, function () {
            setSaveConfigButton(true);
            $scope.configError = true;
        });
    };

    /**
     * Save sample periods to API
     *
     * Must succeed/fail as a group, as there are five periods but the user only
     * configures 3. Night and midday fill the gap between morning/evening on weekday,
     * and weekend period is all day on a weekend.
     * For weekend, we save 00:00:00-23:59:59 since API rejects a full 24 hours.
     *
     */
    $scope.saveSamplePeriods = function () {

        if ($scope.samplePeriodsForm.$invalid) {
            return;
        }

        setSavePeriodsButton(false);
        $scope.samplePeriodsError = false;
        var promises = [];

        var weekdayDate = $scope.weekdayDate;
        var weekendDate = $scope.weekendDate;

        var morning = $scope.samplePeriods.morning;
        morning.period_start = createWeekdayDateTime(weekdayDate, $scope.morningPeakStart);
        morning.period_end = createWeekdayDateTime(weekdayDate, $scope.morningPeakEnd);
        promises.push(morning.$update());

        var midday = $scope.samplePeriods.midday;
        midday.period_start = createWeekdayDateTime(weekdayDate, $scope.morningPeakEnd);
        midday.period_end = createWeekdayDateTime(weekdayDate, $scope.eveningPeakStart);
        promises.push(midday.$update());

        var evening = $scope.samplePeriods.evening;
        evening.period_start = createWeekdayDateTime(weekdayDate, $scope.eveningPeakStart);
        evening.period_end = createWeekdayDateTime(weekdayDate, $scope.eveningPeakEnd);
        promises.push(evening.$update());

        var night = $scope.samplePeriods.night;
        night.period_start = createWeekdayDateTime(weekdayDate, $scope.eveningPeakEnd);
        night.period_end = createWeekdayDateTime(weekdayDate, 24 + $scope.morningPeakStart);
        promises.push(night.$update());

        var weekendDates = createWeekendDateTimes(weekendDate);
        var weekend = $scope.samplePeriods.weekend;
        weekend.period_start = weekendDates[0];
        weekend.period_end = weekendDates[1];
        promises.push(weekend.$update());

        $q.all(promises).then(function (data) {
            setSavePeriodsButton(true);
        }, function (error) {
            setSavePeriodsButton(true);
            $scope.samplePeriodsError = true;
        });
    };

    $scope.openWeekdayPicker = function ($event) {
        $event.preventDefault();
        $event.stopPropagation();
        $scope.weekdayPickerOpen = true;
    };

    $scope.openWeekendPicker = function ($event) {
        $event.preventDefault();
        $event.stopPropagation();
        $scope.weekendPickerOpen = true;
    };

    /**
     * Invalidate samplePeriodsForm if weekdayDate is not a weekday in service date range
     */
    $scope.validateWeekday = function () {
        var isValid = OTIConfigurationService.isWeekday($scope.weekdayDate) && 
                      isInServiceRange($scope.weekdayDate);
        $scope.samplePeriodsForm.weekdayDate.$setValidity('weekdayDate', isValid);
    };

    /**
     * Invalidate samplePeriodsForm if weekendDate is not a weekend in service date range
     */
    $scope.validateWeekend = function () {
        var isValid = OTIConfigurationService.isWeekend($scope.weekendDate) &&
                      isInServiceRange($scope.weekendDate);
        $scope.samplePeriodsForm.weekendDate.$setValidity('weekendDate', isValid);
    };
    
    /**
     * Returns true if the given Date object is within the feed's service range
     */
    var isInServiceRange = function (date) {
        if (!$scope.serviceStart || !$scope.serviceEnd) {
            return true;
        }
        
        if (date && date >= $scope.serviceStart && date <= $scope.serviceEnd) {
            return true;
        }
        
        return false;
    };

    /**
     * Invalidate one of the peak times if it is not between the two it borders,
     * i.e. morningPeakStart < morningPeakEnd < eveningPeakStart
     */
    $scope.validateTimes = function () {
        var isValid = false;

        // Set morningPeakStart
        isValid = ($scope.morningPeakStart < $scope.morningPeakEnd);
        $scope.samplePeriodsForm.morningPeakStart.$setValidity('morningPeakStart', isValid);

        // Set morningPeakEnd
        isValid = ($scope.morningPeakStart < $scope.morningPeakEnd &&
                   $scope.morningPeakEnd < $scope.eveningPeakStart);
        $scope.samplePeriodsForm.morningPeakEnd.$setValidity('morningPeakEnd', isValid);

        // Set eveningPeakStart
        isValid = ($scope.morningPeakEnd < $scope.eveningPeakStart &&
                   $scope.eveningPeakStart < $scope.eveningPeakEnd);
        $scope.samplePeriodsForm.eveningPeakStart.$setValidity('eveningPeakStart', isValid);

        // Set eveningPeakEnd
        isValid = ($scope.eveningPeakStart < $scope.eveningPeakEnd);
        $scope.samplePeriodsForm.eveningPeakEnd.$setValidity('eveningPeakEnd', isValid);
    };

    /**
     * Helper function for weekday datepicker
     */
    $scope.disableWeekend = function (date, mode) {
        return (mode === 'day' && OTIConfigurationService.isWeekend(date));
    };

    /**
     * Helper function for weekend datepicker
     */
    $scope.disableWeekday = function (date, mode) {
        return (mode === 'day' && OTIConfigurationService.isWeekday(date));
    };
    
    /**
     * Get the start and end dates for the feed's service from the query response object
     */
    var setServiceDateRange = function (obj) {
        var serviceDates = obj.serviceDates;
        if (serviceDates !== null) {
            $scope.serviceStart = OTIConfigurationService.createDateFromISO(serviceDates.start);
            $scope.serviceEnd = OTIConfigurationService.createDateFromISO(serviceDates.end);
            
            // if dates have not been chosen yet, default to first valid date in range
            if (!$scope.weekdayDate) {
                var nextDay = new Date($scope.serviceStart);
                while (!OTIConfigurationService.isWeekday(nextDay)) {
                  nextDay.setDate(nextDay.getDate() + 1);
                }
                $scope.weekdayDate = nextDay;
            }
            
            if (!$scope.weekendDate) {
                var nextDay = new Date($scope.serviceStart);
                while (!OTIConfigurationService.isWeekend(nextDay)) {
                  nextDay.setDate(nextDay.getDate() + 1);
                }
                $scope.weekendDate = nextDay;
            }
        } else {
            var error = obj['error'];
            if (error !== null) {
                console.log("Server returned error fetching feed service dates:");
                console.log(error);
            } else {
                // No service dates found; GTFS probably isn't loaded yet.
            }
        }
    };
    
    /**
     * Create a JS Date object from an ISO-formatted date string
     */
    
    /**
     * Initialize config page with data
     */
    $scope.init = function () {
        // get the global configuration object
        OTIConfigurationService.Config.query({}, function (configs) {
            if (configs.length !== 1) {
                $scope.configLoadError = true;
                return;
            }
            setConfig(configs[0]);
        }, function () {
            $scope.configLoadError = true;
        });
        
        // get service date range
        OTIConfigurationService.ServiceDates.get({}, function (data) {
            setServiceDateRange(data);
        }, function () {
          console.log("Error fetching feed service dates!");
        });

        OTIConfigurationService.SamplePeriod.get({type: 'morning'}, function () {
            // Configs exist, set UI
            OTIConfigurationService.SamplePeriod.query(function (data) {
                setSamplePeriods(data);
            }, function () {
                // Unable to make web request, so hide the samplePeriods form and show
                //  static error message
                $scope.samplePeriodsLoadError = true;
                $scope.samplePeriods = {};
            });
        }, function () {
            // No configs exist, so create sensible defaults,
            //  then set the UI
            // TODO: Move to service?
            createDefaultPeriods().then(function (data) {
                setSamplePeriods(data);
            }, function () {
                // Hide the samplePeriods form and show static error message
                $scope.samplePeriodsLoadError = true;
                $scope.samplePeriods = {};
            });
        });
    };
}]);
