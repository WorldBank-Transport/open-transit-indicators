'use strict';
angular.module('transitIndicators')
.controller('OTIUserdataChangeTimezoneController',
            ['config', '$modalInstance', '$scope', '$state', '$cookieStore', 'OTILocalization', 'timezones',
            function (config, $modalInstance, $scope, $state, $cookieStore, OTILocalization, timezones) {

    $scope.selectTimezone = function(timezone) {
        OTILocalization.setTimeZone(timezone.zone).then(function(response) {
            $modalInstance.close();
            $state.go('transit');
        });
    };

    var initialize = function () {

        $scope.timezones = timezones;

    };

    initialize();

}]);
