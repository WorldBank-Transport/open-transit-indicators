/*
 * L.TileLayer.WMS is used for putting WMS tile layers on the map.
 */


L.animatedTiles = {

};

L.activeTiles = [];

//};

var hexToRgb = function(hex) {
    var result = /^0x?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    var result = [ parseInt(result[1],16), parseInt(result[2],16), parseInt(result[3],16) ];
    return result;
}
var colorStrings = ["0xF68481","0xFDB383","0xFEE085",
                    "0xDCF288","0xB6F2AE","0x98FEE6","0x83D9FD",
                    "0x81A8FC","0x8083F7","0x7F81BD"];
var colorArray = _.map(colorStrings, hexToRgb);

var dataBreaks = [10,15,20,30,40,50,60,75,90,120];
var breakLength = dataBreaks.length;

// green and blue channels to seconds
// red is unused because only needed for values > 18 hrs
var gbToSeconds = function(g,b) {
    var time = (g * (255)) + b
    return time
}


L.TileLayer.WMS2 = L.TileLayer.Canvas.extend({
    defaultWmsParams: {
	service: 'WMS',
	request: 'GetMap',
	version: '1.1.1',
	layers: '',
	styles: '',
	format: 'image/jpeg',
	transparent: false,
	width: 256,
	height: 256
    },
    
    _reset: function (e) {
	var i;
	var activeLength = L.activeTiles.length;
	for(i = 0; i < activeLength; i ++) {
	    var tile = L.activeTiles[i];
	    var x = tile.x; 
	    var y = tile.y;
	    var z = -1;

	    L.animatedTiles[x][y][z].active = false;
	}
	L.activeTiles = [];
	L.TileLayer.Canvas.prototype._reset.call(this);
    },
    

    initialize: function (url, options) { // (String, Object)
	this._url = url;
	
	var wmsParams = L.extend({}, this.defaultWmsParams),
	tileSize = options.tileSize || this.options.tileSize;
        this.getValue = options.getValue;

	for (var i in options) {
	    // all keys that are not TileLayer options go to WMS paardrms
	    if (!this.options.hasOwnProperty(i) && i !== 'crs') {
		wmsParams[i] = options[i];
	    }
	}
	
	this.wmsParams = wmsParams;
	
	L.setOptions(this, options);
    },
    _createTile: function () {
        var tile = L.DomUtil.create('canvas', 'leaflet-tile');
        tile.width = tile.height = this.options.tileSize;
        tile.onselectstart = tile.onmousemove = L.Util.falseFn;
	tile.layer = this;

	var id = Math.floor((Math.random()*1000)+1);
	tile.id = id;
        return tile;
    },

    _unloadTile : function(tileInfoz) {
	this.layer.off("tileunload", this._unloadTile, this);
	var tilePoint = tileInfoz.tile._tilePoint;
	var x = tilePoint.x;
	var y = tilePoint.y;
	var z = -1;
	if (L.animatedTiles[x][y][z] == null) {
	    return;
	}
	var tile = L.animatedTiles[x][y][z];

	if (tile.active) {
	    var index = L.activeTiles.indexOf(tile) > -1;
	    if (index > -1) {
		L.activeTiles.splice(index,1);
	    }
	}
	L.animatedTiles[x][y][z] = null;
    },

    drawTile: function(canvas, tilePoint) {
	var tileInfo;

	var id = Math.floor((Math.random()*1000)+1);
	this._adjustTilePoint(tilePoint);
	var dataUrl = this.getTileUrl(tilePoint);

	var x = tilePoint.x;
	var y = tilePoint.y;

	//TODO: set zoom properly
	var z = -1;

	var setup = true;
	if (L.animatedTiles[x] == null) {
	    L.animatedTiles[x] = {};
	}
	if (L.animatedTiles[x][y] == null) {
	    L.animatedTiles[x][y] = {};
	} 
	if (L.animatedTiles[x][y][z] == null) {
	    L.animatedTiles[x][y][z] = { 
		setup: false,
		active: false,
		dataUrl: dataUrl,
	        x: x,
	        y: y,
	        z: z,
		id: id
	    };
	    setup = true;
	} else {
	    tileInfo = L.animatedTiles[x][y][z];
	    if (dataUrl == tileInfo.dataUrl && tileInfo.setup == false) { 
		//tileInfo.setup = true;
		//setup = false;
	    } 
	}

	// If there is an active animation for this tile (e.g. redraw() has been
	// called), update the stored canvas element with the current canvas element.
	var ctx = canvas.getContext('2d');
	tileInfo =  L.animatedTiles[x][y][z];
	L.animatedTiles[x][y][z]['ctx'] = ctx;

	if (! setup) {
	    return;
	}
	
	var ts = this.options.tileSize;

	var _this = this;
	var zoom = -1;
	if (this.map != null) {
	    var zoom = this.map.zoom;
	}

	var imageObj = new Image();
	var dataObj = new Image();

	var loadedCount = 0;
	var data;
       
        var getValue = this.getValue;
	
	dataObj.onload = function() {	
	    // create canvas to get data pixel value array
	    var dataCanvas = document.createElement("canvas");
	    var tileSize = 256;
	    dataCanvas.width = dataCanvas.height = tileSize;
	    var dataCtx = dataCanvas.getContext("2d");
	    dataCtx.drawImage(dataObj, 0, 0);
	    data = dataCtx.getImageData(0, 0, tileSize, tileSize).data;
	    _this.setupTile(ctx, imageObj, data, tilePoint, zoom, dataUrl, tileInfo, id,getValue); 
	}
	dataObj.crossOrigin = '';
	dataObj.src = dataUrl;
    },

    setupTile: function (ctx, imageObj, data, tileInfo, zoom, dataUrl, tileInfo, id,getValue) {
	_this = this;

	var x = tileInfo.x;
	var y = tileInfo.y;
	var z = -1;

	var noDataTile = true;
	var maxSeconds = Number.MIN_VALUE;
	var minSeconds = Number.MAX_VALUE;


	for (var i = 0, n = data.length; i < n; i += 16) {
	    var green = data[i + 1];
	    var blue = data[i + 2];
	    var alpha = data[i + 3];
	    
	    if (alpha != 0) {
		var seconds = (green * 255) + blue;
		if (seconds < minSeconds) {
		    minSeconds = seconds;
		} else if (seconds > maxSeconds) {
		    maxSeconds = seconds;
		}
	    }
	}

	// If this tile has no data in it, do not begin animation loop.
	if (minSeconds == Number.MAX_VALUE) {
	    return;
	}

	L.animatedTiles[x][y][z].active = true;
	L.animatedTiles[x][y][z].setup = true;
	
	L.activeTiles.push(L.animatedTiles[x][y][z]);

	var drawOnCanvas = this._getDrawOnCanvas(ctx, imageObj, new Uint8ClampedArray(data), minSeconds, maxSeconds, id, L.animatedTiles[x][y][z],getValue);

        var animate = function() {
	    drawOnCanvas();

	    if (L.animatedTiles[x][y][z] != null && L.animatedTiles[x][y][z].active) {
		// Continue the animation loop.
		requestAnimationFrame(animate);
	    } 
	}
	animate();

    },
    _getDrawOnCanvas: function(ctx, imageObj, data, minSeconds, maxSeconds, id, tileInfo,getValue) {
	var oldThreshold = 0;
	var imagePix = new Uint8ClampedArray(data);
	for (var a = 0, b = data.length; a < b; a += 4) {
	    var green = data[a + 1];
	    var blue = data[a + 2];
	    var alpha = data[a + 3];
	    if (alpha == 0) {
		imagePix[a + 3] = 0;
	    } else {
		var time = (green * 255) + blue;
		var minutes = time / 60;
		var range = 0;
		while (range < breakLength - 1 && (minutes > dataBreaks[range])) {
		    range++;
		}
		var c = colorArray[range];
		imagePix[a] = c[0];
		imagePix[a + 1] = c[1];
		imagePix[a + 2] = c[2];
		imagePix[a + 3] = 255;
	    }
	}

	var firstDraw = true;
	var draw = function() {
	    var pix;
	    var threshold = getValue();//travelTimeViz.getDuration();
	   
	    if (oldThreshold != threshold) {
		if (((oldThreshold <= maxSeconds || threshold <= maxSeconds) &&
		    (oldThreshold >= minSeconds || threshold >= minSeconds))) {
		    // clone image data
		    pix = new Uint8ClampedArray(imagePix); 
		    
		    var invisible = true;
		    for (var i = 0, n = data.length; i < n; i += 4) {
			var green = data[i + 1];
			var blue = data[i + 2];
			var alpha = data[i + 3];
			
			var time = (green * 255) + blue;
			if (time > threshold || alpha == 0) {
			    pix[i + 3] = 0; // set alpha to 0
			}
		    } 
		
		    var ctx2 = tileInfo['ctx'];
		    var newImgd = ctx2.getImageData(0,0,256,256);
		    newImgd.data.set(pix);
		    ctx2.putImageData(newImgd,0,0);
		}
		oldThreshold = threshold;
	    } 
	}
	return draw;
    },
    onAdd: function (map) {
	
	this._crs = this.options.crs || map.options.crs;
	
	var projectionKey = parseFloat(this.wmsParams.version) >= 1.3 ? 'crs' : 'srs';
	this.wmsParams[projectionKey] = this._crs.code;
	L.TileLayer.prototype.onAdd.call(this, map);
    },
    
    getTileUrl: function (tilePoint) { // (Point, Number) -> String
	
	var map = this._map,
	tileSize = this.options.tileSize,
	
	nwPoint = tilePoint.multiplyBy(tileSize),
	sePoint = nwPoint.add([tileSize, tileSize]),
	
	nw = this._crs.project(map.unproject(nwPoint, tilePoint.z)),
	se = this._crs.project(map.unproject(sePoint, tilePoint.z)),
	bbox = [nw.x, se.y, se.x, nw.y].join(','),
	
	url = L.Util.template(this._url, {s: this._getSubdomain(tilePoint)});
	
	return url + L.Util.getParamString(this.wmsParams, url, true) + '&bbox=' + bbox;
    }
});
