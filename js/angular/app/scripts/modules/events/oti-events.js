'use strict';
angular.module('transitIndicators')
.factory('OTIEvents', [function () {
    var otiEvents = {};

    otiEvents.Settings = {
        Upload: {
            GTFSDone: 'OTIEvent:Settings:Upload:GTFSDone',
            GTFSDelete: 'OTIEvent:Settings:Upload:GTFSDelete'
        }
    };

    otiEvents.Root = {
        MapExtentUpdated: 'OTIEvent:Root:MapExtentUpdated'
    };

    return otiEvents;
}]);