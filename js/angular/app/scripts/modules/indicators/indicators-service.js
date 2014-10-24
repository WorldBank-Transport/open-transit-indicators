'use strict';

angular.module('transitIndicators')
.factory('OTIIndicatorsService',
        ['$q', '$http', '$resource', 'OTIUploadService',
        function ($q, $http, $resource, OTIUploadService) {

    var otiIndicatorsService = {};
    var nullVersion = 0;
    otiIndicatorsService.selfCityName = null;

    otiIndicatorsService.Indicator = $resource('/api/indicators/:id/ ', {id: '@id'}, {
        'update': {
            method: 'PATCH',
            url: '/api/indicators/:id/ '
        }
    });

    /**
     * Resource for indicator jobs
     */
    otiIndicatorsService.IndicatorJob = $resource('/api/indicator-jobs/:id/ ', {id: '@id'}, {
        search: {
            method: 'GET',
            isArray: true,
            url: '/api/indicator-jobs/'
        }
    });

    /**
     * This is here rather than as a 'search' method on Indicator because the function refused to
     *  call the success/failure callbacks on function return as detailed here:
     *  HTTP GET "class" actions: Resource.action([parameters], [success], [error])
     * My guess is the redirect from the stripTrailingSlash is at work again...
     * TODO: Filter out route aggregations in request
     */
    otiIndicatorsService.query = function (method, config) {
        var dfd = $q.defer();
        $http({
            method: method,
            url: '/api/indicators/',
            params: config
        }).success(function (data) {
            dfd.resolve(data);
        }).error(function (error) {
            console.error('otiIndicatorsService.search():', error);
            dfd.resolve([]);
        });
        return dfd.promise;
    };

    /**
     * Thin wrapper for Indicator used in the controller for setting the map properties
     */
    otiIndicatorsService.IndicatorConfig = function (config) {
        this.version = config.version || nullVersion;
        this.type = config.type;
        this.sample_period = config.sample_period;
        this.aggregation = config.aggregation;
    };

    /**
     * Create windshaft urls for leaflet map
     *
     * @param indicator: otiIndicatorsService.Indicator instance
     * @param filetype: String, either png or utfgrid
     */
    otiIndicatorsService.getIndicatorUrl = function (filetype) {
        var url = otiIndicatorsService.getWindshaftHost();
        url += '/tiles/transit_indicators/{version}/{type}/{sample_period}/{aggregation}' +
               '/{z}/{x}/{y}';
        url += (filetype === 'utfgrid') ? '.grid.json?interactivity=value' : '.png';
        return url;
    };

    /**
     * Get the list of cities loaded into the app
     */
    otiIndicatorsService.getCities = function () {
        var dfd = $q.defer();
        $http.get('/api/indicator-cities/').success(function (data) {
            dfd.resolve(data.sort());
        }).error(function(error) {
            console.error('OTIIndicatorsService.getCities:', error);
            dfd.resolve([]);
        });
        return dfd.promise;
    };

    /**
     * Get the current indicator version
     *
     * @param callback: function to call after request is made, has a single argument 'version'
     */
    otiIndicatorsService.getIndicatorVersion = function (callback) {
        var promises = []; // get the city name before using it to filter indicator versions
        promises.push(OTIUploadService.cityName.get({}, function (data) {
            otiIndicatorsService.selfCityName = data.city_name;
        }));

        promises.push($http.get('/api/indicator-version/').success(function (data) {
            var version = nullVersion;
            if (data && data.current_versions && !_.isEmpty(data.current_versions)) {
                var versionObj = _.findWhere(data.current_versions, {version__city_name: otiIndicatorsService.selfCityName});
                if (versionObj) {
                    version = versionObj.version;
                }
            }
            callback(version);
        }).error(function (error) {
            console.error('getIndicatorVersion:', error);
            callback(nullVersion);
        }));

        $q.all(promises);
    };

    otiIndicatorsService.getIndicatorTypes = function () {
        var dfd = $q.defer();
        $http.get('/api/indicator-types/').success(function (data) {
            dfd.resolve(data);
        }).error(function (error) {
            console.error('OTIIndicatorService.getIndicatorTypes', error);
            dfd.resolve({});
        });
        return dfd.promise;
    };

    otiIndicatorsService.getIndicatorAggregationTypes = function () {
        var dfd = $q.defer();
        $http.get('/api/indicator-aggregation-types/').success(function (data) {
            dfd.resolve(data);
        }).error(function (error) {
            console.error('OTIIndicatorService.getIndicatorAggregationTypes', error);
            dfd.resolve({});
        });
        return dfd.promise;
    };

    otiIndicatorsService.getSamplePeriodTypes = function () {
        var dfd = $q.defer();
        $http.get('/api/sample-period-types/').success(function (data) {
            dfd.resolve(data);
        }).error(function (error) {
            console.error('OTIIndicatorService.getSamplePeriodTypes', error);
            dfd.resolve({});
        });
        return dfd.promise;
    };

    otiIndicatorsService.getRouteTypes = function () {
        var dfd = $q.defer();
        $http.get('/api/gtfs-route-types/').success(function (data) {
            dfd.resolve(data);
        }).error(function (error) {
            console.error('getRouteTypes Error: ', error);
            dfd.resolve([]);
        });
        return dfd.promise;
    };

    otiIndicatorsService.getIndicatorDescriptionTranslationKey = function(key) {
        return 'INDICATOR_DESCRIPTION.' + key;
    };

    return otiIndicatorsService;
}]);
