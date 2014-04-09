'use strict';

angular.module('angularApp')
.controller('GTFSUploadController',
    ['$scope', '$upload',
    function ($scope, $upload) {
        $scope.metadata = {};
        
        $scope.files = null;
        $scope.onFileSelect = function($files) {
            // $files: an array of selected files, each with name, size, and type.
            $scope.files = $files;
            $scope.metadata.source_file = $files[0].name;
        };

        $scope.upload = function () {
            console.log("Uploading file...");
            if (!$scope.files) {
                return;
            }
            // TODO: Status message here
            var file = $scope.files[0];
            $scope.upload = $upload.upload({
                url: '/api/gtfs-feeds/',
                method: 'POST',
                data: $scope.metadata,
                fileFormDataName: 'source_file',
                file: file
            }); // TODO: .success() and .error() here.
            console.log("Uploaded!");
        };
}]);
