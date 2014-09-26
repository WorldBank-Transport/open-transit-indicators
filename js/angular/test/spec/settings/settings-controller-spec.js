'use strict';

describe('OTISettingsController', function () {
    var settingsCtl;
    var scope;
    var rootScope;
    var state;

    beforeEach(module('transitIndicators'));
    beforeEach(inject(function ($controller, $rootScope, $state) {
        rootScope = $rootScope;
        scope = $rootScope.$new();
        state = $state;
        settingsCtl = $controller('OTISettingsController', {
            $scope: scope
        });
    }));

    it('should have checkmarks', function () {
        expect(scope.checkmarks).toBeDefined();
    });

    it('should set $scope.activeView when $stateChangeSuccess is fired', function () {
        // Mock of the ui-router State object. We only care about it's name property
        var toState = {
            name: 'overview'
        };
        scope.activeView = 'upload';
        rootScope.$broadcast('$stateChangeSuccess', toState);
        expect(scope.activeView).toEqual('overview');
    });
});
