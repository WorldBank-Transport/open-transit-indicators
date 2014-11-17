'use strict';

angular.module('transitIndicators')
.factory('OTIIndicatorJobManager', ['$http', '$q', '$rootScope', 'authService', 'OTISettingsService',
        function ($http, $q, $rootScope, authService, OTISettingsService) {

    var _currentCityName = '';
    var _nullJob = 0;

    var module = {};

    module.Events = {
        JobUpdated: 'OTI:IndicatorJobManager:Updated'
    };

    module.getCurrentCity = function () {
        return _currentCityName;
    };

    module.getJobs = function (userId) {
        var deferred = $q.defer();

        if(!userId) {
            userId = authService.getUserId();
        }

        $http.get('/api/indicator-jobs/', {
            created_by: userId
        }).then(function (result) {
            deferred.resolve(_.sortBy(result.data, function(job) { return job.id; }));
        }, function () {
            deferred.reject([]);
        });

        return deferred.promise;
    };

    module.getCurrentJob = function (callback) {
        var promises = []; // get the city name before using it to filter indicator CalcJobs
        promises.push(OTISettingsService.cityName.get({}, function (data) {
            _currentCityName = data.city_name;
        }));

        promises.push($http.get('/api/indicator-calculation-job/').success(function () {
        }).error(function (error) {
            console.error('getIndicatorCalcJob:', error);
            callback(_nullJob);
        }));

        $q.all(promises).then(function (data) {
            var job = _nullJob;
            // flatter is better - the following (very long) line just ensures the existence of
            // certain nodes which are operated on in the flow
            if (data && data[1] && data[1].data && data[1].data.current_jobs &&
                !_.isEmpty(data[1].data.current_jobs) &&
                _.findWhere(data[1].data.current_jobs, {calculation_job__city_name: _currentCityName})) {
                var jobs = data[1].data;
                var jobObj = _.findWhere(jobs.current_jobs, {calculation_job__city_name: _currentCityName});
                job = jobObj.calculation_job;
                $rootScope.$broadcast(module.Events.JobUpdated, job);
            }
            callback(job);
        }, function (error) {
            console.log('otiIndicatorsService.getIndicatorCalcJob error:');
            console.log(error);
        });
    };

    return module;
}]);
