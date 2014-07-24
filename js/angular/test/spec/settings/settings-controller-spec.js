'use strict';

describe('OTISettingsController', function () {
    var settingsCtl;
    var scope;

    beforeEach(module('transitIndicators'));
    beforeEach(inject(function ($controller, $rootScope) {
        scope = $rootScope.$new();
        settingsCtl = $controller('OTISettingsController', {
            $scope: scope
        });
    }));

    it('should have a STATUS', function () {
        expect(scope.STATUS).toBeDefined();
    });

    it('should have checkmarks', function () {
        expect(scope.checkmarks).toBeDefined();
    });
});
