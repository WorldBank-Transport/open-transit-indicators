'use strict';
/**

// TODO: Refactor these event objects to their respective modules, following the pattern
in indicator-services folder. The global events object is silly, each module should define
its own events.

*/
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
        MapExtentUpdated: 'OTIEvent:Root:MapExtentUpdated'
    };

    otiEvents.Auth = {
        LoggedIn: 'AuthService:LoggedIn',
        LoggedOut: 'AuthService:LoggedOut',
        LogOutUser: 'AuthService:LogOutUser'
    };

    return otiEvents;
}]);
