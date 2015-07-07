'use strict';

/*jshint camelcase: false */
describe('Service: Indicators', function () {

    var indicatorsService;
    var typesService;

    beforeEach(module('transitIndicators'));
    beforeEach(inject(function(_OTIIndicatorManager_, _OTITypes_) {
        indicatorsService = _OTIIndicatorManager_;
        typesService = _OTITypes_;
    }));

    describe('IndicatorsConfig tests', function () {
        it('should have version, sample_period, type, aggregation properties by default', function () {
            var indicator = indicatorsService.getConfig();
            expect(indicator.calculation_job).toBeDefined();
            expect(indicator.type).toBeDefined();
            expect(indicator.aggregation).toBeDefined();
            expect(indicator.sample_period).toBeDefined();
        });
    });

    describe('getIndicatorTypes tests', function () {
        var $httpBackend;

        beforeEach(inject(function(_$httpBackend_) {
            $httpBackend = _$httpBackend_;
            $httpBackend.when('GET', 'i18n/en.json').respond({});
        }));

        afterEach(function() {
            $httpBackend.verifyNoOutstandingExpectation();
            $httpBackend.verifyNoOutstandingRequest();
        });

        it('should hit the /api/indicator-types/ endpoint', function () {

            $httpBackend.expect('GET', '/api/indicator-types/').respond({
                num_stops: 'Number of Stops'
            });

            // Need these two expects because the authenticate() call fires a
            // $rootScope.$broadcast('authService:loggedIn')
            // Apparently the app still loads in the background and logic in app.js
            // sends these web requests
            $httpBackend.expectGET('scripts/modules/auth/login-partial.html').respond(200);

            var types = typesService.getIndicatorTypes();

            $httpBackend.flush();

            expect(types).toBeDefined();
        });

        it('should hit the /api/indicator-aggregation-types/ endpoint', function () {

            $httpBackend.expect('GET', '/api/indicator-aggregation-types/').respond({
                route: 'Route'
            });

            // Need these two expects because the authenticate() call fires a
            // $rootScope.$broadcast('authService:loggedIn')
            // Apparently the app still loads in the background and logic in app.js
            // sends these web requests
            $httpBackend.expectGET('scripts/modules/auth/login-partial.html').respond(200);

            var aggregations = typesService.getIndicatorAggregationTypes();

            $httpBackend.flush();

            expect(aggregations).toBeDefined();
        });

        it('should hit the /api/sample-period-types/ endpoint', function () {

            $httpBackend.expect('GET', '/api/sample-period-types/').respond({
                morning: 'Morning'
            });

            // Need these two expects because the authenticate() call fires a
            // $rootScope.$broadcast('authService:loggedIn')
            // Apparently the app still loads in the background and logic in app.js
            // sends these web requests
            $httpBackend.expectGET('scripts/modules/auth/login-partial.html').respond(200);

            var periods = typesService.getSamplePeriodTypes();

            $httpBackend.flush();

            expect(periods).toBeDefined();
        });
    });
});
/*jshint camelcase: true */
