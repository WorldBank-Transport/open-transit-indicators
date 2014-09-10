'use strict';

describe('OTIRootController', function () {
    var rootCtl;
    var scope;
    var rootScope;
    var state;

    beforeEach(module('transitIndicators'));
    beforeEach(inject(function ($controller, $rootScope, $state) {
        rootScope = $rootScope;
        scope = $rootScope.$new();
        state = $state;
        rootCtl = $controller('OTIRootController', {
            $scope: scope
        });
    }));

    it('should have leafletDefaults', function () {
        expect(scope.leafletDefaults).toBeDefined();
    });

    it('should set $scope.activeState when $stateChangeSuccess is fired', function () {
        // Mock of the ui-router State object. We only care about it's name property
        var toState = {
            name: 'scenarios',
            parent: 'root'
        };
        scope.activeState = 'transit';
        rootScope.$broadcast('$stateChangeSuccess', toState);
        expect(scope.activeState).toEqual('scenarios');
    });

    it('should set activeState to indicators if toState object has parent attribute == indicators', function () {
        var toState = {
            name: 'data',
            parent: 'indicators'
        };
        scope.activeState = 'transit';
        rootScope.$broadcast('$stateChangeSuccess', toState);
        expect(scope.activeState).toEqual('indicators');
    });

    it('should set activeState to settings if toState object has parent attribute == settings', function () {
        var toState = {
            name: 'overview',
            parent: 'settings'
        };
        scope.activeState = 'transit';
        rootScope.$broadcast('$stateChangeSuccess', toState);
        expect(scope.activeState).toEqual('settings');
    });
});
