'use strict';
/**

A directive to allow jquery code to be run from the prototype html

*/

angular.module('transitIndicators')
.directive('scenariosPrototype',
    ['leafletData',
    function(leafletData) {

        return {
            restrict: 'AE',
            scope: {},
            controller: ['$scope',
                function($scope) {
                    $scope.pageno = 0;

                    $scope.next = function() {
                        $scope.pageno++;
                    };

                    $scope.prev = function() {
                        $scope.pageno--;
                    };

                }
            ],
            templateUrl: 'scripts/modules/scenarios/scenarios-prototype-partial.html',
            link: function(scope) {
                leafletData.getMap().then(function(map) {

                    var height;

                    var date = new Date(),
                        day = date.getDate(),
                        month = date.getMonth(),
                        year = date.getFullYear();
                    $('#prototype-new-date').html(month + '/' + day + '/' +
                        year);
                    var Pages = {
                        init: function() {
                            this.setPagesHeight();
                            this.activePageFocus();
                        },
                        activePage: function() {
                            return $('.scenario-page').filter(
                                '.active');
                        },
                        activeHeader: function() {
                            return $('.scenario-headings');
                        },
                        setPagesHeight: function() {
                            var pageHeight = $(this.activePage()).outerHeight(),
                                headerHeight = $(this.activeHeader())
                                .outerHeight();
                            height = pageHeight + headerHeight;
                            $('.scenario').css({
                                'height': (height)
                            });
                        },
                        activePageFocus: function() {
                            $(this.activePage()).find('input').first()
                                .focus();
                        },
                        setNeighborClasses: function(activePage) {
                            $(activePage).prevAll().addClass(
                                'previous').removeClass('next');
                            $(activePage).nextAll().removeClass(
                                'previous').addClass('next');
                        },
                        close: function(element) {
                            $(element).removeClass('active');
                        },
                        open: function(element) {
                            $(element).addClass('active');
                        },
                        to: function(element) {
                            this.close(this.activePage());
                            this.open(element);
                            this.setNeighborClasses(this.activePage());
                            this.activePageFocus();
                            this.setPagesHeight();
                        },
                    };
                    Pages.init();
                    $('a').on('click', function(e) {
                        e.preventDefault();
                        $('.leaflet-draw-draw-marker').hide();
                        $('.leaflet-draw-draw-polyline').hide();
                        var target = $(this).attr('href');
                        if (target === '#confirm-times') {
                            getTimeTableHtml();
                        }
                        if (target === '#new-scenario') {
                            $('#heading-scenario').removeClass(
                                'active');
                            $('#heading-route').removeClass(
                                'active');
                            $('.scenario').removeClass(
                                'focus--scenario').removeClass(
                                'focus--route');
                            markerController.resetCount();
                            drawControl.options.draw.marker.icon.options
                                .html = markerController.getMarkerCount();
                            map.removeLayer(drawnItems);
                            drawnItems = new L.FeatureGroup();
                            map.addLayer(drawnItems);
                        }
                        if (target === '#my-scenario') {
                            setScenarioName();
                            $('#heading-scenario').addClass(
                                'active');
                            $('#heading-route').removeClass(
                                'active');
                            $('.scenario').addClass(
                                'focus--scenario').removeClass('focus--route');
                        }
                        if (target === '#new-route') {
                            $('#heading-route').removeClass(
                                'active');
                            $('.scenario').removeClass(
                                'focus--route').addClass(
                                'focus--scenario');
                        }
                        if (target === '#add-stops') {
                            setRouteName();
                            $('.leaflet-draw-draw-marker').show();
                            $('#heading-route').addClass('active');
                            $('.scenario').removeClass(
                                'focus--scenario').addClass(
                                'focus--route');
                        }
                        if (target === '#add-shape') {
                            setRouteName();
                            $('.leaflet-draw-draw-polyline').show();
                            $('#heading-route').addClass('active');
                            $('.scenario').removeClass(
                                'focus--scenario').addClass(
                                'focus--route');
                        }
                        Pages.to(target);
                    });
                    var drawnItems = new L.FeatureGroup();
                    var markerController = {
                        markerCount: 1,
                        increaseMarkerCount: function() {
                            this.markerCount++;
                        },
                        getMarkerCount: function() {
                            return this.markerCount;
                        },
                        resetCount: function() {
                            this.markerCount = 1;
                        }
                    };
                    map.addLayer(drawnItems);
                    var circleIcon = L.divIcon({
                        className: 'count-icon',
                        html: 1,
                        iconSize: [30, 30]
                    });

                    function getTimeTableRow(value) {
                        var minute = 14 + 3 * value;
                        var timeTemplate = '<tr>' +
                            '<td><span class="badge">' + value +
                            '</span></td>' + '<td>Stop ' + value +
                            '</td>' +
                            '<td><input type="text" value="15:' +
                            minute + '" class="time"></td>' + '</tr>';
                        return timeTemplate;
                    }

                    function getTimeTableHtml() {
                        var number = (markerController.getMarkerCount() >
                                1) ? markerController.getMarkerCount() :
                            5,
                            timeTableRowHtml = '';
                        for (i = 1; i < number; i++) {
                            timeTableRowHtml += getTimeTableRow(i);
                        }
                        $('#prototype-time-table').html(
                            timeTableRowHtml);
                    }

                    function setScenarioName() {
                        var scenarioName = ($(
                                '#prototype-scenario-field').val()) ? $(
                                '#prototype-scenario-field').val() :
                            'New Scenario';
                        $('.prototype-scenario-text').html(scenarioName);
                    }

                    function setRouteName() {
                        var routeName = ($('#prototype-route-field').val()) ?
                            $('#prototype-route-field').val() :
                            'New Route';
                        $('.prototype-route-text').html(routeName);
                    }

                    function htmlIconTemplate(value) {
                        return '<div class="bount-icon" style="background-color: #41bcff">' +
                            value + '<div>';
                    }
                    var drawControl = new L.Control.Draw({
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
                                },
                            },
                            polygon: false,
                            rectangle: false,
                            circle: false,
                            marker: {
                                icon: circleIcon,
                            }
                        },
                        edit: {
                            featureGroup: drawnItems,
                            remove: false,
                            edit: false
                        }
                    });
                    map.addControl(drawControl);
                    (function(e) {
                        var type = e.layerType,
                            layer = e.layer;
                        if (type === 'marker') {
                            markerController.increaseMarkerCount();
                        }
                        drawnItems.addLayer(layer);
                        drawControl.options.draw.marker.icon.options
                            .html = markerController.getMarkerCount();
                    })(map);
                    map.on('draw:edited', function(e) {
                        var layers = e.layers;
                        var countOfEditedLayers = 0;
                        layers.eachLayer(function(layer) {
                            countOfEditedLayers++;
                        });
                    });
                    $('.leaflet-draw-draw-marker').hide();
                    $('.leaflet-draw-draw-polyline').hide();


                });
            }
        };
    }
]);
