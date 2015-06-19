'use strict';
angular.module('transitIndicators')
.controller('OTIUserdataChangeTimezoneController',
            ['config', '$scope', 'OTILocalization', 'timezones',
            function (config, $scope, OTILocalization, timezones) {

    $scope.selectTimezone = function(timezone) {
        OTILocalization.setTimeZone(timezone.zone).then(function(response) {
            location.reload();
        });
    };

    var initialize = function () {

        $scope.timezones = timezones;

    };

    initialize();

}]);
