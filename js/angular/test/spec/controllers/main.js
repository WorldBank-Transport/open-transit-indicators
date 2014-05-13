'use strict';

describe('OTISettingsController', function () {

  // load the controller's module
  beforeEach(module('transitIndicators'));

  var settingsCtl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    settingsCtl = $controller('OTISettingsController', {
      $scope: scope
    });
  }));

  it('settings controller should have a STATUS', function () {
    expect(scope.STATUS).toBeDefined();
  });
  it('settings controller should have checkmarks', function () {
    expect(scope.checkmarks).toBeDefined();
  });
  // test that a test will fail
  it('setting controller should have unicorns!', function() {
    expect(scope.UNICORNS).toBeDefined();
  });
});
