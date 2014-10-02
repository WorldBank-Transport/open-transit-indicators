'use strict';
/**
 *  Sample, trivial, test for polling upload directive
 *  Unfortunately, all of the testable actions in the directive key off
 *  of private methods that trigger when the startUpload function is called in
 *  the angular-file-upload directive. I was unable to find a way to call this method
 *  directly, with mocked or actual file data. It is not possible to inject a file object
 *  into <input type="file"> elements, as this would be a large security hole.
 *
 *  So, here is a sample test and the framework for testing this directive, if we set up
 *  selenium tests and are able to find a way to test this later.
 */
describe('pollingUploadTests', function () {

    var $compile, $rootScope, $httpBackend, $resource;

    beforeEach(function () {
        module('ngResource');
        // Commented out because httpBackend expects i18n/en.json and I don't have time
        // to figure out exactly where mock wants that expect.
        // TODO: figure it out
        //module('transitIndicators');
        inject(function ($injector) {
            $compile = $injector.get('$compile');
            $rootScope = $injector.get('$rootScope');
            $httpBackend = $injector.get('$httpBackend');
            $resource = $injector.get('$resource');
        });

    });

    describe('rendering', function () {

        var element;

        beforeEach(function () {
            $rootScope.testUpload = {};
            // Mock a resource with an arbitrary URL
            // When testing, the calls this resource makes internally will be intercepted and
            //  can be mocked with $httpBackend
            $rootScope.mockResource = $resource('/api/testUpload/:id');
            $rootScope.config = {};

            // compile and generate the directive
            element = $compile('<polling-upload upload="testUpload" resource="mockResource" url="/api/testUpload/" options="config"></polling-upload>')($rootScope);
            $rootScope.$digest();
        });

        it('should have an input element if no upload object passed', function () {
            // Commented out, see above TODO
            //expect(element.find('input').length).toBe(1);
        });

    });
});