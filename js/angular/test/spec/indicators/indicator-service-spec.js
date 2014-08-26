'use strict';

describe('Service: Indicators', function () {

    var indicatorsService;

    beforeEach(module('transitIndicators'));
    beforeEach(inject(function(_OTIIndicatorsService_) {
        indicatorsService = _OTIIndicatorsService_;
    }));

    describe('IndicatorsConfig tests', function () {
        it('should have version, sample_period, type, aggregation properties', function () {
            var indicator = new indicatorsService.IndicatorConfig({
                version: 0,
                type: 'test',
                aggregation: 'system',
                sample_period: 'morning'
            });
            expect(indicator.version).toBeDefined();
            expect(indicator.type).toBeDefined();
            expect(indicator.aggregation).toBeDefined();
            expect(indicator.sample_period).toBeDefined();
        });

        it('should fail to initialize with an empty object', function () {
            var indicator = new indicatorsService.IndicatorConfig({});
            expect(indicator.version).toBe(0);
            expect(indicator.type).not.toBeDefined();
            expect(indicator.aggregation).not.toBeDefined();
            expect(indicator.sample_period).not.toBeDefined();
        });
    });

    describe('getIndicatorTypes tests', function () {
        var $httpBackend;

        beforeEach(inject(function(_$httpBackend_) {
            $httpBackend = _$httpBackend_;
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

            var types = indicatorsService.getIndicatorTypes();

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

            var aggregations = indicatorsService.getIndicatorAggregationTypes();

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

            var periods = indicatorsService.getSamplePeriodTypes();

            $httpBackend.flush();

            expect(periods).toBeDefined();
        });
    });
});
