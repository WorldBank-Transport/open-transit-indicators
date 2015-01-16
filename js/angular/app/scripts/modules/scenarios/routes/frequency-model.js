'use strict';

angular.module('transitIndicators')
.factory('OTIFrequencyModel', [
        function () {

    function OTIFrequencyModel(frequency) {
        var freq = angular.extend({}, frequency);
        this.start = freq.start || '00:00:00';
        this.end = freq.end || '00:00:00';
        this.headway = freq.headway || 0;
    }

    return OTIFrequencyModel;
}]);