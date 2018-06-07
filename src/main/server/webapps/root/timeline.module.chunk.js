webpackJsonp(["timeline.module"],{

/***/ "./src/app/timeline/timeline.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"main-content\">\n    <div class=\"container-fluid\">\n        <div class=\"row\">\n            <div class=\"col-md-12\">\n                <div class=\"card card-timeline card-plain\">\n                    <div class=\"card-header\">\n                        <h4 class=\"card-title\">Timeline</h4>\n                    </div>\n                    <div class=\"card-content\">\n                        <ul class=\"timeline\">\n                            <li class=\"timeline-inverted\">\n                                <div class=\"timeline-badge danger\">\n                                  <i class=\"ti-gallery\"></i>\n                                </div>\n                                <div class=\"timeline-panel\">\n                                    <div class=\"timeline-heading\">\n                                      <span class=\"label label-danger\">Some Title</span>\n                                    </div>\n                                    <div class=\"timeline-body\">\n                                        <p>Wifey made the best Father's Day meal ever. So thankful so happy so blessed. Thank you for making my family We just had fun with the “future” theme !!!   It was a fun night all together ... The always rude Kanye Show at 2am Sold Out Famous viewing @ Figueroa and 12th in downtown.</p>\n                                    </div>\n                                    <h6>\n                                        <i class=\"ti-time\"></i>\n                                        11 hours ago via Twitter\n                                    </h6>\n                              </div>\n                            </li>\n                            <li>\n                                <div class=\"timeline-badge success\">\n                                    <i class=\"ti-check-box\"></i>\n                                </div>\n                                <div class=\"timeline-panel\">\n                                    <div class=\"timeline-heading\">\n                                      <span class=\"label label-success\">Another One</span>\n                                    </div>\n                                    <div class=\"timeline-body\">\n                                        <p>Thank God for the support of my wife and real friends. I also wanted to point out that it’s the first album to go number 1 off of streaming!!! I love you Ellen and also my number one design rule of anything I do from shoes to music to homes is that Kim has to like it....</p>\n                                    </div>\n                                </div>\n                            </li>\n                            <li class=\"timeline-inverted\">\n                                <div class=\"timeline-badge info\">\n                                    <i class=\"ti-credit-card\"></i>\n                                </div>\n                                <div class=\"timeline-panel\">\n                                    <div class=\"timeline-heading\">\n                                      <span class=\"label label-info\">Another Title</span>\n                                    </div>\n                                    <div class=\"timeline-body\">\n                                        <p>Called I Miss the Old Kanye That’s all it was Kanye And I love you like Kanye loves Kanye Famous viewing @ Figueroa and 12th in downtown LA 11:10PM</p>\n                                        <p>What if Kanye made a song about Kanye Royère doesn't make a Polar bear bed but the Polar bear couch is my favorite piece of furniture we own It wasn’t any Kanyes Set on his goals Kanye</p>\n                                        <hr>\n                                        <div class=\"btn-group\">\n                                            <button type=\"button\" class=\"btn btn-info dropdown-toggle\" data-toggle=\"dropdown\">\n                                              <i class=\"ti-settings\"></i> <span class=\"caret\"></span>\n                                            </button>\n                                            <ul class=\"dropdown-menu dropdown-menu-left\" role=\"menu\">\n                                              <li><a href=\"#action\">Action</a></li>\n                                              <li><a href=\"#another\">Another action</a></li>\n                                              <li><a href=\"#else\">Something else here</a></li>\n                                              <li class=\"divider\"></li>\n                                              <li><a href=\"#link\">Separated link</a></li>\n                                            </ul>\n                                        </div>\n                                    </div>\n                                </div>\n                            </li>\n                            <li>\n                                <div class=\"timeline-badge warning\">\n                                  <i class=\"ti-gallery\"></i>\n                                </div>\n                                <div class=\"timeline-panel\">\n                                    <div class=\"timeline-heading\">\n                                      <span class=\"label label-warning\">Another One</span>\n                                    </div>\n                                    <div class=\"timeline-body\">\n                                        <p>Tune into Big Boy's 92.3 I'm about to play the first single from Cruel Winter Tune into Big Boy's 92.3 I'm about to play the first single from Cruel Winter also to Kim’s hair and makeup Lorraine jewelry and the whole style squad at Balmain and the Yeezy team. Thank you Anna for the invite thank you to the whole Vogue team</p>\n                                    </div>\n                                </div>\n                            </li>\n                        </ul>\n                    </div>\n                </div>\n            </div>\n        </div>\n    </div>\n</div>\n"

/***/ }),

/***/ "./src/app/timeline/timeline.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TimelineComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var TimelineComponent = /** @class */ (function () {
    function TimelineComponent() {
    }
    TimelineComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'timeline-cmp',
            template: __webpack_require__("./src/app/timeline/timeline.component.html")
        })
    ], TimelineComponent);
    return TimelineComponent;
}());



/***/ }),

/***/ "./src/app/timeline/timeline.module.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "TimelineModule", function() { return TimelineModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__("./node_modules/@angular/router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__("./node_modules/@angular/common/esm5/common.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_forms__ = __webpack_require__("./node_modules/@angular/forms/esm5/forms.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__timeline_component__ = __webpack_require__("./src/app/timeline/timeline.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__timeline_routing__ = __webpack_require__("./src/app/timeline/timeline.routing.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};






var TimelineModule = /** @class */ (function () {
    function TimelineModule() {
    }
    TimelineModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["L" /* NgModule */])({
            imports: [
                __WEBPACK_IMPORTED_MODULE_2__angular_common__["b" /* CommonModule */],
                __WEBPACK_IMPORTED_MODULE_1__angular_router__["d" /* RouterModule */].forChild(__WEBPACK_IMPORTED_MODULE_5__timeline_routing__["a" /* TimelineRoutes */]),
                __WEBPACK_IMPORTED_MODULE_3__angular_forms__["c" /* FormsModule */]
            ],
            declarations: [__WEBPACK_IMPORTED_MODULE_4__timeline_component__["a" /* TimelineComponent */]]
        })
    ], TimelineModule);
    return TimelineModule;
}());



/***/ }),

/***/ "./src/app/timeline/timeline.routing.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TimelineRoutes; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__timeline_component__ = __webpack_require__("./src/app/timeline/timeline.component.ts");

var TimelineRoutes = [{
        path: '',
        children: [{
                path: 'pages/timeline',
                component: __WEBPACK_IMPORTED_MODULE_0__timeline_component__["a" /* TimelineComponent */]
            }]
    }];


/***/ })

});
//# sourceMappingURL=timeline.module.chunk.js.map