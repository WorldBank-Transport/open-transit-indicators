'use strict';

describe('Service: auth', function () {

    var $cookieStore;
    var authService;

    /* If these keys are changed in authService, they must be updated in authInterceptor
     * as well, since the getToken function cannot be used there due to
     * a circular dependency.
     * The first set of tests ensure these keys are not errantly changed
     */
    var userIdKey = 'authService.userId';
    var tokenKey = 'authService.token';

    beforeEach(module('transitIndicators'));
    beforeEach(inject(function(_$cookieStore_, _authService_) {
        $cookieStore = _$cookieStore_;
        authService = _authService_;
        $cookieStore.remove(userIdKey);
        $cookieStore.remove(tokenKey);
    }));

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

    describe('authenticate tests', function () {
        var $httpBackend;

        beforeEach(inject(function(_$httpBackend_) {
            $httpBackend = _$httpBackend_;
            $httpBackend.when('GET', 'i18n/en.json').respond({});
        }));

        afterEach(function() {
            $httpBackend.verifyNoOutstandingExpectation();
            $httpBackend.verifyNoOutstandingRequest();
        });

        it('should hit the /api-token-auth endpoint', function () {
            var auth = {
                username: 'testuser',
                password: '123'
            };
            var userId = 1;

            $httpBackend.expect('POST', '/api-token-auth/', auth).respond({
                user: userId,
                token: 'abcde12345'
            });
            // Need this expect because the authenticate() call fires a
            // $rootScope.$broadcast('authService:loggedIn')
            // Apparently the app still loads in the background and logic in app.js
            // sends these web requests
            $httpBackend.expectGET('scripts/modules/auth/login-partial.html').respond(200);

            expect(authService.isAuthenticated()).toEqual(false);
            authService.authenticate(auth);
            $httpBackend.flush();

            expect(authService.isAuthenticated()).toEqual(true);
            expect(authService.getUserId()).toEqual(userId);
        });

    });

});
