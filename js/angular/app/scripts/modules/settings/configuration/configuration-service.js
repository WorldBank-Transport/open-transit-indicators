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

        createDateFromISO: function (str) {
            if (!str) {
                return null;
            }

           var dt = new Date(str);
           dt = new Date(dt.getUTCFullYear(), dt.getUTCMonth(), dt.getUTCDate(), 0, 0, 0);
           return dt;
        },

        ServiceDates: $resource('/gt/utils/service-dates', {}, {}),

        SamplePeriodTypes: ['morning', 'midday', 'evening', 'night', 'weekend']
    };
}]);

