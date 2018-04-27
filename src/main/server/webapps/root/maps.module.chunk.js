webpackJsonp(["maps.module"],{

/***/ "../../../../../src/app/maps/fullscreenmap/fullscreenmap.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"main-content\">\n    <div class=\"container-fluid\">\n        <div class=\"card \">\n            <div class=\"card-header\">\n                <h4 class=\"card-title\">Full Screen Map</h4>\n            </div>\n            <!-- <div class=\"full-screen-map\"> -->\n                <ngui-map zoom=\"13\" center=\"40.748817,-73.985428\" >\n                    <marker [position]=\"[40.748817,-73.985428]\"></marker>\n                </ngui-map>\n            <!-- </div> -->\n        </div>\n    </div>\n</div>\n"

/***/ }),

/***/ "../../../../../src/app/maps/fullscreenmap/fullscreenmap.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return FullScreenMapsComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var FullScreenMapsComponent = /** @class */ (function () {
    function FullScreenMapsComponent() {
    }
    FullScreenMapsComponent.prototype.ngOnInit = function () {
    };
    FullScreenMapsComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'fullscreen-map-cmp',
            template: __webpack_require__("../../../../../src/app/maps/fullscreenmap/fullscreenmap.component.html")
        })
    ], FullScreenMapsComponent);
    return FullScreenMapsComponent;
}());



/***/ }),

/***/ "../../../../../src/app/maps/googlemaps/googlemaps.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"main-content\">\n    <div class=\"container-fluid\">\n        <div class=\"row\">\n            <div class=\"col-md-12\">\n                <div class=\"card card-map\">\n                    <div class=\"card-header\">\n                        <h4 class=\"card-title\">Satellite Map</h4>\n                    </div>\n                    <div class=\"card-content\">\n                        <ngui-map zoom=\"3\" center=\"40.748817,-73.985428\"  mapTypeId=\"satellite\" >\n                            <marker [position]=\"[40.748817,-73.985428]\"></marker>\n                        </ngui-map>\n                    </div>\n                </div>\n            </div>\n            <div class=\"col-md-12\">\n                <div class=\"card card-map\">\n                    <div class=\"card-header\">\n                        <h4 class=\"card-title\">Regular Map</h4>\n                    </div>\n                    <div class=\"card-content\">\n                        <ngui-map zoom=\"13\" center=\"40.748817,-73.985428\" >\n                            <marker [position]=\"[40.748817,-73.985428]\"></marker>\n                        </ngui-map>\n                    </div>\n                </div>\n            </div>\n        </div>\n    </div>\n</div>\n"

/***/ }),

/***/ "../../../../../src/app/maps/googlemaps/googlemaps.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return GoogleMapsComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var GoogleMapsComponent = /** @class */ (function () {
    function GoogleMapsComponent() {
    }
    GoogleMapsComponent.prototype.ngOnInit = function () {
    };
    GoogleMapsComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'vector-maps-cmp',
            template: __webpack_require__("../../../../../src/app/maps/googlemaps/googlemaps.component.html")
        })
    ], GoogleMapsComponent);
    return GoogleMapsComponent;
}());



/***/ }),

/***/ "../../../../../src/app/maps/maps.module.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "MapsModule", function() { return MapsModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__("../../../router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__("../../../common/esm5/common.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_forms__ = __webpack_require__("../../../forms/esm5/forms.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__ngui_map__ = __webpack_require__("../../../../@ngui/map/dist/@ngui/map.es5.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__maps_routing__ = __webpack_require__("../../../../../src/app/maps/maps.routing.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__fullscreenmap_fullscreenmap_component__ = __webpack_require__("../../../../../src/app/maps/fullscreenmap/fullscreenmap.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__googlemaps_googlemaps_component__ = __webpack_require__("../../../../../src/app/maps/googlemaps/googlemaps.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__vectormaps_vectormaps_component__ = __webpack_require__("../../../../../src/app/maps/vectormaps/vectormaps.component.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};









var MapsModule = /** @class */ (function () {
    function MapsModule() {
    }
    MapsModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["L" /* NgModule */])({
            imports: [
                __WEBPACK_IMPORTED_MODULE_2__angular_common__["b" /* CommonModule */],
                __WEBPACK_IMPORTED_MODULE_1__angular_router__["d" /* RouterModule */].forChild(__WEBPACK_IMPORTED_MODULE_5__maps_routing__["a" /* MapsRoutes */]),
                __WEBPACK_IMPORTED_MODULE_3__angular_forms__["c" /* FormsModule */],
                __WEBPACK_IMPORTED_MODULE_4__ngui_map__["a" /* NguiMapModule */].forRoot({ apiUrl: 'https://maps.google.com/maps/api/js?key=AIzaSyBr-tgUtpm8cyjYVQDrjs8YpZH7zBNWPuY' })
            ],
            declarations: [
                __WEBPACK_IMPORTED_MODULE_6__fullscreenmap_fullscreenmap_component__["a" /* FullScreenMapsComponent */],
                __WEBPACK_IMPORTED_MODULE_7__googlemaps_googlemaps_component__["a" /* GoogleMapsComponent */],
                __WEBPACK_IMPORTED_MODULE_8__vectormaps_vectormaps_component__["a" /* VectorMapsComponent */]
            ],
            schemas: [__WEBPACK_IMPORTED_MODULE_0__angular_core__["j" /* CUSTOM_ELEMENTS_SCHEMA */]]
        })
    ], MapsModule);
    return MapsModule;
}());



/***/ }),

/***/ "../../../../../src/app/maps/maps.routing.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return MapsRoutes; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__fullscreenmap_fullscreenmap_component__ = __webpack_require__("../../../../../src/app/maps/fullscreenmap/fullscreenmap.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__googlemaps_googlemaps_component__ = __webpack_require__("../../../../../src/app/maps/googlemaps/googlemaps.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__vectormaps_vectormaps_component__ = __webpack_require__("../../../../../src/app/maps/vectormaps/vectormaps.component.ts");



var MapsRoutes = [{
        path: '',
        children: [{
                path: 'fullscreen',
                component: __WEBPACK_IMPORTED_MODULE_0__fullscreenmap_fullscreenmap_component__["a" /* FullScreenMapsComponent */]
            }]
    }, {
        path: '',
        children: [{
                path: 'google',
                component: __WEBPACK_IMPORTED_MODULE_1__googlemaps_googlemaps_component__["a" /* GoogleMapsComponent */]
            }]
    }, {
        path: '',
        children: [{
                path: 'vector',
                component: __WEBPACK_IMPORTED_MODULE_2__vectormaps_vectormaps_component__["a" /* VectorMapsComponent */]
            }]
    }
];


/***/ }),

/***/ "../../../../../src/app/maps/vectormaps/vectormaps.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"main-content\">\n    <div class=\"container-fluid\">\n        <div class=\"row\">\n            <div class=\"col-md-12\">\n                <h3 class=\"text-center\">\n                    World Map<br />\n                    <small>\n                        Looks great on any resolution. Made by our friends from <a target=\"_blank\" href=\"http://jvectormap.com/\">jVector Map</a>.\n                    </small>\n                </h3>\n                <div class=\"card card-plain\">\n                    <div class=\"card-content\">\n                        <div id=\"worldMap\" class=\"map map-big\"></div>\n                    </div>\n                </div>\n            </div>\n        </div>\n    </div>\n</div>\n"

/***/ }),

/***/ "../../../../../src/app/maps/vectormaps/vectormaps.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return VectorMapsComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var VectorMapsComponent = /** @class */ (function () {
    function VectorMapsComponent() {
    }
    VectorMapsComponent.prototype.ngOnInit = function () {
        var mapData = {
            "AU": 760,
            "BR": 550,
            "CA": 120,
            "DE": 1300,
            "FR": 540,
            "GB": 690,
            "GE": 200,
            "IN": 200,
            "RO": 600,
            "RU": 300,
            "US": 2920,
        };
        $('#worldMap').vectorMap({
            map: 'world_mill_en',
            backgroundColor: "transparent",
            zoomOnScroll: false,
            regionStyle: {
                initial: {
                    fill: '#e4e4e4',
                    "fill-opacity": 0.9,
                    stroke: 'none',
                    "stroke-width": 0,
                    "stroke-opacity": 0
                }
            },
            series: {
                regions: [{
                        values: mapData,
                        scale: ["#AAAAAA", "#444444"],
                        normalizeFunction: 'polynomial'
                    }]
            },
        });
    };
    VectorMapsComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'vector-maps-cmp',
            template: __webpack_require__("../../../../../src/app/maps/vectormaps/vectormaps.component.html")
        })
    ], VectorMapsComponent);
    return VectorMapsComponent;
}());



/***/ }),

/***/ "../../../../@ngui/map/dist/@ngui/map.es5.js":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export BicyclingLayer */
/* unused harmony export NavigatorGeolocation */
/* unused harmony export OptionBuilder */
/* unused harmony export NG_MAP_CONFIG_TOKEN */
/* unused harmony export NgMapApiLoader */
/* unused harmony export NgMapAsyncApiLoader */
/* unused harmony export NgMapAsyncCallbackApiLoader */
/* unused harmony export NguiMapComponent */
/* unused harmony export InfoWindow */
/* unused harmony export CustomMarker */
/* unused harmony export Circle */
/* unused harmony export DataLayer */
/* unused harmony export DirectionsRenderer */
/* unused harmony export DrawingManager */
/* unused harmony export GeoCoder */
/* unused harmony export GroundOverlay */
/* unused harmony export HeatmapLayer */
/* unused harmony export KmlLayer */
/* unused harmony export Marker */
/* unused harmony export NguiMap */
/* unused harmony export PlacesAutoComplete */
/* unused harmony export Polygon */
/* unused harmony export Polyline */
/* unused harmony export StreetViewPanorama */
/* unused harmony export TrafficLayer */
/* unused harmony export TransitLayer */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return NguiMapModule; });
/* unused harmony export ɵa */
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_rxjs_Observable__ = __webpack_require__("../../../../rxjs/_esm5/Observable.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_ReplaySubject__ = __webpack_require__("../../../../rxjs/_esm5/ReplaySubject.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_operator_first__ = __webpack_require__("../../../../rxjs/_esm5/operator/first.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_rxjs_Subject__ = __webpack_require__("../../../../rxjs/_esm5/Subject.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_rxjs_operator_debounceTime__ = __webpack_require__("../../../../rxjs/_esm5/operator/debounceTime.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__angular_common__ = __webpack_require__("../../../common/esm5/common.js");
var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();







/**
 * return json string from json-like string
 * @param {?} str
 * @return {?}
 */
function jsonize(str) {
    try {
        return str;
    }
    catch (e) {
        return str
            .replace(/([\$\w]+)\s*:/g, // wrap keys without double quote
        function (_, $1) {
            return '"' + $1 + '":';
        })
            .replace(/'([^']+)'/g, // replacing single quote to double quote
        function (_, $1) {
            return '"' + $1 + '"';
        });
    }
}
/**
 * Returns string to an object by using JSON.parse()
 * @param {?} input
 * @return {?}
 */
function getJSON(input) {
    if (typeof input === 'string') {
        var /** @type {?} */ re = /^[\+\-]?[0-9\.]+,[ ]*\ ?[\+\-]?[0-9\.]+$/; // lat,lng
        if (input.match(re)) {
            input = '[' + input + ']';
        }
        return JSON.parse(jsonize(input));
    }
    else {
        return input;
    }
}
/**
 * Returns camel-cased from string 'Foo Bar' to 'fooBar'
 * @param {?} str
 * @return {?}
 */
function toCamelCase(str) {
    return str.replace(/(?:^\w|[A-Z]|\b\w)/g, function (letter, index) {
        return index === 0 ? letter.toLowerCase() : letter.toUpperCase();
    }).replace(/\s+/g, '');
}
/**
 * @return {?}
 */
function isMapsApiLoaded() {
    return typeof google === 'object' && typeof google.maps === 'object';
}
/**
 * @param {?} component
 * @param {?} libName
 * @return {?}
 */
function missingLibraryError(component, libName) {
    return Error(component + ": library '" + libName + "' is missing, please ensure to include it in a 'libraries' parameter.\n    Example:\n      NguiMapModule.forRoot({\n        apiUrl: 'https://maps.googleapis.com/maps/api/js?libraries=" + libName + "'\n      })\n  ");
}
/**
 * @abstract
 */
var BaseMapDirective = /** @class */ (function () {
    /**
     * @param {?} nguiMapComponent
     * @param {?} mapObjectName
     * @param {?} inputs
     * @param {?} outputs
     */
    function BaseMapDirective(nguiMapComponent, mapObjectName, inputs, outputs) {
        var _this = this;
        this.nguiMapComponent = nguiMapComponent;
        this.mapObjectName = mapObjectName;
        this.inputs = inputs;
        this.outputs = outputs;
        // this should be redefined on each childr directive
        this.initialized$ = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["y" /* EventEmitter */]();
        this._subscriptions = [];
        this.nguiMap = this.nguiMapComponent['nguiMap'];
        this.optionBuilder = this.nguiMapComponent['optionBuilder'];
        // all outputs must be initialized
        this.outputs.forEach(function (output) { return _this[output] = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["y" /* EventEmitter */](); });
        this.mapObjectName = mapObjectName;
    }
    /**
     * @return {?}
     */
    BaseMapDirective.prototype.ngOnInit = function () {
        var _this = this;
        if (this.nguiMapComponent.mapIdledOnce) {
            this.initialize();
        }
        else {
            this.nguiMapComponent.mapReady$.subscribe(function (map) { return _this.initialize(); });
        }
    };
    /**
     * @return {?}
     */
    BaseMapDirective.prototype.initialize = function () {
        this.objectOptions = this.optionBuilder.googlizeAllInputs(this.inputs, this);
        // will be set after geocoded
        typeof this.objectOptions.position === 'string' && (delete this.objectOptions.position);
        typeof this.objectOptions.center === 'string' && (delete this.objectOptions.center);
        // noinspection TypeScriptUnresolvedFunction
        if (this.libraryName) {
            if (!google.maps[this.libraryName]) {
                throw missingLibraryError(this.mapObjectName, this.libraryName);
            }
            this.mapObject = new google.maps[this.libraryName][this.mapObjectName](this.objectOptions);
        }
        else {
            this.mapObject = new google.maps[this.mapObjectName](this.objectOptions);
        }
        this.mapObject.setMap(this.nguiMapComponent.map);
        this.mapObject['mapObjectName'] = this.mapObjectName;
        this.mapObject['nguiMapComponent'] = this.nguiMapComponent;
        // set google events listeners and emits to this outputs listeners
        this.nguiMap.setObjectEvents(this.outputs, this, 'mapObject');
        this.nguiMapComponent.addToMapObjectGroup(this.mapObjectName, this.mapObject);
        this.initialized$.emit(this.mapObject);
    };
    /**
     * @param {?} changes
     * @return {?}
     */
    BaseMapDirective.prototype.ngOnChanges = function (changes) {
        this.nguiMap.updateGoogleObject(this.mapObject, changes);
    };
    /**
     * @return {?}
     */
    BaseMapDirective.prototype.ngOnDestroy = function () {
        this._subscriptions.map(function (subscription) { return subscription.unsubscribe(); });
        this.nguiMapComponent.removeFromMapObjectGroup(this.mapObjectName, this.mapObject);
        if (this.mapObject) {
            this.nguiMap.clearObjectEvents(this.outputs, this, 'mapObject');
        }
    };
    return BaseMapDirective;
}());
BaseMapDirective.propDecorators = {
    'initialized$': [{ type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["S" /* Output */] },],
};
/**
 * change any object to google object options
 * e.g. [1,2] -> new google.maps.LatLng(1,2);
 */
var OptionBuilder = /** @class */ (function () {
    function OptionBuilder() {
    }
    /**
     * @param {?} definedInputs
     * @param {?} userInputs
     * @return {?}
     */
    OptionBuilder.prototype.googlizeAllInputs = function (definedInputs, userInputs) {
        var _this = this;
        var /** @type {?} */ options = {};
        // if options given from user, only take options and ignore other inputs
        if (userInputs.options) {
            options = userInputs.options;
            if (!this.onlyOptionsGiven(definedInputs, userInputs)) {
                console.error('when "options" are used, other options are ignored');
            }
        }
        else {
            definedInputs.forEach(function (input) {
                if (userInputs[input] !== undefined) {
                    options[input] = _this.googlize(userInputs[input], { key: input });
                }
            });
        }
        return options;
    };
    /**
     * @param {?} inputs
     * @param {?=} options
     * @return {?}
     */
    OptionBuilder.prototype.googlizeMultiple = function (inputs, options) {
        options = options || {};
        for (var /** @type {?} */ key in inputs) {
            var /** @type {?} */ val = inputs[key];
            // (non-strings are fully converted)
            if (typeof val !== 'string') {
                options[key] = val;
            } // sometimes '0' needed to stay as it is
            else if (!(options['doNotConverStringToNumber'] && val.match(/^[0-9]+$/))) {
                options[key] = this.googlize(val, { key: key });
            }
        } // for(var key in attrs)
        return options;
    };
    /**
     * @param {?} input
     * @param {?=} options
     * @return {?}
     */
    OptionBuilder.prototype.googlize = function (input, options) {
        options = options || {};
        var /** @type {?} */ output = input;
        if (typeof input === 'string') {
            if (input === 'false') {
                output = false;
            }
            else if (input === '0') {
                output = 0;
            }
            else {
                output =
                    // -> googlize -> getJsonParsed -> googlizeMultiple -> googlize until all elements are parsed
                    this.getJSONParsed(input, options)
                        /* Foo.Bar(...) -> new google.maps.Foo.Bar(...) */
                        || this.getAnyMapObject(input)
                        /*  MapTypeID.HYBRID -> new google.maps.MapTypeID.HYBRID */
                        || this.getAnyMapConstant(input, options)
                        /*  2016-06-20 -> new Date('2016-06-20') */
                        || this.getDateObject(input)
                        || input;
            }
        }
        if (options['key']) {
            var /** @type {?} */ key = (options['key']);
            if (output instanceof Array) {
                if (key === 'bounds') {
                    output = new google.maps.LatLngBounds(output[0], output[1]);
                }
                else if (key === 'icons') {
                    output = this.getMapIcons(output);
                }
                else if (key === 'position' || key.match(/^geoFallback/)) {
                    output = this.getLatLng(output);
                }
            }
            else if (output instanceof Object) {
                if (key === 'icon') {
                    output = this.getMarkerIcon(output);
                }
                else if (key.match(/ControlOptions$/)) {
                    output = this.getMapControlOption(output);
                }
            }
        }
        // delete keys only for processing, not used by google
        delete output['doNotConverStringToNumber'];
        delete output['key'];
        return output;
    };
    /**
     * @param {?} input
     * @return {?}
     */
    OptionBuilder.prototype.getLatLng = function (input) {
        var /** @type {?} */ output;
        if (input[0].constructor === Array) {
            output = ((input)).map(function (el) { return new google.maps.LatLng(el[0], el[1]); });
        }
        else if (!isNaN(parseFloat(input[0])) && isFinite(input[0])) {
            output = new google.maps.LatLng(input[0], input[1]);
        }
        return output;
    };
    /**
     * @param {?} input
     * @param {?} options
     * @return {?}
     */
    OptionBuilder.prototype.getJSONParsed = function (input, options) {
        var /** @type {?} */ output;
        try {
            output = getJSON(input);
            if (output instanceof Array) {
                // [{a:1}] : not lat/lng ones
                if (output[0].constructor !== Object) {
                    output = this.getLatLng(output);
                }
            }
            else if (output === Object(output)) {
                // check for nested hashes and convert to Google API options
                var /** @type {?} */ newOptions = options;
                newOptions['doNotConverStringToNumber'] = true;
                output = this.googlizeMultiple(output, newOptions);
            }
        }
        catch (e) {
        }
        return output;
    };
    /**
     * @param {?} input
     * @return {?}
     */
    OptionBuilder.prototype.getAnyMapObject = function (input) {
        var /** @type {?} */ output;
        if (input.match(/^[A-Z][a-zA-Z0-9]+\(.*\)$/)) {
            try {
                output = Function("return new google.maps." + input + ";")();
            }
            catch (e) { }
        }
        return output;
    };
    /**
     * @param {?} input
     * @param {?} options
     * @return {?}
     */
    OptionBuilder.prototype.getAnyMapConstant = function (input, options) {
        var /** @type {?} */ output;
        if (input.match(/^([A-Z][a-zA-Z0-9]+)\.([A-Z]+)$/)) {
            try {
                var /** @type {?} */ matches = input.match(/^([A-Z][a-zA-Z0-9]+)\.([A-Z]+)$/);
                output = google.maps[matches[1]][matches[2]];
            }
            catch (e) { }
        }
        else if (input.match(/^[A-Z]+$/)) {
            try {
                var /** @type {?} */ capitalizedKey = ((options['key'])).charAt(0).toUpperCase() +
                    ((options['key'])).slice(1);
                output = google.maps[capitalizedKey][input];
            }
            catch (e) { }
        }
        return output;
    };
    /**
     * streetviewControl, panControl, etc, not a general control
     * @param {?} controlOptions
     * @return {?}
     */
    OptionBuilder.prototype.getMapControlOption = function (controlOptions) {
        var /** @type {?} */ newControlOptions = controlOptions;
        for (var /** @type {?} */ key in newControlOptions) {
            if (newControlOptions[key]) {
                var /** @type {?} */ value = newControlOptions[key];
                if (typeof value === 'string') {
                    value = ((value)).toUpperCase();
                }
                else if (key === 'mapTypeIds') {
                    value = ((value)).map(function (str) {
                        if (str.match(/^[A-Z]+$/)) {
                            return google.maps.MapTypeId[str.toUpperCase()];
                        }
                        else {
                            return str;
                        }
                    });
                }
                if (key === 'style') {
                    var /** @type {?} */ objName = key.replace(/Options$/, '') + 'Style';
                    newControlOptions[key] = google.maps[objName][(value)];
                }
                else if (key === 'position') {
                    newControlOptions[key] = google.maps.ControlPosition[(value)];
                }
                else {
                    newControlOptions[key] = value;
                }
            }
        }
        return newControlOptions;
    };
    /**
     * @param {?} input
     * @return {?}
     */
    OptionBuilder.prototype.getDateObject = function (input) {
        var /** @type {?} */ output;
        if (input.match(/^(\d{4}\-\d\d\-\d\d([tT][\d:\.]*)?)([zZ]|([+\-])(\d\d):?(\d\d))?$/)) {
            try {
                output = new Date(input);
            }
            catch (e) { }
        }
        return output;
    };
    /**
     * @param {?} input
     * @return {?}
     */
    OptionBuilder.prototype.getMapIcons = function (input) {
        return input.map(function (el) {
            if (el.icon.path.match(/^[A-Z_]+$/)) {
                el.icon.path = google.maps.SymbolPath[el.icon.path];
            }
            return el;
        });
    };
    /**
     * @param {?} input
     * @return {?}
     */
    OptionBuilder.prototype.getMarkerIcon = function (input) {
        var /** @type {?} */ output = input;
        if (('' + output.path).match(/^[A-Z_]+$/)) {
            output.path = google.maps.SymbolPath[output.path];
        }
        for (var /** @type {?} */ key in output) {
            var /** @type {?} */ arr = output[key];
            if (key === 'anchor' || key === 'origin' || key === 'labelOrigin') {
                output[key] = new google.maps.Point(arr[0], arr[1]);
            }
            else if (key === 'size' || key === 'scaledSize') {
                output[key] = new google.maps.Size(arr[0], arr[1]);
            }
        }
        return output;
    };
    /**
     * @param {?} definedInputs
     * @param {?} userInputs
     * @return {?}
     */
    OptionBuilder.prototype.onlyOptionsGiven = function (definedInputs, userInputs) {
        for (var /** @type {?} */ i = 0; i < definedInputs.length; i++) {
            var /** @type {?} */ input = definedInputs[i];
            if (input !== 'options' && typeof userInputs[input] !== 'undefined') {
                return false;
            }
        }
        return true;
    };
    return OptionBuilder;
}());
OptionBuilder.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["D" /* Injectable */] },
];
/**
 * @nocollapse
 */
OptionBuilder.ctorParameters = function () { return []; };
/**
 *  service for navigator.geolocation methods
 */
var NavigatorGeolocation = /** @class */ (function () {
    function NavigatorGeolocation() {
    }
    /**
     * @param {?=} geoLocationOptions
     * @return {?}
     */
    NavigatorGeolocation.prototype.getCurrentPosition = function (geoLocationOptions) {
        geoLocationOptions = geoLocationOptions || { timeout: 5000 };
        return new __WEBPACK_IMPORTED_MODULE_1_rxjs_Observable__["a" /* Observable */](function (responseObserver) {
            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(function (position) {
                    responseObserver.next(position);
                    responseObserver.complete();
                }, function (evt) { return responseObserver.error(evt); }, geoLocationOptions);
            }
            else {
                responseObserver.error('Browser Geolocation service failed.');
            }
        });
    };
    return NavigatorGeolocation;
}());
NavigatorGeolocation.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["D" /* Injectable */] },
];
/**
 * @nocollapse
 */
NavigatorGeolocation.ctorParameters = function () { return []; };
var NG_MAP_CONFIG_TOKEN = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["E" /* InjectionToken */]('NG_MAP_CONFIG_TOKEN');
/**
 * @abstract
 */
var NgMapApiLoader = /** @class */ (function () {
    /**
     * @param {?} config
     */
    function NgMapApiLoader(config) {
        this.config = config;
        this.api$ = __WEBPACK_IMPORTED_MODULE_3_rxjs_operator_first__["a" /* first */].call(new __WEBPACK_IMPORTED_MODULE_2_rxjs_ReplaySubject__["a" /* ReplaySubject */](1));
        this.config = this.config || { apiUrl: 'https://maps.google.com/maps/api/js' };
    }
    /**
     * @abstract
     * @return {?}
     */
    NgMapApiLoader.prototype.load = function () { };
    /**
     * @return {?}
     */
    NgMapApiLoader.prototype.ngOnDestroy = function () {
        this.api$.complete();
    };
    return NgMapApiLoader;
}());
var NgMapAsyncCallbackApiLoader = /** @class */ (function (_super) {
    __extends(NgMapAsyncCallbackApiLoader, _super);
    /**
     * @param {?} zone
     * @param {?} config
     */
    function NgMapAsyncCallbackApiLoader(zone, config) {
        var _this = _super.call(this, config) || this;
        _this.zone = zone;
        return _this;
    }
    /**
     * @return {?}
     */
    NgMapAsyncCallbackApiLoader.prototype.load = function () {
        var _this = this;
        if (typeof window === 'undefined') {
            return;
        }
        if (isMapsApiLoaded()) {
            this.api$.next(google.maps);
        }
        else if (!document.querySelector('#ngui-map-api')) {
            ((window))['nguiMapRef'] = ((window))['nguiMapRef'] || [];
            ((window))['nguiMapRef'].push({ zone: this.zone, componentFn: function () { return _this.api$.next(google.maps); } });
            this.addGoogleMapsApi();
        }
    };
    /**
     * @return {?}
     */
    NgMapAsyncCallbackApiLoader.prototype.addGoogleMapsApi = function () {
        ((window))['initNguiMap'] = ((window))['initNguiMap'] || function () {
            ((window))['nguiMapRef'].forEach(function (nguiMapRef) {
                nguiMapRef.zone.run(function () { nguiMapRef.componentFn(); });
            });
            ((window))['nguiMapRef'].splice(0, ((window))['nguiMapRef'].length);
        };
        var /** @type {?} */ script = document.createElement('script');
        script.id = 'ngui-map-api';
        // script.src = "https://maps.google.com/maps/api/js?callback=initNguiMap";
        var /** @type {?} */ apiUrl = this.config.apiUrl;
        apiUrl += apiUrl.indexOf('?') !== -1 ? '&' : '?';
        script.src = apiUrl + 'callback=initNguiMap';
        document.querySelector('body').appendChild(script);
    };
    return NgMapAsyncCallbackApiLoader;
}(NgMapApiLoader));
NgMapAsyncCallbackApiLoader.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["D" /* Injectable */] },
];
/**
 * @nocollapse
 */
NgMapAsyncCallbackApiLoader.ctorParameters = function () { return [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["Q" /* NgZone */], },
    { type: undefined, decorators: [{ type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["R" /* Optional */] }, { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["C" /* Inject */], args: [NG_MAP_CONFIG_TOKEN,] },] },
]; };
var NgMapAsyncApiLoader = /** @class */ (function (_super) {
    __extends(NgMapAsyncApiLoader, _super);
    /**
     * @param {?} config
     */
    function NgMapAsyncApiLoader(config) {
        return _super.call(this, config) || this;
    }
    /**
     * @return {?}
     */
    NgMapAsyncApiLoader.prototype.load = function () {
        var _this = this;
        if (typeof window === 'undefined') {
            return;
        }
        if (isMapsApiLoaded()) {
            this.api$.next(google.maps);
        }
        else if (!document.querySelector('#ngui-map-api')) {
            var /** @type {?} */ script = document.createElement('script');
            script.id = 'ngui-map-api';
            script.async = true;
            script.onload = function () { return _this.api$.next(google.maps); };
            script.src = this.config.apiUrl;
            document.querySelector('body').appendChild(script);
        }
    };
    return NgMapAsyncApiLoader;
}(NgMapApiLoader));
NgMapAsyncApiLoader.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["D" /* Injectable */] },
];
/**
 * @nocollapse
 */
NgMapAsyncApiLoader.ctorParameters = function () { return [
    { type: undefined, decorators: [{ type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["R" /* Optional */] }, { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["C" /* Inject */], args: [NG_MAP_CONFIG_TOKEN,] },] },
]; };
/**
 *   Provides [defered/promise API](https://docs.angularjs.org/api/ng/service/$q)
 *   service for Google Geocoder service
 */
var GeoCoder = /** @class */ (function () {
    /**
     * @param {?} apiLoader
     */
    function GeoCoder(apiLoader) {
        this.apiLoader = apiLoader;
        this.apiLoaderSubs = [];
    }
    /**
     * @param {?} options
     * @return {?}
     */
    GeoCoder.prototype.geocode = function (options) {
        var _this = this;
        return new __WEBPACK_IMPORTED_MODULE_1_rxjs_Observable__["a" /* Observable */](function (responseObserver) {
            _this.apiLoaderSubs.push(_this.apiLoader.api$
                .subscribe(function () { return _this.requestGeocode(options, responseObserver); }));
        });
    };
    /**
     * @return {?}
     */
    GeoCoder.prototype.ngOnDestroy = function () {
        this.apiLoaderSubs.map(function (sub) { return sub.unsubscribe(); });
    };
    /**
     * @param {?} options
     * @param {?} observer
     * @return {?}
     */
    GeoCoder.prototype.requestGeocode = function (options, observer) {
        var /** @type {?} */ geocoder = new google.maps.Geocoder();
        geocoder.geocode(options, function (results, status) {
            if (status === google.maps.GeocoderStatus.OK) {
                observer.next(results);
                observer.complete();
            }
            else {
                observer.error(results);
            }
        });
    };
    return GeoCoder;
}());
GeoCoder.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["D" /* Injectable */] },
];
/**
 * @nocollapse
 */
GeoCoder.ctorParameters = function () { return [
    { type: NgMapApiLoader, },
]; };
/**
 * collection of map instance-related properties and methods
 */
var NguiMap = /** @class */ (function () {
    /**
     * @param {?} geoCoder
     * @param {?} optionBuilder
     * @param {?} zone
     */
    function NguiMap(geoCoder, optionBuilder, zone) {
        var _this = this;
        this.geoCoder = geoCoder;
        this.optionBuilder = optionBuilder;
        this.zone = zone;
        this.updateGoogleObject = function (object, changes) {
            var /** @type {?} */ val, /** @type {?} */ currentValue, /** @type {?} */ setMethodName;
            if (object) {
                for (var /** @type {?} */ key in changes) {
                    setMethodName = "set" + key.replace(/^[a-z]/, function (x) { return x.toUpperCase(); });
                    currentValue = changes[key].currentValue;
                    if (['position', 'center'].indexOf(key) !== -1 && typeof currentValue === 'string') {
                        // To preserve setMethod name in Observable callback, wrap it as a function, then execute
                        (function (setMethodName) {
                            _this.geoCoder.geocode({ address: currentValue }).subscribe(function (results) {
                                if (typeof object[setMethodName] === 'function') {
                                    object[setMethodName](results[0].geometry.location);
                                }
                                else {
                                    console.error('Not all options are dynamically updatable according to Googles Maps API V3 documentation.\n' +
                                        'Please check Google Maps API documentation, and use "setOptions" instead.');
                                }
                            });
                        })(setMethodName);
                    }
                    else {
                        val = _this.optionBuilder.googlize(currentValue);
                        if (typeof object[setMethodName] === 'function') {
                            object[setMethodName](val);
                        }
                        else {
                            console.error('Not all options are dynamically updatable according to Googles Maps API V3 documentation.\n' +
                                'Please check Google Maps API documentation, and use "setOptions" instead.');
                        }
                    }
                }
            }
        };
    }
    /**
     * @param {?} definedEvents
     * @param {?} thisObj
     * @param {?} prefix
     * @return {?}
     */
    NguiMap.prototype.setObjectEvents = function (definedEvents, thisObj, prefix) {
        var _this = this;
        definedEvents.forEach(function (definedEvent) {
            var /** @type {?} */ eventName = _this.getEventName(definedEvent), /** @type {?} */ zone = _this.zone;
            zone.runOutsideAngular(function () {
                thisObj[prefix].addListener(eventName, function (event) {
                    var /** @type {?} */ param = event ? event : {};
                    param.target = this;
                    zone.run(function () { return thisObj[definedEvent].emit(param); });
                });
            });
        });
    };
    /**
     * @param {?} definedEvents
     * @param {?} thisObj
     * @param {?} prefix
     * @return {?}
     */
    NguiMap.prototype.clearObjectEvents = function (definedEvents, thisObj, prefix) {
        var _this = this;
        definedEvents.forEach(function (definedEvent) {
            var /** @type {?} */ eventName = _this.getEventName(definedEvent);
            _this.zone.runOutsideAngular(function () {
                if (thisObj[prefix]) {
                    google.maps.event.clearListeners(thisObj[prefix], eventName);
                }
            });
        });
        if (thisObj[prefix]) {
            if (thisObj[prefix].setMap) {
                thisObj[prefix].setMap(null);
            }
            delete thisObj[prefix].nguiMapComponent;
            delete thisObj[prefix];
        }
    };
    /**
     * @param {?} definedEvent
     * @return {?}
     */
    NguiMap.prototype.getEventName = function (definedEvent) {
        return definedEvent
            .replace(/([A-Z])/g, function ($1) { return "_" + $1.toLowerCase(); }) // positionChanged -> position_changed
            .replace(/^map_/, ''); // map_click -> click  to avoid DOM conflicts
    };
    return NguiMap;
}());
NguiMap.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["D" /* Injectable */] },
];
/**
 * @nocollapse
 */
NguiMap.ctorParameters = function () { return [
    { type: GeoCoder, },
    { type: OptionBuilder, },
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["Q" /* NgZone */], },
]; };
var INPUTS$1 = [
    'backgroundColor', 'center', 'disableDefaultUI', 'disableDoubleClickZoom', 'draggable', 'draggableCursor',
    'draggingCursor', 'heading', 'keyboardShortcuts', 'mapMaker', 'mapTypeControl', 'mapTypeId', 'maxZoom', 'minZoom',
    'noClear', 'overviewMapControl', 'panControl', 'panControlOptions', 'rotateControl', 'scaleControl', 'scrollwheel',
    'streetView', 'styles', 'tilt', 'zoom', 'streetViewControl', 'zoomControl', 'zoomControlOptions', 'mapTypeControlOptions',
    'overviewMapControlOptions', 'rotateControlOptions', 'scaleControlOptions', 'streetViewControlOptions', 'fullscreenControl', 'fullscreenControlOptions',
    'options',
    // ngui-map-specific inputs
    'geoFallbackCenter'
];
var OUTPUTS$1 = [
    'bounds_changed', 'center_changed', 'click', 'dblclick', 'drag', 'dragend', 'dragstart', 'heading_changed', 'idle',
    'typeid_changed', 'mousemove', 'mouseout', 'mouseover', 'projection_changed', 'resize', 'rightclick',
    'tilesloaded', 'tile_changed', 'zoom_changed',
    // to avoid DOM event conflicts
    'mapClick', 'mapMouseover', 'mapMouseout', 'mapMousemove', 'mapDrag', 'mapDragend', 'mapDragstart'
];
var NguiMapComponent = /** @class */ (function () {
    /**
     * @param {?} optionBuilder
     * @param {?} elementRef
     * @param {?} geolocation
     * @param {?} geoCoder
     * @param {?} nguiMap
     * @param {?} apiLoader
     * @param {?} zone
     */
    function NguiMapComponent(optionBuilder, elementRef, geolocation, geoCoder, nguiMap, apiLoader, zone) {
        var _this = this;
        this.optionBuilder = optionBuilder;
        this.elementRef = elementRef;
        this.geolocation = geolocation;
        this.geoCoder = geoCoder;
        this.nguiMap = nguiMap;
        this.apiLoader = apiLoader;
        this.zone = zone;
        this.mapReady$ = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["y" /* EventEmitter */]();
        this.mapOptions = {};
        this.inputChanges$ = new __WEBPACK_IMPORTED_MODULE_4_rxjs_Subject__["a" /* Subject */]();
        this.infoWindows = {};
        this.mapIdledOnce = false;
        this.initializeMapAfterDisplayed = false;
        apiLoader.load();
        // all outputs needs to be initialized,
        // http://stackoverflow.com/questions/37765519/angular2-directive-cannot-read-property-subscribe-of-undefined-with-outputs
        OUTPUTS$1.forEach(function (output) { return _this[output] = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["y" /* EventEmitter */](); });
    }
    /**
     * @return {?}
     */
    NguiMapComponent.prototype.ngAfterViewInit = function () {
        var _this = this;
        this.apiLoaderSub = this.apiLoader.api$.subscribe(function () { return _this.initializeMap(); });
    };
    /**
     * @return {?}
     */
    NguiMapComponent.prototype.ngAfterViewChecked = function () {
        if (this.initializeMapAfterDisplayed && this.el && this.el.offsetWidth > 0) {
            this.initializeMap();
        }
    };
    /**
     * @param {?} changes
     * @return {?}
     */
    NguiMapComponent.prototype.ngOnChanges = function (changes) {
        this.inputChanges$.next(changes);
    };
    /**
     * @return {?}
     */
    NguiMapComponent.prototype.initializeMap = function () {
        var _this = this;
        this.el = this.elementRef.nativeElement.querySelector('.google-map');
        if (this.el && this.el.offsetWidth === 0) {
            this.initializeMapAfterDisplayed = true;
            return;
        }
        this.initializeMapAfterDisplayed = false;
        this.mapOptions = this.optionBuilder.googlizeAllInputs(INPUTS$1, this);
        this.mapOptions.zoom = this.mapOptions.zoom || 15;
        typeof this.mapOptions.center === 'string' && (delete this.mapOptions.center);
        this.zone.runOutsideAngular(function () {
            _this.map = new google.maps.Map(_this.el, _this.mapOptions);
            _this.map['mapObjectName'] = 'NguiMapComponent';
            if (!_this.mapOptions.center) {
                _this.setCenter();
            }
            // set google events listeners and emits to this outputs listeners
            _this.nguiMap.setObjectEvents(OUTPUTS$1, _this, 'map');
            _this.map.addListener('idle', function () {
                if (!_this.mapIdledOnce) {
                    _this.mapIdledOnce = true;
                    setTimeout(function () {
                        _this.mapReady$.emit(_this.map);
                    });
                }
            });
            // update map when input changes
            __WEBPACK_IMPORTED_MODULE_5_rxjs_operator_debounceTime__["a" /* debounceTime */].call(_this.inputChanges$, 1000)
                .subscribe(function (changes) { return _this.nguiMap.updateGoogleObject(_this.map, changes); });
            if (typeof window !== 'undefined' && ((window))['nguiMapRef']) {
                // expose map object for test and debugging on (<any>window)
                ((window))['nguiMapRef'].map = _this.map;
            }
        });
    };
    /**
     * @return {?}
     */
    NguiMapComponent.prototype.setCenter = function () {
        var _this = this;
        if (!this['center']) {
            this.geolocation.getCurrentPosition().subscribe(function (position) {
                var /** @type {?} */ latLng = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
                _this.map.setCenter(latLng);
            }, function (error) {
                console.error('ngui-map: Error finding the current position');
                _this.map.setCenter(_this.mapOptions['geoFallbackCenter'] || new google.maps.LatLng(0, 0));
            });
        }
        else if (typeof this['center'] === 'string') {
            this.geoCoder.geocode({ address: this['center'] }).subscribe(function (results) {
                _this.map.setCenter(results[0].geometry.location);
            }, function (error) {
                _this.map.setCenter(_this.mapOptions['geoFallbackCenter'] || new google.maps.LatLng(0, 0));
            });
        }
    };
    /**
     * @param {?} id
     * @param {?} anchor
     * @return {?}
     */
    NguiMapComponent.prototype.openInfoWindow = function (id, anchor) {
        this.infoWindows[id].open(anchor);
    };
    /**
     * @param {?} id
     * @return {?}
     */
    NguiMapComponent.prototype.closeInfoWindow = function (id) {
        // if infoWindow for id exists, close the infoWindow
        if (this.infoWindows[id])
            this.infoWindows[id].close();
    };
    /**
     * @return {?}
     */
    NguiMapComponent.prototype.ngOnDestroy = function () {
        this.inputChanges$.complete();
        if (this.el && !this.initializeMapAfterDisplayed) {
            this.nguiMap.clearObjectEvents(OUTPUTS$1, this, 'map');
        }
        if (this.apiLoaderSub) {
            this.apiLoaderSub.unsubscribe();
        }
    };
    /**
     * @param {?} mapObjectName
     * @param {?} mapObject
     * @return {?}
     */
    NguiMapComponent.prototype.addToMapObjectGroup = function (mapObjectName, mapObject) {
        var /** @type {?} */ groupName = toCamelCase(mapObjectName.toLowerCase()) + 's'; // e.g. markers
        this.map[groupName] = this.map[groupName] || [];
        this.map[groupName].push(mapObject);
    };
    /**
     * @param {?} mapObjectName
     * @param {?} mapObject
     * @return {?}
     */
    NguiMapComponent.prototype.removeFromMapObjectGroup = function (mapObjectName, mapObject) {
        var /** @type {?} */ groupName = toCamelCase(mapObjectName.toLowerCase()) + 's'; // e.g. markers
        if (this.map && this.map[groupName]) {
            var /** @type {?} */ index = this.map[groupName].indexOf(mapObject);
            (index > -1) && this.map[groupName].splice(index, 1);
        }
    };
    return NguiMapComponent;
}());
NguiMapComponent.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */], args: [{
                selector: 'ngui-map',
                providers: [NguiMap, OptionBuilder, GeoCoder, NavigatorGeolocation],
                styles: ["\n    ngui-map {display: block; height: 300px;}\n    .google-map {width: 100%; height: 100%}\n  "],
                inputs: INPUTS$1,
                outputs: OUTPUTS$1,
                encapsulation: __WEBPACK_IMPORTED_MODULE_0__angular_core__["_15" /* ViewEncapsulation */].None,
                template: "\n    <div class=\"google-map\"></div>\n    <ng-content></ng-content>\n  ",
            },] },
];
/**
 * @nocollapse
 */
NguiMapComponent.ctorParameters = function () { return [
    { type: OptionBuilder, },
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["v" /* ElementRef */], },
    { type: NavigatorGeolocation, },
    { type: GeoCoder, },
    { type: NguiMap, },
    { type: NgMapApiLoader, },
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["Q" /* NgZone */], },
]; };
NguiMapComponent.propDecorators = {
    'mapReady$': [{ type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["S" /* Output */] },],
};
var INPUTS = [];
var OUTPUTS = [];
var BicyclingLayer = /** @class */ (function (_super) {
    __extends(BicyclingLayer, _super);
    /**
     * @param {?} nguiMapComp
     */
    function BicyclingLayer(nguiMapComp) {
        return _super.call(this, nguiMapComp, 'BicyclingLayer', INPUTS, OUTPUTS) || this;
    }
    return BicyclingLayer;
}(BaseMapDirective));
BicyclingLayer.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */], args: [{
                selector: 'ngui-map > bicycling-layer',
                inputs: INPUTS,
                outputs: OUTPUTS,
            },] },
];
/**
 * @nocollapse
 */
BicyclingLayer.ctorParameters = function () { return [
    { type: NguiMapComponent, },
]; };
var INPUTS$2 = [
    'content', 'disableAutoPan', 'maxWidth', 'pixelOffset', 'position', 'zIndex', 'options'
];
var OUTPUTS$2 = [
    'closeclick', 'content_changed', 'domready', 'position_changed', 'zindex_changed'
];
var InfoWindow = /** @class */ (function () {
    /**
     * @param {?} elementRef
     * @param {?} nguiMap
     * @param {?} nguiMapComponent
     */
    function InfoWindow(elementRef, nguiMap, nguiMapComponent) {
        var _this = this;
        this.elementRef = elementRef;
        this.nguiMap = nguiMap;
        this.nguiMapComponent = nguiMapComponent;
        this.initialized$ = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["y" /* EventEmitter */]();
        this.objectOptions = {};
        this.inputChanges$ = new __WEBPACK_IMPORTED_MODULE_4_rxjs_Subject__["a" /* Subject */]();
        this.elementRef.nativeElement.style.display = 'none';
        OUTPUTS$2.forEach(function (output) { return _this[output] = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["y" /* EventEmitter */](); });
    }
    /**
     * @return {?}
     */
    InfoWindow.prototype.ngOnInit = function () {
        var _this = this;
        if (this.nguiMapComponent.mapIdledOnce) {
            this.initialize();
        }
        else {
            this.nguiMapComponent.mapReady$.subscribe(function (map) { return _this.initialize(); });
        }
    };
    /**
     * @param {?} changes
     * @return {?}
     */
    InfoWindow.prototype.ngOnChanges = function (changes) {
        this.inputChanges$.next(changes);
    };
    /**
     * @return {?}
     */
    InfoWindow.prototype.initialize = function () {
        var _this = this;
        this.objectOptions = this.nguiMapComponent.optionBuilder.googlizeAllInputs(INPUTS$2, this);
        this.infoWindow = new google.maps.InfoWindow(this.objectOptions);
        this.infoWindow['mapObjectName'] = 'InfoWindow';
        // register infoWindow ids to NguiMap, so that it can be opened by id
        if (this.elementRef.nativeElement.id) {
            this.nguiMapComponent.infoWindows[this.elementRef.nativeElement.id] = this;
        }
        else {
            console.error('An InfoWindow must have an id. e.g. id="detail"');
        }
        // set google events listeners and emits to this outputs listeners
        this.nguiMap.setObjectEvents(OUTPUTS$2, this, 'infoWindow');
        // update object when input changes
        __WEBPACK_IMPORTED_MODULE_5_rxjs_operator_debounceTime__["a" /* debounceTime */].call(this.inputChanges$, 1000)
            .subscribe(function (changes) { return _this.nguiMap.updateGoogleObject(_this.infoWindow, changes); });
        this.nguiMapComponent.addToMapObjectGroup('InfoWindow', this.infoWindow);
        this.initialized$.emit(this.infoWindow);
    };
    /**
     * @param {?} anchor
     * @return {?}
     */
    InfoWindow.prototype.open = function (anchor) {
        // set content and open it
        this.infoWindow.setContent(this.template.element.nativeElement);
        this.infoWindow.open(this.nguiMapComponent.map, anchor);
    };
    /**
     * @return {?}
     */
    InfoWindow.prototype.close = function () {
        // check if infoWindow exists, and closes it
        if (this.infoWindow)
            this.infoWindow.close();
    };
    /**
     * @return {?}
     */
    InfoWindow.prototype.ngOnDestroy = function () {
        this.inputChanges$.complete();
        if (this.infoWindow) {
            this.nguiMap.clearObjectEvents(OUTPUTS$2, this, 'infoWindow');
            delete this.infoWindow;
        }
    };
    return InfoWindow;
}());
InfoWindow.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */], args: [{
                selector: 'ngui-map > info-window',
                inputs: INPUTS$2,
                outputs: OUTPUTS$2,
                template: "<div #template><ng-content></ng-content></div>",
            },] },
];
/**
 * @nocollapse
 */
InfoWindow.ctorParameters = function () { return [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["v" /* ElementRef */], },
    { type: NguiMap, },
    { type: NguiMapComponent, },
]; };
InfoWindow.propDecorators = {
    'initialized$': [{ type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["S" /* Output */] },],
    'template': [{ type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["_12" /* ViewChild */], args: ['template', { read: __WEBPACK_IMPORTED_MODULE_0__angular_core__["_14" /* ViewContainerRef */] },] },],
};
var INPUTS$3 = [
    'position'
];
// to avoid DOM event conflicts map_*
var OUTPUTS$3 = [
    'animationChanged', 'click', 'clickableChanged', 'cursorChanged', 'dblclick', 'drag', 'dragend', 'draggableChanged',
    'dragstart', 'flatChanged', 'iconChanged', 'mousedown', 'mouseout', 'mouseover', 'mouseup', 'positionChanged', 'rightclick',
    'shapeChanged', 'titleChanged', 'visibleChanged', 'zindexChanged',
    'map_click', 'map_mouseover', 'map_mouseout', 'map_mouseup', 'map_mousedown', 'map_drag', 'map_dragend'
];
/**
 * Wrapper to a create extend OverlayView at runtime, only after google maps is loaded.
 * Otherwise throws a google is unknown error.
 * @param {?} htmlEl
 * @param {?} position
 * @return {?}
 */
function getCustomMarkerOverlayView(htmlEl, position) {
    var CustomMarkerOverlayView = /** @class */ (function (_super) {
        __extends(CustomMarkerOverlayView, _super);
        /**
         * @param {?} htmlEl
         * @param {?} position
         */
        function CustomMarkerOverlayView(htmlEl, position) {
            var _this = _super.call(this) || this;
            _this.visible = true;
            _this.setPosition = function (position) {
                _this.htmlEl.style.visibility = 'hidden';
                if (position.constructor.name === 'Array') {
                    _this.position = new google.maps.LatLng(position[0], position[1]);
                }
                else if (typeof position === 'string') {
                    var /** @type {?} */ geocoder = new google.maps.Geocoder();
                    geocoder.geocode({ address: position }, function (results, status) {
                        if (status === google.maps.GeocoderStatus.OK) {
                            _this.setPosition(results[0].geometry.location);
                        }
                        else {
                        }
                    });
                }
                else if (position && typeof position.lng === 'function') {
                    _this.position = position;
                }
                if (_this.getProjection() && typeof _this.position.lng === 'function') {
                    var /** @type {?} */ positionOnMap_1 = function () {
                        var /** @type {?} */ projection = _this.getProjection();
                        if (!projection) {
                            return;
                        }
                        var /** @type {?} */ posPixel = projection.fromLatLngToDivPixel(_this.position);
                        var /** @type {?} */ x = Math.round(posPixel.x - (_this.htmlEl.offsetWidth / 2));
                        var /** @type {?} */ y = Math.round(posPixel.y - _this.htmlEl.offsetHeight / 2);
                        _this.htmlEl.style.left = x + 'px';
                        _this.htmlEl.style.top = y + 'px';
                        _this.htmlEl.style.visibility = 'visible';
                    };
                    if (_this.htmlEl.offsetWidth && _this.htmlEl.offsetHeight) {
                        positionOnMap_1();
                    }
                    else {
                        setTimeout(function () { return positionOnMap_1(); });
                    }
                }
            };
            _this.htmlEl = htmlEl;
            _this.position = position;
            return _this;
        }
        /**
         * @return {?}
         */
        CustomMarkerOverlayView.prototype.onAdd = function () {
            this.getPanes().overlayMouseTarget.appendChild(this.htmlEl);
            // required for correct display inside google maps container
            this.htmlEl.style.position = 'absolute';
        };
        /**
         * @return {?}
         */
        CustomMarkerOverlayView.prototype.draw = function () {
            this.setPosition(this.position);
            this.setZIndex(this.zIndex);
            this.setVisible(this.visible);
        };
        /**
         * @return {?}
         */
        CustomMarkerOverlayView.prototype.onRemove = function () {
            //
        };
        /**
         * @return {?}
         */
        CustomMarkerOverlayView.prototype.getPosition = function () {
            return this.position;
        };
        /**
         * @param {?} zIndex
         * @return {?}
         */
        CustomMarkerOverlayView.prototype.setZIndex = function (zIndex) {
            zIndex && (this.zIndex = zIndex); /* jshint ignore:line */
            this.htmlEl.style.zIndex = this.zIndex;
        };
        /**
         * @param {?} visible
         * @return {?}
         */
        CustomMarkerOverlayView.prototype.setVisible = function (visible) {
            this.htmlEl.style.display = visible ? 'inline-block' : 'none';
            this.visible = visible;
        };
        return CustomMarkerOverlayView;
    }(google.maps.OverlayView));
    return new CustomMarkerOverlayView(htmlEl, position);
}
var CustomMarker = /** @class */ (function () {
    /**
     * @param {?} nguiMapComponent
     * @param {?} elementRef
     * @param {?} nguiMap
     */
    function CustomMarker(nguiMapComponent, elementRef, nguiMap) {
        var _this = this;
        this.nguiMapComponent = nguiMapComponent;
        this.elementRef = elementRef;
        this.nguiMap = nguiMap;
        this.initialized$ = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["y" /* EventEmitter */]();
        this.inputChanges$ = new __WEBPACK_IMPORTED_MODULE_4_rxjs_Subject__["a" /* Subject */]();
        this.elementRef.nativeElement.style.display = 'none';
        OUTPUTS$3.forEach(function (output) { return _this[output] = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["y" /* EventEmitter */](); });
    }
    /**
     * @return {?}
     */
    CustomMarker.prototype.ngOnInit = function () {
        var _this = this;
        if (this.nguiMapComponent.mapIdledOnce) {
            this.initialize();
        }
        else {
            this.nguiMapComponent.mapReady$.subscribe(function (map) { return _this.initialize(); });
        }
    };
    /**
     * @param {?} changes
     * @return {?}
     */
    CustomMarker.prototype.ngOnChanges = function (changes) {
        this.inputChanges$.next(changes);
    };
    /**
     * @return {?}
     */
    CustomMarker.prototype.ngOnDestroy = function () {
        this.inputChanges$.complete();
        this.nguiMapComponent.removeFromMapObjectGroup('CustomMarker', this.mapObject);
        if (this.mapObject) {
            this.nguiMap.clearObjectEvents(OUTPUTS$3, this, 'mapObject');
        }
    };
    /**
     * @return {?}
     */
    CustomMarker.prototype.initialize = function () {
        var _this = this;
        this.el = this.elementRef.nativeElement;
        this.mapObject = getCustomMarkerOverlayView(this.el, this['position']);
        this.mapObject.setMap(this.nguiMapComponent.map);
        // set google events listeners and emits to this outputs listeners
        this.nguiMap.setObjectEvents(OUTPUTS$3, this, 'mapObject');
        // update object when input changes
        __WEBPACK_IMPORTED_MODULE_5_rxjs_operator_debounceTime__["a" /* debounceTime */].call(this.inputChanges$, 1000)
            .subscribe(function (changes) { return _this.nguiMap.updateGoogleObject(_this.mapObject, changes); });
        this.nguiMapComponent.addToMapObjectGroup('CustomMarker', this.mapObject);
        this.initialized$.emit(this.mapObject);
    };
    return CustomMarker;
}());
CustomMarker.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */], args: [{
                selector: 'ngui-map > custom-marker',
                inputs: INPUTS$3,
                outputs: OUTPUTS$3,
                template: "\n    <ng-content></ng-content>\n  ",
            },] },
];
/**
 * @nocollapse
 */
CustomMarker.ctorParameters = function () { return [
    { type: NguiMapComponent, },
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["v" /* ElementRef */], },
    { type: NguiMap, },
]; };
CustomMarker.propDecorators = {
    'initialized$': [{ type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["S" /* Output */] },],
};
var INPUTS$4 = [
    'center', 'clickable', 'draggable', 'editable', 'fillColor', 'fillOpacity', 'map', 'radius',
    'strokeColor', 'strokeOpacity', 'strokePosition', 'strokeWeight', 'visible', 'zIndex', 'options',
    // ngui-map specific inputs
    'geoFallbackCenter'
];
var OUTPUTS$4 = [
    'centerChanged', 'click', 'dblclick', 'drag', 'dragend', 'dragstart',
    'mousedown', 'mousemove', 'mouseout', 'mouseover', 'mouseup', 'radiusChanged', 'rightclick',
];
var Circle = /** @class */ (function (_super) {
    __extends(Circle, _super);
    /**
     * @param {?} nguiMapComp
     */
    function Circle(nguiMapComp) {
        var _this = _super.call(this, nguiMapComp, 'Circle', INPUTS$4, OUTPUTS$4) || this;
        _this.nguiMapComp = nguiMapComp;
        _this.objectOptions = ({});
        return _this;
    }
    /**
     * @return {?}
     */
    Circle.prototype.initialize = function () {
        _super.prototype.initialize.call(this);
        this.setCenter();
    };
    /**
     * @return {?}
     */
    Circle.prototype.setCenter = function () {
        var _this = this;
        if (!this['center']) {
            this._subscriptions.push(this.nguiMapComp.geolocation.getCurrentPosition().subscribe(function (center) {
                var /** @type {?} */ latLng = new google.maps.LatLng(center.coords.latitude, center.coords.longitude);
                _this.mapObject.setCenter(latLng);
            }, function (error) {
                console.error('ngui-map, error in finding the current position');
                _this.mapObject.setCenter(_this.objectOptions['geoFallbackCenter'] || new google.maps.LatLng(0, 0));
            }));
        }
        else if (typeof this['center'] === 'string') {
            this._subscriptions.push(this.nguiMapComp.geoCoder.geocode({ address: this['center'] }).subscribe(function (results) {
                _this.mapObject.setCenter(results[0].geometry.location);
            }, function (error) {
                console.error('ngui-map, error in finding location from', _this['center']);
                _this.mapObject.setCenter(_this.objectOptions['geoFallbackCenter'] || new google.maps.LatLng(0, 0));
            }));
        }
    };
    return Circle;
}(BaseMapDirective));
Circle.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */], args: [{
                selector: 'ngui-map>circle, ngui-map>map-circle',
                inputs: INPUTS$4,
                outputs: OUTPUTS$4,
            },] },
];
/**
 * @nocollapse
 */
Circle.ctorParameters = function () { return [
    { type: NguiMapComponent, },
]; };
var INPUTS$5 = ['controlPosition', 'controls', 'drawingMode', 'featureFactory', 'style', 'geoJson', 'geoJsonUrl'];
var OUTPUTS$5 = [
    'addfeature', 'click', 'dblclick', 'mousedown', 'mouseout', 'mouseover',
    'mouseup', 'removefeature', 'removeproperty', 'rightclick', 'setgeometry', 'setproperty'
];
var DataLayer = /** @class */ (function (_super) {
    __extends(DataLayer, _super);
    /**
     * @param {?} nguiMapComponent
     */
    function DataLayer(nguiMapComponent) {
        return _super.call(this, nguiMapComponent, 'Data', INPUTS$5, OUTPUTS$5) || this;
    }
    /**
     * @return {?}
     */
    DataLayer.prototype.initialize = function () {
        if (this['geoJson']) {
            // addGeoJson from an object
            this.nguiMapComponent.map.data.addGeoJson(this['geoJson']);
        }
        else if (this['geoJsonUrl']) {
            // loadGeoJson from a URL
            this.nguiMapComponent.map.data.loadGeoJson(this['geoJsonUrl']);
        }
        else {
            this.objectOptions = this.optionBuilder.googlizeAllInputs(this.inputs, this);
            this.nguiMapComponent.map.data.add(this.objectOptions);
        }
        // unlike others, data belongs to map. e.g., map.data.loadGeoJson(), map.data.add()
        this.mapObject = this.nguiMapComponent.map.data;
        // set google events listeners and emits to this outputs listeners
        this.nguiMap.setObjectEvents(this.outputs, this, 'mapObject');
        this.nguiMapComponent.addToMapObjectGroup(this.mapObjectName, this.mapObject);
        this.initialized$.emit(this.mapObject);
    };
    return DataLayer;
}(BaseMapDirective));
DataLayer.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */], args: [{
                selector: 'ngui-map > data-layer',
                inputs: INPUTS$5,
                outputs: OUTPUTS$5,
            },] },
];
/**
 * @nocollapse
 */
DataLayer.ctorParameters = function () { return [
    { type: NguiMapComponent, },
]; };
var INPUTS$6 = [
    'directions', 'draggable', 'hideRouteList', 'infoWindow', 'panel', 'markerOptions',
    'polylineOptions', 'preserveViewport', 'routeIndex', 'suppressBicyclingLayer',
    'suppressInfoWindows', 'suppressMarkers', 'suppressPolylines'
];
var OUTPUTS$6 = ['directions_changed'];
var DirectionsRenderer = /** @class */ (function (_super) {
    __extends(DirectionsRenderer, _super);
    /**
     * @param {?} nguiMapComponent
     * @param {?} geolocation
     */
    function DirectionsRenderer(nguiMapComponent, geolocation) {
        var _this = _super.call(this, nguiMapComponent, 'DirectionsRenderer', INPUTS$6, OUTPUTS$6) || this;
        _this.geolocation = geolocation;
        return _this;
    }
    /**
     * @return {?}
     */
    DirectionsRenderer.prototype.initialize = function () {
        this.objectOptions = this.optionBuilder.googlizeAllInputs(this.inputs, this);
        if (typeof this.objectOptions['panel'] === 'string') {
            this.objectOptions['panel'] = document.querySelector(this.objectOptions['panel']);
        }
        this.directionsService = new google.maps.DirectionsService();
        this.directionsRenderer = new google.maps.DirectionsRenderer(this.objectOptions);
        this.directionsRenderer.setMap(this.nguiMapComponent.map);
        // set google events listeners and emidirectionsRenderer to this outputs listeners
        this.showDirections(this.directionsRequest);
        this.nguiMap.setObjectEvents(this.outputs, this, 'directionsRenderer');
        this.nguiMapComponent.addToMapObjectGroup(this.mapObjectName, this.mapObject);
        this.initialized$.emit(this.directionsRenderer);
    };
    /**
     * @param {?} changes
     * @return {?}
     */
    DirectionsRenderer.prototype.ngOnChanges = function (changes) {
        var /** @type {?} */ newOptions = {};
        for (var /** @type {?} */ key in changes) {
            if (this.inputs.indexOf(key) !== -1) {
                newOptions[key] = this.optionBuilder.googlize(changes[key].currentValue);
            }
        }
        if (changes['directionsRequest'] && this.directionsRenderer) {
            this.directionsService && this.showDirections(this.directionsRequest);
        }
    };
    /**
     * @param {?} directionsRequest
     * @return {?}
     */
    DirectionsRenderer.prototype.showDirections = function (directionsRequest) {
        var _this = this;
        this.directionsService.route(directionsRequest, function (response, status) {
            // in some-case the callback is called during destroy component,
            // we should make sure directionsRenderer is still defined (cancelling `route` callback is not possible).
            if (!_this.directionsRenderer) {
                return;
            }
            if (status === google.maps.DirectionsStatus.OK) {
                _this.directionsRenderer.setDirections(response);
            }
            else {
                console.error('Directions request failed due to ' + status);
            }
        });
    };
    /**
     * @return {?}
     */
    DirectionsRenderer.prototype.ngOnDestroy = function () {
        _super.prototype.ngOnDestroy.call(this);
        this.nguiMap.clearObjectEvents(this.outputs, this, 'directionsRenderer');
    };
    return DirectionsRenderer;
}(BaseMapDirective));
DirectionsRenderer.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */], args: [{
                selector: 'ngui-map > directions-renderer',
                inputs: INPUTS$6,
                outputs: OUTPUTS$6,
            },] },
];
/**
 * @nocollapse
 */
DirectionsRenderer.ctorParameters = function () { return [
    { type: NguiMapComponent, },
    { type: NavigatorGeolocation, },
]; };
DirectionsRenderer.propDecorators = {
    'directionsRequest': [{ type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["G" /* Input */], args: ['directions-request',] },],
};
var INPUTS$7 = [
    'options',
    'circleOptions', 'drawingControl', 'drawingControlOptions', 'drawingMode',
    'map', 'markerOptions', 'polygonOptions', 'polylineOptions', 'rectangleOptions'
];
var OUTPUTS$7 = [
    'circlecomplete', 'markercomplete', 'overlaycomplete',
    'polygoncomplete', 'polylinecomplete', 'rectanglecomplete'
];
var DrawingManager = /** @class */ (function (_super) {
    __extends(DrawingManager, _super);
    /**
     * @param {?} nguiMapComp
     */
    function DrawingManager(nguiMapComp) {
        var _this = _super.call(this, nguiMapComp, 'DrawingManager', INPUTS$7, OUTPUTS$7) || this;
        _this.libraryName = 'drawing';
        return _this;
    }
    return DrawingManager;
}(BaseMapDirective));
DrawingManager.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */], args: [{
                selector: 'ngui-map > drawing-manager',
                inputs: INPUTS$7,
                outputs: OUTPUTS$7,
            },] },
];
/**
 * @nocollapse
 */
DrawingManager.ctorParameters = function () { return [
    { type: NguiMapComponent, },
]; };
var INPUTS$8 = ['url', 'bounds', 'clickable', 'opacity'];
var OUTPUTS$8 = ['click', 'dblclick'];
var GroundOverlay = /** @class */ (function (_super) {
    __extends(GroundOverlay, _super);
    /**
     * @param {?} nguiMapComp
     */
    function GroundOverlay(nguiMapComp) {
        var _this = _super.call(this, nguiMapComp, 'GroundOverlay', INPUTS$8, OUTPUTS$8) || this;
        _this.objectOptions = ({});
        return _this;
    }
    /**
     * @return {?}
     */
    GroundOverlay.prototype.initialize = function () {
        // url, bounds are not the options of GroundOverlay
        this.objectOptions = this.optionBuilder.googlizeAllInputs(['clickable', 'opacity'], this);
        // noinspection TypeScriptUnresolvedFunction
        this.mapObject = new google.maps.GroundOverlay(this['url'], this['bounds'], this.objectOptions);
        this.mapObject.setMap(this.nguiMapComponent.map);
        this.mapObject['mapObjectName'] = this.mapObjectName;
        // set google events listeners and emits to this outputs listeners
        this.nguiMap.setObjectEvents(this.outputs, this, 'mapObject');
        this.nguiMapComponent.addToMapObjectGroup(this.mapObjectName, this.mapObject);
        this.initialized$.emit(this.mapObject);
    };
    return GroundOverlay;
}(BaseMapDirective));
GroundOverlay.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */], args: [{
                selector: 'ngui-map > ground-overlay',
                inputs: INPUTS$8,
                outputs: OUTPUTS$8,
            },] },
];
/**
 * @nocollapse
 */
GroundOverlay.ctorParameters = function () { return [
    { type: NguiMapComponent, },
]; };
var INPUTS$9 = ['data', 'dissipating', 'gradient', 'maxIntensity', 'opacity', 'radius', 'options'];
var OUTPUTS$9 = [];
var HeatmapLayer = /** @class */ (function (_super) {
    __extends(HeatmapLayer, _super);
    /**
     * @param {?} nguiMapComp
     */
    function HeatmapLayer(nguiMapComp) {
        var _this = _super.call(this, nguiMapComp, 'HeatmapLayer', INPUTS$9, OUTPUTS$9) || this;
        _this.libraryName = 'visualization';
        return _this;
    }
    return HeatmapLayer;
}(BaseMapDirective));
HeatmapLayer.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */], args: [{
                selector: 'ngui-map > heatmap-layer',
                inputs: INPUTS$9,
                outputs: OUTPUTS$9,
            },] },
];
/**
 * @nocollapse
 */
HeatmapLayer.ctorParameters = function () { return [
    { type: NguiMapComponent, },
]; };
var INPUTS$10 = ['clickable', 'preserveViewport', 'screenOverlays', 'suppressInfoWindows', 'url', 'zIndex', 'options'];
var OUTPUTS$10 = ['click', 'defaultviewport_changed', 'status_changed'];
var KmlLayer = /** @class */ (function (_super) {
    __extends(KmlLayer, _super);
    /**
     * @param {?} nguiMapComp
     */
    function KmlLayer(nguiMapComp) {
        return _super.call(this, nguiMapComp, 'KmlLayer', INPUTS$10, OUTPUTS$10) || this;
    }
    return KmlLayer;
}(BaseMapDirective));
KmlLayer.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */], args: [{
                selector: 'ngui-map > kml-layer',
                inputs: INPUTS$10,
                outputs: OUTPUTS$10,
            },] },
];
/**
 * @nocollapse
 */
KmlLayer.ctorParameters = function () { return [
    { type: NguiMapComponent, },
]; };
var INPUTS$11 = [
    'anchorPoint', 'animation', 'clickable', 'cursor', 'draggable', 'icon', 'label', 'opacity',
    'optimized', 'place', 'position', 'shape', 'title', 'visible', 'zIndex', 'options',
    // ngui-map specific inputs
    'geoFallbackPosition'
];
var OUTPUTS$11 = [
    'animationChanged', 'click', 'clickableChanged', 'cursorChanged', 'dblclick', 'drag', 'dragend', 'draggableChanged',
    'dragstart', 'flatChanged', 'iconChanged', 'mousedown', 'mouseout', 'mouseover', 'mouseup', 'positionChanged', 'rightclick',
    'shapeChanged', 'titleChanged', 'visibleChanged', 'zindexChanged'
];
var Marker = /** @class */ (function (_super) {
    __extends(Marker, _super);
    /**
     * @param {?} nguiMapComp
     */
    function Marker(nguiMapComp) {
        var _this = _super.call(this, nguiMapComp, 'Marker', INPUTS$11, OUTPUTS$11) || this;
        _this.nguiMapComp = nguiMapComp;
        _this.objectOptions = ({});
        return _this;
    }
    /**
     * @return {?}
     */
    Marker.prototype.ngOnInit = function () {
        var _this = this;
        if (this.nguiMapComponent.mapIdledOnce) {
            this.initialize();
        }
        else {
            this.nguiMapComponent.mapReady$.subscribe(function (map) { return _this.initialize(); });
        }
    };
    /**
     * @return {?}
     */
    Marker.prototype.initialize = function () {
        _super.prototype.initialize.call(this);
        this.setPosition();
    };
    /**
     * @return {?}
     */
    Marker.prototype.setPosition = function () {
        var _this = this;
        if (!this['position']) {
            this._subscriptions.push(this.nguiMapComp.geolocation.getCurrentPosition().subscribe(function (position) {
                var /** @type {?} */ latLng = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
                _this.mapObject.setPosition(latLng);
            }, function (error) {
                console.error('ngui-map, error finding the current location');
                _this.mapObject.setPosition(_this.objectOptions['geoFallbackPosition'] || new google.maps.LatLng(0, 0));
            }));
        }
        else if (typeof this['position'] === 'string') {
            this._subscriptions.push(this.nguiMapComp.geoCoder.geocode({ address: this['position'] }).subscribe(function (results) {
                _this.mapObject.setPosition(results[0].geometry.location);
            }, function (error) {
                console.error('ngui-map, error finding the location from', _this['position']);
                _this.mapObject.setPosition(_this.objectOptions['geoFallbackPosition'] || new google.maps.LatLng(0, 0));
            }));
        }
    };
    return Marker;
}(BaseMapDirective));
Marker.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */], args: [{
                selector: 'ngui-map > marker',
                inputs: INPUTS$11,
                outputs: OUTPUTS$11,
            },] },
];
/**
 * @nocollapse
 */
Marker.ctorParameters = function () { return [
    { type: NguiMapComponent, },
]; };
var PlacesAutoComplete = /** @class */ (function () {
    /**
     * @param {?} optionBuilder
     * @param {?} elementRef
     * @param {?} apiLoader
     */
    function PlacesAutoComplete(optionBuilder, elementRef, apiLoader) {
        var _this = this;
        this.optionBuilder = optionBuilder;
        this.elementRef = elementRef;
        this.apiLoader = apiLoader;
        this.place_changed = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["y" /* EventEmitter */]();
        this.initialized$ = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["y" /* EventEmitter */]();
        // only called when map is ready
        this.initialize = function () {
            _this.objectOptions =
                _this.optionBuilder.googlizeAllInputs(['bounds', 'componentRestrictions', 'types'], _this);
            if (!google.maps.places) {
                throw missingLibraryError('PlacesAutoComplete', 'places');
            }
            _this.autocomplete = new google.maps.places.Autocomplete(_this.elementRef.nativeElement, _this.objectOptions);
            _this.autocomplete.addListener('place_changed', function (place) {
                _this.place_changed.emit(_this.autocomplete.getPlace());
            });
            _this.initialized$.emit(_this.autocomplete);
        };
        apiLoader.load();
        apiLoader.api$.subscribe(function () { return _this.initialize(); });
    }
    return PlacesAutoComplete;
}());
PlacesAutoComplete.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */], args: [{
                selector: '[places-auto-complete]'
            },] },
];
/**
 * @nocollapse
 */
PlacesAutoComplete.ctorParameters = function () { return [
    { type: OptionBuilder, },
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["v" /* ElementRef */], },
    { type: NgMapApiLoader, },
]; };
PlacesAutoComplete.propDecorators = {
    'bounds': [{ type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["G" /* Input */], args: ['bounds',] },],
    'componentRestrictions': [{ type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["G" /* Input */], args: ['componentRestrictions',] },],
    'types': [{ type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["G" /* Input */], args: ['types',] },],
    'place_changed': [{ type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["S" /* Output */], args: ['place_changed',] },],
    'initialized$': [{ type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["S" /* Output */] },],
};
var INPUTS$12 = [
    'clickable', 'draggable', 'editable', 'fillColor', 'fillOpacity', 'geodesic', 'paths',
    'strokeColor', 'strokeOpacity', 'strokePosition', 'strokeWeight', 'visible', 'zIndex', 'options',
];
var OUTPUTS$12 = [
    'click', 'dblclick', 'drag', 'dragend', 'dragstart', 'mousedown',
    'mousemove', 'mouseout', 'mouseover', 'mouseup', 'rightclick',
];
var Polygon = /** @class */ (function (_super) {
    __extends(Polygon, _super);
    /**
     * @param {?} nguiMapComp
     */
    function Polygon(nguiMapComp) {
        return _super.call(this, nguiMapComp, 'Polygon', INPUTS$12, OUTPUTS$12) || this;
    }
    return Polygon;
}(BaseMapDirective));
Polygon.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */], args: [{
                selector: 'ngui-map>polygon, ngui-map>map-polygon',
                inputs: INPUTS$12,
                outputs: OUTPUTS$12,
            },] },
];
/**
 * @nocollapse
 */
Polygon.ctorParameters = function () { return [
    { type: NguiMapComponent, },
]; };
var INPUTS$13 = [
    'clickable', 'draggable', 'editable', 'geodesic', 'icons', 'path', 'strokeColor',
    'strokeOpacity', 'strokeWeight', 'visible', 'zIndex', 'options'
];
var OUTPUTS$13 = [
    'click', 'dblclick', 'drag', 'dragend', 'dragstart', 'mousedown',
    'mousemove', 'mouseout', 'mouseover', 'mouseup', 'rightclick'
];
var Polyline = /** @class */ (function (_super) {
    __extends(Polyline, _super);
    /**
     * @param {?} nguiMapComp
     */
    function Polyline(nguiMapComp) {
        return _super.call(this, nguiMapComp, 'Polyline', INPUTS$13, OUTPUTS$13) || this;
    }
    return Polyline;
}(BaseMapDirective));
Polyline.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */], args: [{
                selector: 'ngui-map > polyline',
                inputs: INPUTS$13,
                outputs: OUTPUTS$13,
            },] },
];
/**
 * @nocollapse
 */
Polyline.ctorParameters = function () { return [
    { type: NguiMapComponent, },
]; };
var INPUTS$14 = [
    'selector', 'options',
    'addressControl', 'addressControlOptions', 'clickToGo', 'disableDefaultUI', 'disableDoubleClickZoom',
    'enableCloseButton', 'fullscreenControl', 'fullscreenControlOptions', 'imageDateControl', 'linksControl',
    'motionTracking', 'motionTrackingControl', 'panControl', 'panControlOptions', 'pano',
    'position', 'pov', 'scrollwheel', 'showRoadLabels', 'visible', 'zoomControl', 'zoomControlOptions'
];
var OUTPUTS$14 = [
    'closeclick', 'pano_changed', 'position_changed', 'pov_changed', 'resize', 'status_changed',
    'visible_changed', 'zoom_changed'
];
var StreetViewPanorama = /** @class */ (function (_super) {
    __extends(StreetViewPanorama, _super);
    /**
     * @param {?} nguiMapComp
     */
    function StreetViewPanorama(nguiMapComp) {
        return _super.call(this, nguiMapComp, 'StreetViewPanorama', INPUTS$14, OUTPUTS$14) || this;
    }
    /**
     * @return {?}
     */
    StreetViewPanorama.prototype.initialize = function () {
        this.objectOptions = this.optionBuilder.googlizeAllInputs(this.inputs, this);
        var /** @type {?} */ element;
        if (this.objectOptions.selector) {
            // noinspection TypeScriptValidateTypes
            element = document.querySelector(this['selector']);
            delete this.objectOptions.selector;
        }
        else {
            element = this.nguiMapComponent.el;
        }
        // will be set after geocoded
        typeof this.objectOptions.position === 'string' && (delete this.objectOptions.position);
        this.mapObject = new google.maps[this.mapObjectName](element, this.objectOptions);
        this.mapObject['mapObjectName'] = this.mapObjectName;
        this.mapObject['nguiMapComponent'] = this.nguiMapComponent;
        // set google events listeners and emits to this outputs listeners
        this.nguiMap.setObjectEvents(this.outputs, this, 'mapObject');
        this.nguiMapComponent.addToMapObjectGroup(this.mapObjectName, this.mapObject);
        this.initialized$.emit(this.mapObject);
    };
    /**
     * @return {?}
     */
    StreetViewPanorama.prototype.ngOnDestroy = function () {
        if (this.nguiMapComponent.el) {
            this.nguiMap.clearObjectEvents(this.outputs, this, 'mapObject');
        }
    };
    return StreetViewPanorama;
}(BaseMapDirective));
StreetViewPanorama.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */], args: [{
                selector: 'ngui-map > street-view-panorama',
                inputs: INPUTS$14,
                outputs: OUTPUTS$14,
            },] },
];
/**
 * @nocollapse
 */
StreetViewPanorama.ctorParameters = function () { return [
    { type: NguiMapComponent, },
]; };
var INPUTS$15 = ['autoRefresh', 'options'];
var OUTPUTS$15 = [];
var TrafficLayer = /** @class */ (function (_super) {
    __extends(TrafficLayer, _super);
    /**
     * @param {?} nguiMapComp
     */
    function TrafficLayer(nguiMapComp) {
        return _super.call(this, nguiMapComp, 'TrafficLayer', INPUTS$15, OUTPUTS$15) || this;
    }
    return TrafficLayer;
}(BaseMapDirective));
TrafficLayer.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */], args: [{
                selector: 'ngui-map > traffic-layer',
                inputs: INPUTS$15,
                outputs: OUTPUTS$15,
            },] },
];
/**
 * @nocollapse
 */
TrafficLayer.ctorParameters = function () { return [
    { type: NguiMapComponent, },
]; };
var INPUTS$16 = [];
var OUTPUTS$16 = [];
var TransitLayer = /** @class */ (function (_super) {
    __extends(TransitLayer, _super);
    /**
     * @param {?} nguiMapComp
     */
    function TransitLayer(nguiMapComp) {
        return _super.call(this, nguiMapComp, 'TransitLayer', INPUTS$16, OUTPUTS$16) || this;
    }
    return TransitLayer;
}(BaseMapDirective));
TransitLayer.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */], args: [{
                selector: 'ngui-map > transit-layer',
                inputs: INPUTS$16,
                outputs: OUTPUTS$16,
            },] },
];
/**
 * @nocollapse
 */
TransitLayer.ctorParameters = function () { return [
    { type: NguiMapComponent, },
]; };
var COMPONENTS_DIRECTIVES = [
    NguiMapComponent, InfoWindow,
    Marker, Circle, CustomMarker, Polygon, InfoWindow, Polyline, GroundOverlay,
    TransitLayer, TrafficLayer, HeatmapLayer, BicyclingLayer, KmlLayer, DataLayer,
    StreetViewPanorama, PlacesAutoComplete, DirectionsRenderer,
    DrawingManager,
];
var NguiMapModule = /** @class */ (function () {
    function NguiMapModule() {
    }
    /**
     * @param {?=} config
     * @return {?}
     */
    NguiMapModule.forRoot = function (config) {
        if (config === void 0) { config = {}; }
        return {
            ngModule: NguiMapModule,
            providers: [
                { provide: NG_MAP_CONFIG_TOKEN, useValue: config }
            ],
        };
    };
    return NguiMapModule;
}());
NguiMapModule.decorators = [
    { type: __WEBPACK_IMPORTED_MODULE_0__angular_core__["L" /* NgModule */], args: [{
                imports: [__WEBPACK_IMPORTED_MODULE_6__angular_common__["b" /* CommonModule */]],
                declarations: COMPONENTS_DIRECTIVES,
                exports: [COMPONENTS_DIRECTIVES],
                providers: [
                    GeoCoder,
                    NavigatorGeolocation,
                    NguiMap,
                    OptionBuilder,
                    { provide: NgMapApiLoader, useClass: NgMapAsyncCallbackApiLoader },
                ]
            },] },
];
/**
 * @nocollapse
 */
NguiMapModule.ctorParameters = function () { return []; };
/**
 * Generated bundle index. Do not edit.
 */

//# sourceMappingURL=map.es5.js.map


/***/ }),

/***/ "../../../../rxjs/_esm5/ReplaySubject.js":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ReplaySubject; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__Subject__ = __webpack_require__("../../../../rxjs/_esm5/Subject.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__scheduler_queue__ = __webpack_require__("../../../../rxjs/_esm5/scheduler/queue.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__Subscription__ = __webpack_require__("../../../../rxjs/_esm5/Subscription.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__operators_observeOn__ = __webpack_require__("../../../../rxjs/_esm5/operators/observeOn.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__util_ObjectUnsubscribedError__ = __webpack_require__("../../../../rxjs/_esm5/util/ObjectUnsubscribedError.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__SubjectSubscription__ = __webpack_require__("../../../../rxjs/_esm5/SubjectSubscription.js");
/** PURE_IMPORTS_START ._Subject,._scheduler_queue,._Subscription,._operators_observeOn,._util_ObjectUnsubscribedError,._SubjectSubscription PURE_IMPORTS_END */
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b)
        if (b.hasOwnProperty(p))
            d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};






/**
 * @class ReplaySubject<T>
 */
var ReplaySubject = /*@__PURE__*/ (/*@__PURE__*/ function (_super) {
    __extends(ReplaySubject, _super);
    function ReplaySubject(bufferSize, windowTime, scheduler) {
        if (bufferSize === void 0) {
            bufferSize = Number.POSITIVE_INFINITY;
        }
        if (windowTime === void 0) {
            windowTime = Number.POSITIVE_INFINITY;
        }
        _super.call(this);
        this.scheduler = scheduler;
        this._events = [];
        this._bufferSize = bufferSize < 1 ? 1 : bufferSize;
        this._windowTime = windowTime < 1 ? 1 : windowTime;
    }
    ReplaySubject.prototype.next = function (value) {
        var now = this._getNow();
        this._events.push(new ReplayEvent(now, value));
        this._trimBufferThenGetEvents();
        _super.prototype.next.call(this, value);
    };
    ReplaySubject.prototype._subscribe = function (subscriber) {
        var _events = this._trimBufferThenGetEvents();
        var scheduler = this.scheduler;
        var subscription;
        if (this.closed) {
            throw new __WEBPACK_IMPORTED_MODULE_4__util_ObjectUnsubscribedError__["a" /* ObjectUnsubscribedError */]();
        }
        else if (this.hasError) {
            subscription = __WEBPACK_IMPORTED_MODULE_2__Subscription__["a" /* Subscription */].EMPTY;
        }
        else if (this.isStopped) {
            subscription = __WEBPACK_IMPORTED_MODULE_2__Subscription__["a" /* Subscription */].EMPTY;
        }
        else {
            this.observers.push(subscriber);
            subscription = new __WEBPACK_IMPORTED_MODULE_5__SubjectSubscription__["a" /* SubjectSubscription */](this, subscriber);
        }
        if (scheduler) {
            subscriber.add(subscriber = new __WEBPACK_IMPORTED_MODULE_3__operators_observeOn__["a" /* ObserveOnSubscriber */](subscriber, scheduler));
        }
        var len = _events.length;
        for (var i = 0; i < len && !subscriber.closed; i++) {
            subscriber.next(_events[i].value);
        }
        if (this.hasError) {
            subscriber.error(this.thrownError);
        }
        else if (this.isStopped) {
            subscriber.complete();
        }
        return subscription;
    };
    ReplaySubject.prototype._getNow = function () {
        return (this.scheduler || __WEBPACK_IMPORTED_MODULE_1__scheduler_queue__["a" /* queue */]).now();
    };
    ReplaySubject.prototype._trimBufferThenGetEvents = function () {
        var now = this._getNow();
        var _bufferSize = this._bufferSize;
        var _windowTime = this._windowTime;
        var _events = this._events;
        var eventsCount = _events.length;
        var spliceCount = 0;
        // Trim events that fall out of the time window.
        // Start at the front of the list. Break early once
        // we encounter an event that falls within the window.
        while (spliceCount < eventsCount) {
            if ((now - _events[spliceCount].time) < _windowTime) {
                break;
            }
            spliceCount++;
        }
        if (eventsCount > _bufferSize) {
            spliceCount = Math.max(spliceCount, eventsCount - _bufferSize);
        }
        if (spliceCount > 0) {
            _events.splice(0, spliceCount);
        }
        return _events;
    };
    return ReplaySubject;
}(__WEBPACK_IMPORTED_MODULE_0__Subject__["a" /* Subject */]));
var ReplayEvent = /*@__PURE__*/ (/*@__PURE__*/ function () {
    function ReplayEvent(time, value) {
        this.time = time;
        this.value = value;
    }
    return ReplayEvent;
}());
//# sourceMappingURL=ReplaySubject.js.map


/***/ }),

/***/ "../../../../rxjs/_esm5/operator/debounceTime.js":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = debounceTime;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__scheduler_async__ = __webpack_require__("../../../../rxjs/_esm5/scheduler/async.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__operators_debounceTime__ = __webpack_require__("../../../../rxjs/_esm5/operators/debounceTime.js");
/** PURE_IMPORTS_START .._scheduler_async,.._operators_debounceTime PURE_IMPORTS_END */


/**
 * Emits a value from the source Observable only after a particular time span
 * has passed without another source emission.
 *
 * <span class="informal">It's like {@link delay}, but passes only the most
 * recent value from each burst of emissions.</span>
 *
 * <img src="./img/debounceTime.png" width="100%">
 *
 * `debounceTime` delays values emitted by the source Observable, but drops
 * previous pending delayed emissions if a new value arrives on the source
 * Observable. This operator keeps track of the most recent value from the
 * source Observable, and emits that only when `dueTime` enough time has passed
 * without any other value appearing on the source Observable. If a new value
 * appears before `dueTime` silence occurs, the previous value will be dropped
 * and will not be emitted on the output Observable.
 *
 * This is a rate-limiting operator, because it is impossible for more than one
 * value to be emitted in any time window of duration `dueTime`, but it is also
 * a delay-like operator since output emissions do not occur at the same time as
 * they did on the source Observable. Optionally takes a {@link IScheduler} for
 * managing timers.
 *
 * @example <caption>Emit the most recent click after a burst of clicks</caption>
 * var clicks = Rx.Observable.fromEvent(document, 'click');
 * var result = clicks.debounceTime(1000);
 * result.subscribe(x => console.log(x));
 *
 * @see {@link auditTime}
 * @see {@link debounce}
 * @see {@link delay}
 * @see {@link sampleTime}
 * @see {@link throttleTime}
 *
 * @param {number} dueTime The timeout duration in milliseconds (or the time
 * unit determined internally by the optional `scheduler`) for the window of
 * time required to wait for emission silence before emitting the most recent
 * source value.
 * @param {Scheduler} [scheduler=async] The {@link IScheduler} to use for
 * managing the timers that handle the timeout for each value.
 * @return {Observable} An Observable that delays the emissions of the source
 * Observable by the specified `dueTime`, and may drop some values if they occur
 * too frequently.
 * @method debounceTime
 * @owner Observable
 */
function debounceTime(dueTime, scheduler) {
    if (scheduler === void 0) {
        scheduler = __WEBPACK_IMPORTED_MODULE_0__scheduler_async__["a" /* async */];
    }
    return Object(__WEBPACK_IMPORTED_MODULE_1__operators_debounceTime__["a" /* debounceTime */])(dueTime, scheduler)(this);
}
//# sourceMappingURL=debounceTime.js.map


/***/ }),

/***/ "../../../../rxjs/_esm5/scheduler/QueueAction.js":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return QueueAction; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__AsyncAction__ = __webpack_require__("../../../../rxjs/_esm5/scheduler/AsyncAction.js");
/** PURE_IMPORTS_START ._AsyncAction PURE_IMPORTS_END */
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b)
        if (b.hasOwnProperty(p))
            d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};

/**
 * We need this JSDoc comment for affecting ESDoc.
 * @ignore
 * @extends {Ignored}
 */
var QueueAction = /*@__PURE__*/ (/*@__PURE__*/ function (_super) {
    __extends(QueueAction, _super);
    function QueueAction(scheduler, work) {
        _super.call(this, scheduler, work);
        this.scheduler = scheduler;
        this.work = work;
    }
    QueueAction.prototype.schedule = function (state, delay) {
        if (delay === void 0) {
            delay = 0;
        }
        if (delay > 0) {
            return _super.prototype.schedule.call(this, state, delay);
        }
        this.delay = delay;
        this.state = state;
        this.scheduler.flush(this);
        return this;
    };
    QueueAction.prototype.execute = function (state, delay) {
        return (delay > 0 || this.closed) ?
            _super.prototype.execute.call(this, state, delay) :
            this._execute(state, delay);
    };
    QueueAction.prototype.requestAsyncId = function (scheduler, id, delay) {
        if (delay === void 0) {
            delay = 0;
        }
        // If delay exists and is greater than 0, or if the delay is null (the
        // action wasn't rescheduled) but was originally scheduled as an async
        // action, then recycle as an async action.
        if ((delay !== null && delay > 0) || (delay === null && this.delay > 0)) {
            return _super.prototype.requestAsyncId.call(this, scheduler, id, delay);
        }
        // Otherwise flush the scheduler starting with this action.
        return scheduler.flush(this);
    };
    return QueueAction;
}(__WEBPACK_IMPORTED_MODULE_0__AsyncAction__["a" /* AsyncAction */]));
//# sourceMappingURL=QueueAction.js.map


/***/ }),

/***/ "../../../../rxjs/_esm5/scheduler/QueueScheduler.js":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return QueueScheduler; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__AsyncScheduler__ = __webpack_require__("../../../../rxjs/_esm5/scheduler/AsyncScheduler.js");
/** PURE_IMPORTS_START ._AsyncScheduler PURE_IMPORTS_END */
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b)
        if (b.hasOwnProperty(p))
            d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};

var QueueScheduler = /*@__PURE__*/ (/*@__PURE__*/ function (_super) {
    __extends(QueueScheduler, _super);
    function QueueScheduler() {
        _super.apply(this, arguments);
    }
    return QueueScheduler;
}(__WEBPACK_IMPORTED_MODULE_0__AsyncScheduler__["a" /* AsyncScheduler */]));
//# sourceMappingURL=QueueScheduler.js.map


/***/ }),

/***/ "../../../../rxjs/_esm5/scheduler/queue.js":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return queue; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__QueueAction__ = __webpack_require__("../../../../rxjs/_esm5/scheduler/QueueAction.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__QueueScheduler__ = __webpack_require__("../../../../rxjs/_esm5/scheduler/QueueScheduler.js");
/** PURE_IMPORTS_START ._QueueAction,._QueueScheduler PURE_IMPORTS_END */


/**
 *
 * Queue Scheduler
 *
 * <span class="informal">Put every next task on a queue, instead of executing it immediately</span>
 *
 * `queue` scheduler, when used with delay, behaves the same as {@link async} scheduler.
 *
 * When used without delay, it schedules given task synchronously - executes it right when
 * it is scheduled. However when called recursively, that is when inside the scheduled task,
 * another task is scheduled with queue scheduler, instead of executing immediately as well,
 * that task will be put on a queue and wait for current one to finish.
 *
 * This means that when you execute task with `queue` scheduler, you are sure it will end
 * before any other task scheduled with that scheduler will start.
 *
 * @examples <caption>Schedule recursively first, then do something</caption>
 *
 * Rx.Scheduler.queue.schedule(() => {
 *   Rx.Scheduler.queue.schedule(() => console.log('second')); // will not happen now, but will be put on a queue
 *
 *   console.log('first');
 * });
 *
 * // Logs:
 * // "first"
 * // "second"
 *
 *
 * @example <caption>Reschedule itself recursively</caption>
 *
 * Rx.Scheduler.queue.schedule(function(state) {
 *   if (state !== 0) {
 *     console.log('before', state);
 *     this.schedule(state - 1); // `this` references currently executing Action,
 *                               // which we reschedule with new state
 *     console.log('after', state);
 *   }
 * }, 0, 3);
 *
 * // In scheduler that runs recursively, you would expect:
 * // "before", 3
 * // "before", 2
 * // "before", 1
 * // "after", 1
 * // "after", 2
 * // "after", 3
 *
 * // But with queue it logs:
 * // "before", 3
 * // "after", 3
 * // "before", 2
 * // "after", 2
 * // "before", 1
 * // "after", 1
 *
 *
 * @static true
 * @name queue
 * @owner Scheduler
 */
var queue = /*@__PURE__*/ new __WEBPACK_IMPORTED_MODULE_1__QueueScheduler__["a" /* QueueScheduler */](__WEBPACK_IMPORTED_MODULE_0__QueueAction__["a" /* QueueAction */]);
//# sourceMappingURL=queue.js.map


/***/ })

});
//# sourceMappingURL=maps.module.chunk.js.map