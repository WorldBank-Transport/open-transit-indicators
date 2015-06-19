'use strict';
angular.module('transitIndicators')
.controller('OTIUserdataChangeTimezoneController',
            ['config', '$scope', 'OTILocalization', 'timezones',
            function (config, $scope, OTILocalization, timezones) {

    $scope.selectTimezone = function(timezone) {
        // TODO:
        // post endpoint to django to set system timezone

        //$translate.use(language).then(function() { location.reload(); });

        // $state.reload has a bug that does not actually force a refresh.
        // See: https://github.com/angular-ui/ui-router/issues/582
        // TODO: Use $state.reload() when ui-router is fixed
        //
        // The workaround for this ($state.transitionTo) also doesn't do a full refresh.
        // The javascript translations update, but none of the django-populated elements do.
        // A rudimentary location reload seems to be the easiest fix for this.
    };

    var initialize = function () {

        $scope.timezones = timezones;

    };

    initialize();

}]);
