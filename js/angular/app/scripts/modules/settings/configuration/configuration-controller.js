'use strict';

angular.module('transitIndicators')
.controller('OTIConfigurationController',
        ['$scope', '$q', 'OTIConfigurationService',
        function ($scope, $q, OTIConfigurationService) {

    $scope.datepickerFormat = 'dd MMMM yyyy';
    $scope.dateOptions = {
        startingDay: 1,
        showWeeks: false
    };

    $scope.weekdayDate = new Date();
    $scope.weekendDate = new Date();
    $scope.hours = OTIConfigurationService.getHours();

    $scope.config = {};
    $scope.samplePeriods = null;

    var setConfig = function (config) {
        $scope.config = config;
        $scope.max_commute_time_min = config.max_commute_time_s / 60 || 0;
        $scope.max_walk_time_min = config.max_walk_time_s / 60 || 0;
    };

    var createWeekdayDateTime = function (date, hour) {
        var newDate = new Date(date.getTime());
        newDate.setHours(hour);
        newDate.setMinutes(0);
        newDate.setSeconds(0);
        return newDate;
    };

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

    var createDefaultPeriods = function () {
        var promises = [];
        var SamplePeriod = OTIConfigurationService.SamplePeriod;

        var date = new Date(2014, 0, 1);
        var morning = new SamplePeriod({type: 'morning'});
        morning.period_start = createWeekdayDateTime(date, 7);
        morning.period_end = createWeekdayDateTime(date, 9);
        promises.push(morning.$save());

        var midday = new SamplePeriod({type: 'midday'});
        midday.period_start = createWeekdayDateTime(date, 9);
        midday.period_end = createWeekdayDateTime(date, 16);
        promises.push(midday.$save());

        var evening = new SamplePeriod({type: 'evening'});
        evening.period_start = createWeekdayDateTime(date, 16);
        evening.period_end = createWeekdayDateTime(date, 18);
        promises.push(evening.$save());

        var night = new SamplePeriod({type: 'night'});
        night.period_start = createWeekdayDateTime(date, 18);
        night.period_end = createWeekdayDateTime(date, 24 + 7);
        promises.push(night.$save());

        var weekend = new SamplePeriod({type: 'weekend'});
        var weekendDates = createWeekendDateTimes(new Date(2014, 0, 4));
        weekend.period_start = weekendDates[0];
        weekend.period_end = weekendDates[1];
        promises.push(weekend.$save());

        return $q.all(promises);
    };

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

    $scope.saveConfig = function () {
        $scope.config.$update($scope.config, function (data) {
            console.log('Config updated:', data);
        });
    };

    $scope.saveSamplePeriods = function () {

        if ($scope.samplePeriodsForm.$invalid) {
            return;
        }
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
            console.log('Saved:', data);
        }, function (error) {
            console.error('Saved Error:', error);
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

    $scope.validateWeekday = function () {
        var isValid = OTIConfigurationService.isWeekday($scope.weekdayDate);
        $scope.samplePeriodsForm.weekdayDate.$setValidity('weekdayDate', isValid);
    };

    $scope.validateWeekend = function () {
        var isValid = OTIConfigurationService.isWeekend($scope.weekendDate);
        $scope.samplePeriodsForm.weekendDate.$setValidity('weekendDate', isValid);
    };

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

    $scope.disableWeekend = function (date, mode) {
        return (mode === 'day' && OTIConfigurationService.isWeekend(date));
    };

    $scope.disableWeekday = function (date, mode) {
        return (mode === 'day' && OTIConfigurationService.isWeekday(date));
    };

    $scope.init = function () {
        // get the global configuration object
        OTIConfigurationService.Config.query({}, function (configs) {
            if (configs.length !== 1) {
                console.error('Expected a single configuration, but found: ', configs);
                return;
            }
            setConfig(configs[0]);
        });

        OTIConfigurationService.SamplePeriod.get({type: 'morning'}, function () {
            // Configs exist, set UI
            OTIConfigurationService.SamplePeriod.query(function (data) {
                setSamplePeriods(data);
            }, function (error) {
                console.log('Get Error:', error);
            });
        }, function () {
            // No configs exist, create defaults
            // TODO: Move to service
            createDefaultPeriods().then(function (data) {
                console.log('Create:', data);
                setSamplePeriods(data);
            }, function (error) {
                // Display some kind of error here
                console.log('Create Error:', error);
            });
        });
    };
}]);
