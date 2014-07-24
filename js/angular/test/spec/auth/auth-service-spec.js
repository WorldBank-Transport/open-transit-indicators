'use strict';

describe('Service: auth', function () {

    var $cookieStore;
    var authService;

    beforeEach(module('transitIndicators'));
    beforeEach(inject(function(_$cookieStore_) {
        $cookieStore = _$cookieStore_;
        authService = _authService_;
    }));

    /* If these keys are changed in authService, they must be updated in authInterceptor
     * as well, since the getToken function cannot be used there due to
     * a circular dependency.
     * The first set of tests ensure these keys are not errantly changed
     */
    var userIdKey = 'authService.userId';
    var tokenKey = 'authService.token';

    describe('cookie token tests', function () {
        it('getUserId $cookieStore should always use ' + userIdKey + ' as its key', function () {
            spyOn($cookieStore, 'get');
            authService.getUserId();
            expect($cookieStore.get).toHaveBeenCalledWith(userIdKey);
        });

        it('getUserToken $cookieStore should always use ' + tokenKey + ' as its key', function () {
            spyOn($cookieStore, 'get');
            authService.getToken();
            expect($cookieStore.get).toHaveBeenCalledWith(tokenKey);
        });

    });

    describe('isAuthenticated tests', function () {

        it('should ensure that a false userId key authenticates false', function () {
            $cookieStore.put(userIdKey, null);
            $cookieStore.put(tokenKey, 'abcde12345');
            expect(authService.isAuthenticated()).toEqual(false);
        });

        it('should ensure that a false token key authenticates false', function () {
            $cookieStore.put(userIdKey, 123);
            $cookieStore.put(tokenKey, null);
            expect(authService.isAuthenticated()).toEqual(false);
        });

        it('should ensure that a negative userId authenticates false', function () {
            $cookieStore.put(userIdKey, -123);
            $cookieStore.put(tokenKey, null);
            expect(authService.isAuthenticated()).toEqual(false);
        });

        it('should ensure that a zero userId authenticates true', function () {
            $cookieStore.put(userIdKey, 0);
            $cookieStore.put(tokenKey, 'abcde12345');
            expect(authService.isAuthenticated()).toEqual(true);
        });

        it('should ensure that both keys authenticates true', function () {
            $cookieStore.put(userIdKey, 123);
            $cookieStore.put(tokenKey, 'abcde12345');
            expect(authService.isAuthenticated()).toEqual(true);
        });
    
    });

});
