'use strict';

angular.module('transitIndicators')
.factory('authInterceptor', 
        ['$q', '$cookieStore',
        function ($q, $cookieStore) {
    return {
        'request': function(config) {
            if (config.url.indexOf('/api') === 0) {
                if (!(config.url.indexOf('/api-token-auth') === 0)) {
                    config.headers.Authorization = 'Token ' + $cookieStore.get('authService.token');
                }
            }
            return config || $q.when(config);
        }
    };
}]);
