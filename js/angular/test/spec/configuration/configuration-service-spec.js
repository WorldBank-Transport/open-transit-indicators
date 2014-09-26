'use strict';

describe('OTIConfigurationService', function () {
    var configService;

    var monday = new Date(2014, 0, 6);
    var tuesday = new Date(2014, 0, 7);
    var wednesday = new Date(2014, 0, 1);
    var thursday = new Date(2014, 0, 2);
    var friday = new Date(2014, 0, 3);
    var saturday = new Date(2014, 0, 4);
    var sunday = new Date(2014, 0, 5);

    beforeEach(module('transitIndicators'));
    beforeEach(inject(function (OTIConfigurationService) {
        configService = OTIConfigurationService;
    }));

    it('should ensure getHours returns an array', function () {
        var hours = configService.getHours();
        expect(hours.length).toEqual(25);
        expect(hours.slice(0, 3)).toEqual([0,1,2]);
    });

    it('should esnure isWeekday returns false if date null/missing', function () {
        var isWeekday = configService.isWeekday();
        expect(isWeekday).toEqual(false);
        isWeekday = configService.isWeekday(null);
        expect(isWeekday).toEqual(false);
    });

    it('should ensure isWeekday returns true if date is weekday', function () {
        expect(configService.isWeekday(monday)).toEqual(true);
        expect(configService.isWeekday(tuesday)).toEqual(true);
        expect(configService.isWeekday(wednesday)).toEqual(true);
        expect(configService.isWeekday(thursday)).toEqual(true);
        expect(configService.isWeekday(friday)).toEqual(true);
        expect(configService.isWeekday(saturday)).toEqual(false);
        expect(configService.isWeekday(sunday)).toEqual(false);

    });

    it('should ensure isWeekend returns false if date null/missing', function () {
        var isWeekend = configService.isWeekend();
        expect(isWeekend).toEqual(false);
        isWeekend = configService.isWeekend(null);
        expect(isWeekend).toEqual(false);
    });

    it('should ensure isWeekend returns true if date is weekday', function () {
        expect(configService.isWeekend(monday)).toEqual(false);
        expect(configService.isWeekend(tuesday)).toEqual(false);
        expect(configService.isWeekend(wednesday)).toEqual(false);
        expect(configService.isWeekend(thursday)).toEqual(false);
        expect(configService.isWeekend(friday)).toEqual(false);
        expect(configService.isWeekend(saturday)).toEqual(true);
        expect(configService.isWeekend(sunday)).toEqual(true);

    });

});