'use strict';

angular.module('transitIndicators')
.factory('logoutInterceptor',
        ['$q', '$rootScope', 'OTIEvents',
        function ($q, $rootScope, OTIEvents) {
    return {
        'responseError': function(response) {
            if (response.status === 401) {
                $rootScope.$broadcast(OTIEvents.Auth.LogOutUser);
            }
            return $q.reject(response);
        }
    };
}]);
