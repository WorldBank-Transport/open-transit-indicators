'use strict';

/**
 * Service responsible for getting/updating global configuration
 */
angular.module('transitIndicators')
.factory('OTIConfigurationService', ['$resource', function($resource) {
    return {

        // Allow selection of 0-24 hours inclusive
        getHours: function () {
            var hours = [];
            for (var i = 0; i < 25; i++) {
                hours.push(i);
            }
            return hours;
        },

        isWeekday: function(date) {
            if (!date) {
                return false;
            }
            var day = date.getDay();
            return (day === 1 || day === 2 || day === 3 || day === 4 || day === 5);
        },

        isWeekend: function (date) {
            if (!date) {
                return false;
            }
            var day = date.getDay();
            return (day === 0 || day === 6);
        },

        Config: $resource('/api/config/:id/ ', {}, {
            update: {
                method: 'PATCH',
                url: '/api/config/:id'
            }
        }),

        SamplePeriod: $resource('/api/sample-periods/:type/ ', {type: '@type'}, {
            update: {
                method: 'PUT',
                url: '/api/sample-periods/:type/ '
            }
        }),
        
        createDateFromISO: function (str) {
            if (!str) {
                return null;
            }
            
           var dt = new Date(str);
           
           // explicitly set time part for local time zone
           dt.setHours(0);
           dt.setMinutes(0);
           dt.setSeconds(0);
           return dt;
        },
        
        ServiceDates: $resource('/gt/service-dates', {}, {}),
        
        SamplePeriodTypes: ['morning', 'midday', 'evening', 'night', 'weekend']
    };
}]);

