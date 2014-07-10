'use strict';

angular.module('transitIndicators')
.factory('OTIMapService',
        ['$q', '$resource',
        function ($q, $resource) {

    var otiMapService = {};

    // retrieves map information from the server
    otiMapService.getMapInfo = function() {
        var r = $resource('/gt/map-info');
        var dfd = $q.defer();

        var result = r.get({}, function() {
            dfd.resolve(result);
        });
        return dfd.promise;
    };

    return otiMapService;
}]);
