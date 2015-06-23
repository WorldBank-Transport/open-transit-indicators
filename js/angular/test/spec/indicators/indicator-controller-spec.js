'use strict';

/*jshint camelcase: false */
describe('OTIIndicatorsController', function () {
    var indicatorsCtl;
    var scope;
    var rootScope;
    var state;

    beforeEach(module('transitIndicators'));
    beforeEach(inject(function ($controller, $rootScope, $state) {
        rootScope = $rootScope;
        scope = $rootScope.$new();
        state = $state;
        indicatorsCtl = $controller('OTIIndicatorsController', {
            $scope: scope,
            cities: []
        });
    }));

    it('should have aggregations properties', function () {
        expect(scope.aggregations).toBeDefined();
    });

    it('should have types properties', function () {
        expect(scope.types).toBeDefined();
    });

    it('should have sample_periods properties', function () {
        expect(scope.sample_periods).toBeDefined();
    });

    it('should set $scope.mapActive when $stateChangeSuccess is fired', function () {
        var toState = {
            name: 'map',
            parent: 'indicators'
        };
        scope.mapActive = false;
        rootScope.$broadcast('$stateChangeSuccess', toState);
        expect(scope.showingState).toEqual('map');
    });

});
/*jshint camelcase: true */
