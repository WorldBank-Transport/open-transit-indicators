'use strict';
angular.module('transitIndicators')
.factory('OTIEvents', [function () {
    var otiEvents = {};

    otiEvents.Settings = {
        Upload: {
            GTFSDone: 'OTIEvent:Settings:Upload:GTFSDone',
            GTFSDelete: 'OTIEvent:Settings:Upload:GTFSDelete'
        },
        Demographics: {
            AssignmentDone: 'OTIEvent:Settings:Demographics:AssignmentDone'
        }
    };

    otiEvents.Root = {
        MapExtentUpdated: 'OTIEvent:Root:MapExtentUpdated',
        VisibleModesSelected: 'OTIEvent:Root:VisibleModesSelected',
        AvailableModesUpdated: 'OTIEvent:Root:AvailableModesUpdated'
    };

    otiEvents.Indicators = {
        IndicatorCalcJobUpdated: 'OTIIndicatorsService:IndicatorCalcJobUpdated',
        IndicatorUpdated: 'OTIIndicatorsService:IndicatorUpdated',
        SamplePeriodUpdated: 'OTIIndicatorsService:SamplePeriodUpdated',
        CitiesUpdated: 'OTIIndicatorsService:CitiesUpdated'
    };

    otiEvents.Auth = {
        LoggedIn: 'AuthService:LoggedIn',
        LoggedOut: 'AuthService:LoggedOut',
        LogOutUser: 'AuthService:LogOutUser'
    };

    return otiEvents;
}]);
