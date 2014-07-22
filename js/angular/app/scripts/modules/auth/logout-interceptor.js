'use strict';

angular.module('transitIndicators')
.factory('logoutInterceptor', 
        ['$q', '$rootScope',
        function ($q, $rootScope) {
    return {
        'responseError': function(response) {
            if (response.status === 401) {
                $rootScope.$broadcast('authService:logOutUser');
            }
            return $q.reject(response);
        }
    };
}]);
