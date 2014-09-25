/**

Async Polling Upload directive
Run code after a 201 created upload completes some async task

Upload param must be a $resource object, defined with a unique 'id' field
The default state for this directive is displaying an "upload file" div

Dependencies:
https://github.com/danialfarid/angular-file-upload

Required Params:
  - url:        URL to post the file upload to. This will likely be the same as the URL contained
                within the resource param.
  - upload:     upload $resource instance. If a valid upload $resource, the directive will indicate to
                the user that the $resource is already uploaded, and provided the option to delete it
                and re-upload. If null, the upload dialog will be displayed instead, and this param
                will populate with the uploaded $resource object once the user uploads a file.
  - resource:   The $resource object that creates instances of upload.

Options:
  - fileFormName:       POST parameter for the file name
  - pollingTimeoutMs:   Time in milliseconds between upload status queries
  - uploadTimeoutMs:    Time in milliseconds before timing out the job and calling the error handler
  - checkInvalid:       function (object) should return true if the object is invalid and processing
                        should be cancelled
  - checkContinue:      function (object) should return true if the object is still processing

Events:
$rootScope:                         (event args)
  - pollingUpload:uploadStarted     ()
  - pollingUpload:uploadFinished    (upload)
  - pollingUpload:uploadError       (error, status)
  - pollingUpload:uploadDelete      (upload)
  - pollingUpload:uploadCancel      (upload)
  - pollingUpload:pollingStarted    (upload)
  - pollingUpload:pollingFinished   (upload)
  - pollingUpload:pollingError      (upload, error, status)
  - pollingUpload:processingError   (upload, error)

*/

(function ( angular ) {
    'use strict';
    angular.module('transitIndicators')
    .directive('pollingUpload',
                               ['$http', '$rootScope', '$upload', '$timeout', 'OTIUploadStatus',
                                function ($http, $rootScope, $upload, $timeout, OTIUploadStatus) {

        var events = {
            uploadStarted: 'pollingUpload:uploadStarted',
            uploadFinished: 'pollingUpload:uploadFinished',
            uploadError: 'pollingUpload:uploadError',
            uploadCancel: 'pollingUpload:uploadCancel',
            uploadDelete: 'pollingUpload:uploadDelete',
            pollingStarted: 'pollingUpload:pollingStarted',
            pollingFinished: 'pollingUpload:pollingFinished',
            pollingError: 'pollingUpload:pollingError',
            processingError: 'pollingUpload:processingError'
        };

        // TODO: Make configurable templateUrl and add custom stylesheet
        var template = [
            '<div class="dropzone" ng-show="!upload.status && !uploadError"> ',
            '  <div class="h4" ng-file-drop="startUpload($files)" ng-file-drop-available="true">Drop file here or </div>',
            '  <input type="file" ng-file-select="startUpload($files)" />',
            '</div>',
            '<div class="dropzone inprogress" ng-show="Status.isPolling(upload.status)">',
            '  <div class="h4">Uploading your file... <span class="h5"><a ng-click="cancel()">Cancel</a></span></div>',
            '  <progressbar value="uploadProgress" class="progress-striped active" max=100><i>{{ Status.STRINGS[upload.status] }}</i></progressbar>',
            '</div>',
            '<div class="dropzone" ng-show="Status.isComplete(upload.status)">',
            '  <div class="h3">',
            '    <span class="glyphicon glyphicon-ok"></span> Data Loaded',
            '    <span class="h5 pull-right"><button class="btn btn-danger" ng-click="delete()">Delete Data</button></span>',
            '  </div>',
            '</div>',
            '<div class="dropzone notices" ng-show="Status.isError(upload.status) || uploadError">',
            '  <div class="h4">',
            '    <span class="glyphicon glyphicon-remove"></span> Upload Failed: {{ uploadError }}',
            '    <span class="h5"><a ng-click="cancel()">Try Again</a></span>',
            '  </div>',
            '</div>'
        ].join('');

        var ensureDefault = function (obj, property, value) {
            if (!obj.hasOwnProperty(property)) {
                    obj[property] = value;
            }
        };

        var defaultCheckInvalid = function (upload) {
            return OTIUploadStatus.isError(upload.status);
        };

        var defaultCheckContinue = function (upload) {
            return OTIUploadStatus.isPolling(upload.status);
        };

        return {
            restrict: 'E',
            scope: {
                url: '@',
                upload: '=',
                resource: '=',
                options: '=?'
            },
            template: template,
            link: function (scope) {

                scope.options = scope.options || {};
                ensureDefault(scope.options, 'pollingTimeoutMs', 3 * 1000);
                ensureDefault(scope.options, 'uploadTimeoutMs', 60 * 1000);
                ensureDefault(scope.options, 'checkInvalid', defaultCheckInvalid);
                ensureDefault(scope.options, 'checkContinue', defaultCheckContinue);
                ensureDefault(scope.options, 'fileFormName', 'source_file');

                scope.Status = OTIUploadStatus;

                /**
                 *  Clear UI by resetting to blank state
                 */
                var clearUploadProblems = function () {
                    scope.uploadError = null;
                };

                /**
                 * Set error message in UI
                 */
                var setUploadError = function (msg) {
                    if (msg) {
                        scope.uploadError = msg;
                    }
                };

                /**
                 *  Recursive poll for job completion
                 *  Determines completion based on return values from:
                 *      checkInvalid, checkContinue
                 *  Updates scope.upload anytime an http request returns an instance
                 *      of our resource object
                 */
                var pollForComplete = function () {
                    var UPLOAD_TIMEOUT_MS = scope.options.uploadTimeoutMs;
                    var POLLING_TIMEOUT_MS = scope.options.pollingTimeoutMs;
                    var startDatetime = new Date();
                    var checkUpload = function () {
                        var nowDatetime = new Date();
                        var err = '';
                        if (nowDatetime.getTime() - startDatetime.getTime() > UPLOAD_TIMEOUT_MS) {
                            err = 'Upload Timeout';
                            setUploadError(err);
                            $rootScope.$broadcast(events.pollingError, scope.upload, { error: err });
                        } else if (scope.options.checkContinue(scope.upload)) {
                            scope.timeoutId = $timeout(function () {
                                scope.resource.get({id: scope.upload.id}, function (data) {
                                    scope.upload = data;
                                    checkUpload();
                                }, function () {
                                    // Ignore errors here and reschedule check, will eventually timeout
                                    checkUpload();
                                });
                            }, POLLING_TIMEOUT_MS);
                        } else if (scope.options.checkInvalid(scope.upload)) {
                            err = 'Upload processing failed';
                            setUploadError(err);
                            $rootScope.$broadcast(events.processingError, scope.upload, { error: err });
                        } else {
                            scope.resource.get({id: scope.upload.id}, function (data) {
                                scope.upload = data;
                                if (OTIUploadStatus.isComplete(data.status)) {
                                    $rootScope.$broadcast(events.pollingFinished, data);
                                } else {
                                    setUploadError('Error processing upload.');
                                    $rootScope.$broadcast(events.pollingError, scope.upload, data, status);
                                }
                            }, function (data, status) {
                                setUploadError('Unable to verify upload.');
                                $rootScope.$broadcast(events.pollingError, scope.upload, data, status);
                            });
                        }
                    };
                    checkUpload();
                    $rootScope.$broadcast(events.pollingStarted, scope.upload);
                };

                /**
                 *  Kicks off an upload, setting scope.upload to the created resource
                 *      on success
                 *
                 *  @param $files: The array of files to upload, directly copied from
                 *                 the angular-file-upload directive
                 */
                scope.startUpload = function ($files) {
                    if (!($files && $files[0])) {
                        return;
                    }
                    var $file = $files[0];
                    var data = {};
                    data[scope.options.fileFormName] = $file.name;
                    scope.uploadProgress = 0;
                    $upload.upload({
                        url: scope.url,
                        method: 'POST',
                        data: data,
                        fileFormDataName: scope.options.fileFormName,
                        file: $file
                    }).progress(function (evt) {
                        scope.uploadProgress = parseInt(100 * evt.loaded / evt.total, 10);
                    }).success(function (data) {
                        scope.upload = data;
                        scope.uploadProgress = 100;
                        $rootScope.$broadcast(events.uploadFinished, data);
                        pollForComplete();
                    }).error(function (data, status) {
                        setUploadError(JSON.stringify(data));
                        $rootScope.$broadcast(events.uploadError, data, status);
                    });

                    $rootScope.$broadcast(events.uploadStarted);
                };

                /**
                 *  Cancels an in progress upload, resetting the UI to its default state
                 */
                scope.cancel = function () {
                    $timeout.cancel(scope.timeoutId);
                    $rootScope.$broadcast(events.uploadCancel, scope.upload);
                    clearUploadProblems();
                    scope.upload = null;
                };

                /**
                 *  Deletes an upload, resetting the UI to its default state
                 */
                scope.delete = function () {
                    scope.resource.delete({id: scope.upload.id}, function () {
                        clearUploadProblems();
                        $rootScope.$broadcast(events.uploadDelete);
                        scope.upload = null;
                    });
                };

                if (!scope.upload.status) {
                    clearUploadProblems();
                }

                scope.$watch('upload', function (newValue, oldValue) {
                    if (!(newValue && oldValue)) {
                        return;
                    }
                    var newPolling = OTIUploadStatus.isPolling(newValue.status);
                    var oldPolling = OTIUploadStatus.isPolling(oldValue.status);
                    if (newPolling && !oldPolling && !scope.timeoutId) {
                        scope.uploadProgress = 100;
                        pollForComplete();
                    }
                });
            }
        };
    }]);
})( angular );