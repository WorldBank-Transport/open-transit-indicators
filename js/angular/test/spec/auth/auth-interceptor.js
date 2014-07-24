'use strict';

describe('authInterceptor', function() {
    var authInterceptor;
    var config;
    var $cookieStore;

    beforeEach(function () {
        module('transitIndicators');
        inject(function (_authInterceptor_, _$cookieStore_) {
            authInterceptor = _authInterceptor_;
            $cookieStore = _$cookieStore_;
        });
        $cookieStore.put('authService.token', 'TESTAUTHTOKEN');
        config = {
            url: '',
            headers: {}
        }
    });

    it('should have a request function', function() {
        expect(angular.isFunction(authInterceptor.request)).toBe(true);
    });

    it('should add authorization header on API requests', function() {
        config.url = '/api/some-api-endpoint';
        config = authInterceptor.request(config);
        expect('Authorization' in config.headers).toBe(true);
    });

    it('should not add authorization headers on other requests', function() {
        config.url = '/some-non-api-endpoint';
        config = authInterceptor.request(config);
        expect('Authorization' in config.headers).toBe(false);
    });

    it('should not add authorization headers on api-token-auth requests', function() {
        config.url = '/api-token-auth';
        config = authInterceptor.request(config);
        expect('Authorization' in config.headers).toBe(false);
    });
});
