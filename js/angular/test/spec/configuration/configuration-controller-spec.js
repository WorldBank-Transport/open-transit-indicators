'use strict';

describe('OTIConfigurationController', function () {
    var configCtl;
    var $scope;
    var $rootScope;
    var $compile;

    var monday = new Date(2014, 0, 6);
    var tuesday = new Date(2014, 0, 7);
    var wednesday = new Date(2014, 0, 1);
    var thursday = new Date(2014, 0, 2);
    var friday = new Date(2014, 0, 3);
    var saturday = new Date(2014, 0, 4);
    var sunday = new Date(2014, 0, 5);

    // Mock event object
    var $event = {
        preventDefault: function () {},
        stopPropagation: function () {}
    };

    beforeEach(module('transitIndicators'));
    beforeEach(inject(function ($controller, _$rootScope_) {
        $rootScope = _$rootScope_;
        $scope = $rootScope.$new();
        // Mock $setValidity(validationErrorKey, isValid)
        // https://docs.angularjs.org/api/ng/type/ngModel.NgModelController
        var mockSetValidity = function(validationErrorKey, isValid) {
            $scope.samplePeriodsForm[validationErrorKey].$valid = isValid;
            $scope.samplePeriodsForm[validationErrorKey].$invalid = !isValid;
        };
        // Mock our samplePeriodForm
        // Default form to invalid, then check for flips to valid in tests
        // Other option is to somehow compile the controller template and inject that
        $scope.samplePeriodsForm = {
            weekdayDate: {
                $setValidity: mockSetValidity,
                $valid: false,
                $invalid: true
            },
            weekendDate: {
                $setValidity: mockSetValidity,
                $valid: false,
                $invalid: true
            },
            morningPeakStart: {
                $setValidity: mockSetValidity,
                $valid: false,
                $invalid: true
            },
            morningPeakEnd: {
                $setValidity: mockSetValidity,
                $valid: false,
                $invalid: true
            },
            eveningPeakStart: {
                $setValidity: mockSetValidity,
                $valid: false,
                $invalid: true
            },
            eveningPeakEnd: {
                $setValidity: mockSetValidity,
                $valid: false,
                $invalid: true
            },
        };

        configCtl = $controller('OTIConfigurationController', {
            $scope: $scope
        });
    }));

    it('should ensure disableWeekend returns true when passed date is weekend', function () {
        expect($scope.disableWeekend(saturday, 'day')).toEqual(true);
        expect($scope.disableWeekend(monday, 'day')).toEqual(false);
    });

    it('should ensure disableWeekend returns false when mode !== "day"', function () {
        expect($scope.disableWeekend(saturday, 'month')).toEqual(false);
    });

    it('should ensure disableWeekday returns true when passed date is weekday', function () {
        expect($scope.disableWeekday(tuesday, 'day')).toEqual(true);
        expect($scope.disableWeekday(sunday, 'day')).toEqual(false);
    });

    it('should ensure disableWeekday returns false when mode !== "day"', function () {
        expect($scope.disableWeekday(tuesday, 'month')).toEqual(false);
    });

    it('should ensure openWeekdayPicker opens weekday picker', function (){
        $scope.weekdayPickerOpen = false;
        $scope.openWeekdayPicker($event);
        expect($scope.weekdayPickerOpen).toEqual(true);
    });

    it('should ensure openWeekendPicker opens weekend picker', function (){
        $scope.weekendPickerOpen = false;
        $scope.openWeekendPicker($event);
        expect($scope.weekendPickerOpen).toEqual(true);
    });

    it('should ensure validateWeekday sets form true if weekday set', function () {
        $scope.weekdayDate = monday;
        $scope.validateWeekday();
        expect($scope.samplePeriodsForm.weekdayDate.$valid).toEqual(true);

        // Test opposite direction, to false
        $scope.weekdayDate = saturday;
        $scope.validateWeekday();
        expect($scope.samplePeriodsForm.weekdayDate.$valid).toEqual(false);
    });

    it('should ensure validateWeekend sets form true if weekend set, false if not', function () {
        $scope.weekendDate = saturday;
        $scope.validateWeekend();
        expect($scope.samplePeriodsForm.weekendDate.$valid).toEqual(true);

        // Test opposite direction, to false
        $scope.weekendDate = thursday;
        $scope.validateWeekend();
        expect($scope.samplePeriodsForm.weekendDate.$valid).toEqual(false);
    });

    it('should ensure validateTimes sets all false if times descending', function () {
        $scope.morningPeakStart = 23;
        $scope.morningPeakEnd = 22;
        $scope.eveningPeakStart = 21;
        $scope.eveningPeakEnd = 20;
        $scope.validateTimes();
        expect($scope.samplePeriodsForm.morningPeakStart.$valid).toEqual(false);
        expect($scope.samplePeriodsForm.morningPeakEnd.$valid).toEqual(false);
        expect($scope.samplePeriodsForm.eveningPeakStart.$valid).toEqual(false);
        expect($scope.samplePeriodsForm.eveningPeakEnd.$valid).toEqual(false);
    });

    it('should ensure validateTimes sets all true if times ascending', function () {
        $scope.morningPeakStart = 12;
        $scope.morningPeakEnd = 13;
        $scope.eveningPeakStart = 14;
        $scope.eveningPeakEnd = 20;
        $scope.validateTimes();
        expect($scope.samplePeriodsForm.morningPeakStart.$valid).toEqual(true);
        expect($scope.samplePeriodsForm.morningPeakEnd.$valid).toEqual(true);
        expect($scope.samplePeriodsForm.eveningPeakStart.$valid).toEqual(true);
        expect($scope.samplePeriodsForm.eveningPeakEnd.$valid).toEqual(true);
    });

    it('should ensure validateTimes sets false if times are adjacent', function () {
        $scope.morningPeakStart = 12;
        $scope.morningPeakEnd = 13;
        $scope.eveningPeakStart = 13;
        $scope.eveningPeakEnd = 20;
        $scope.validateTimes();
        expect($scope.samplePeriodsForm.morningPeakStart.$valid).toEqual(true);
        expect($scope.samplePeriodsForm.morningPeakEnd.$valid).toEqual(false);
        expect($scope.samplePeriodsForm.eveningPeakStart.$valid).toEqual(false);
        expect($scope.samplePeriodsForm.eveningPeakEnd.$valid).toEqual(true);
    });

});