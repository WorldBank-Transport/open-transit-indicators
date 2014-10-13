'use strict';

/* global L */

angular.module('transitIndicators')
.factory('OTIDrawService',
        ['leafletData',
        function (leafletData) {

    var drawService = {};

    drawService.drawnItems = new L.FeatureGroup();
    drawService.isScenarioLayer = false;

    drawService.reset = function () {
        leafletData.getMap().then(function (map) {
            map.removeLayer(drawService.drawnItems);
            drawService.drawnItems = new L.FeatureGroup();
            drawService.isScenarioLayer = false;
            drawService.markerController.reset();
        });
    };

    drawService.markerController = {
        markerCount: 1,
        count: function () {
            return this.markerCount;
        },
        increment: function () {
            this.markerCount++;
        },
        reset: function () {
            this.markerCount = 1;
        }
    };

    drawService.getCircleIcon = function () {
        return L.divIcon({
            className: 'count-icon',
            html: 'X',
            iconSize: [30, 30]
        });
    };

    drawService.markerDrawControl = new L.Control.Draw({
        position: 'topright',
        draw: {
            polyline: false,
            polygon: false,
            rectangle: false,
            circle: false,
            marker: {
                icon: drawService.getCircleIcon()
            }
        }
    });

    drawService.lineDrawControl = new L.Control.Draw({
        position: 'topright',
        draw: {
            polyline: {
                metric: true,
                shapeOptions: {
                    stroke: true,
                    color: '#41bcff',
                    weight: 5,
                    opacity: 0.9,
                    fill: false
                }
            },
            polygon: false,
            rectangle: false,
            circle: false,
            marker: false
        },
        edit: {
            featureGroup: drawService.drawnItems,
            remove: false,
            edit: false
        }
    });

    return drawService;
}]);