'use strict';

angular.module('transitIndicators')
.factory('OTIUploadStatus',
         ['$http', '$q',
         function ($http, $q) {

    var otiUploadStatus = {};

    otiUploadStatus.get = function () {
        var dfd = $q.defer();
        $http.get('/api/upload-statuses/').success(function (data) {
            dfd.resolve(data);
        }).error(function (error) {
            console.error('OTISettingsService.getUploadStatuses:', error);
            dfd.resolve({});
        });
        return dfd.promise;
    };

    otiUploadStatus.STRINGS = {};
    otiUploadStatus.get().then(function (data) {
        otiUploadStatus.STRINGS = data;
    });

    var PENDING = 'pending';
    var UPLOADING = 'uploading';
    var PROCESSING = 'processing';
    var DOWNLOADING = 'downloading';
    var VALIDATING = 'validating';
    var IMPORTING = 'importing';
    var COMPLETE = 'complete';
    var WARNING = 'warning';
    var ERROR = 'error';
    var WAITING_INPUT = 'waiting_input';

    otiUploadStatus.PENDING = PENDING;
    otiUploadStatus.PROCESSING = PROCESSING;
    otiUploadStatus.IMPORTING = IMPORTING;
    otiUploadStatus.WAITING_INPUT = WAITING_INPUT;
    otiUploadStatus.COMPLETE = COMPLETE;
    otiUploadStatus.ERROR = ERROR;

    otiUploadStatus.isPolling = function (status) {
        return _.indexOf([UPLOADING, PENDING, PROCESSING, DOWNLOADING, VALIDATING, IMPORTING], status) >= 0;
    };

    otiUploadStatus.isComplete = function (status) {
        return _.indexOf([COMPLETE, WARNING], status) >= 0;
    };

    otiUploadStatus.isError = function (status) {
        return status === ERROR;
    };

    otiUploadStatus.isWaiting = function (status) {
        return status === WAITING_INPUT;
    };

    return otiUploadStatus;
}]);