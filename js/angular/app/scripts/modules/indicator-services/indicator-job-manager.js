'use strict';

angular.module('transitIndicators')
.factory('OTIIndicatorJobManager', ['$cookieStore', '$http', '$q', '$rootScope', 'OTISettingsService',
        function ($cookieStore, $http, $q, $rootScope, OTISettingsService) {

    var COOKIE_STORE_LOADED_SCENARIOS = 'OTIIndicatorJobManager:LoadedScenarios';

    var _currentCityName = '';
    var _nullJob = 0;

    var _loadedScenarios = $cookieStore.get(COOKIE_STORE_LOADED_SCENARIOS) || [];

    var module = {};

    module.Events = {
        JobUpdated: 'OTI:IndicatorJobManager:Updated'
    };

    module.getCurrentCity = function () {
        return _currentCityName;
    };

    module.getLoadedScenarios = function () {
        return _loadedScenarios;
    };

    module.setLoadedScenarios = function (scenarios) { // persist loaded scenarios on indicators page
        _loadedScenarios = scenarios;
        $cookieStore.put(COOKIE_STORE_LOADED_SCENARIOS, _loadedScenarios);
    };

    module.isLoaded = function (scenarionum) {
        var val =  !!(_.find(module.getLoadedScenarios(),
                    function(num) { return scenarionum === num; }));
        return val;
    };

    module.getJobs = function (userId) {
        var deferred = $q.defer();

        $http.get('/api/indicator-jobs/', {
            params: {
                created_by: userId
            }
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
