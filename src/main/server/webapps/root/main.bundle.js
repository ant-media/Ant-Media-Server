webpackJsonp(["main"],{

/***/ "../../../../../src/$$_lazy_route_resource lazy recursive":
/***/ (function(module, exports, __webpack_require__) {

var map = {
	"./app.page/app.page.module": [
		"../../../../../src/app/app.page/app.page.module.ts",
		"app.page.module"
	],
	"./calendar/calendar.module": [
		"../../../../../src/app/calendar/calendar.module.ts",
		"calendar.module"
	],
	"./charts/charts.module": [
		"../../../../../src/app/charts/charts.module.ts",
		"charts.module"
	],
	"./dashboard/dashboard.module": [
		"../../../../../src/app/dashboard/dashboard.module.ts",
		"dashboard.module"
	],
	"./forms/forms.module": [
		"../../../../../src/app/forms/forms.module.ts",
		"forms.module"
	],
	"./maps/maps.module": [
		"../../../../../src/app/maps/maps.module.ts",
		"maps.module"
	],
	"./pages/pages.module": [
		"../../../../../src/app/pages/pages.module.ts",
		"pages.module"
	],
	"./tables/tables.module": [
		"../../../../../src/app/tables/tables.module.ts",
		"tables.module"
	],
	"./timeline/timeline.module": [
		"../../../../../src/app/timeline/timeline.module.ts",
		"timeline.module"
	],
	"./userpage/user.module": [
		"../../../../../src/app/userpage/user.module.ts",
		"user.module"
	]
};
function webpackAsyncContext(req) {
	var ids = map[req];
	if(!ids)
		return Promise.reject(new Error("Cannot find module '" + req + "'."));
	return __webpack_require__.e(ids[1]).then(function() {
		return __webpack_require__(ids[0]);
	});
};
webpackAsyncContext.keys = function webpackAsyncContextKeys() {
	return Object.keys(map);
};
webpackAsyncContext.id = "../../../../../src/$$_lazy_route_resource lazy recursive";
module.exports = webpackAsyncContext;

/***/ }),

/***/ "../../../../../src/app/app.component.css":
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__("../../../../css-loader/lib/css-base.js")(false);
// imports


// module
exports.push([module.i, "", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ "../../../../../src/app/app.component.html":
/***/ (function(module, exports) {

module.exports = "<router-outlet></router-outlet>\n"

/***/ }),

/***/ "../../../../../src/app/app.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AppComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__locale_locale__ = __webpack_require__("../../../../../src/app/locale/locale.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var AppComponent = /** @class */ (function () {
    function AppComponent(localeService) {
        this.localeService = localeService;
    }
    AppComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            selector: 'app-root',
            template: __webpack_require__("../../../../../src/app/app.component.html"),
            styles: [__webpack_require__("../../../../../src/app/app.component.css")]
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__locale_locale__["a" /* Locale */]])
    ], AppComponent);
    return AppComponent;
}());



/***/ }),

/***/ "../../../../../src/app/app.module.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AppModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__("../../../router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_http__ = __webpack_require__("../../../http/esm5/http.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_common__ = __webpack_require__("../../../common/esm5/common.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__angular_forms__ = __webpack_require__("../../../forms/esm5/forms.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__angular_platform_browser_animations__ = __webpack_require__("../../../platform-browser/esm5/animations.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__app_component__ = __webpack_require__("../../../../../src/app/app.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__sidebar_sidebar_module__ = __webpack_require__("../../../../../src/app/sidebar/sidebar.module.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__shared_footer_footer_module__ = __webpack_require__("../../../../../src/app/shared/footer/footer.module.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__shared_navbar_navbar_module__ = __webpack_require__("../../../../../src/app/shared/navbar/navbar.module.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10__layouts_admin_admin_layout_component__ = __webpack_require__("../../../../../src/app/layouts/admin/admin-layout.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_11__layouts_auth_auth_layout_component__ = __webpack_require__("../../../../../src/app/layouts/auth/auth-layout.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_12__app_routing__ = __webpack_require__("../../../../../src/app/app.routing.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_13__rest_auth_service__ = __webpack_require__("../../../../../src/app/rest/auth.service.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_14__rest_rest_service__ = __webpack_require__("../../../../../src/app/rest/rest.service.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_15__angular_common_http__ = __webpack_require__("../../../common/esm5/http.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_16_ngx_clipboard__ = __webpack_require__("../../../../ngx-clipboard/dist/ngx-clipboard.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_17__locale_locale__ = __webpack_require__("../../../../../src/app/locale/locale.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_18__app_page_app_page_component__ = __webpack_require__("../../../../../src/app/app.page/app.page.component.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
















//import { AuthInterceptor } from './rest/auth.interceptor';



var AppModule = /** @class */ (function () {
    function AppModule() {
    }
    AppModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["L" /* NgModule */])({
            imports: [
                __WEBPACK_IMPORTED_MODULE_5__angular_platform_browser_animations__["a" /* BrowserAnimationsModule */],
                __WEBPACK_IMPORTED_MODULE_4__angular_forms__["c" /* FormsModule */],
                __WEBPACK_IMPORTED_MODULE_1__angular_router__["d" /* RouterModule */].forRoot(__WEBPACK_IMPORTED_MODULE_12__app_routing__["a" /* AppRoutes */]),
                __WEBPACK_IMPORTED_MODULE_2__angular_http__["a" /* HttpModule */],
                __WEBPACK_IMPORTED_MODULE_7__sidebar_sidebar_module__["a" /* SidebarModule */],
                __WEBPACK_IMPORTED_MODULE_9__shared_navbar_navbar_module__["a" /* NavbarModule */],
                __WEBPACK_IMPORTED_MODULE_8__shared_footer_footer_module__["a" /* FooterModule */],
                __WEBPACK_IMPORTED_MODULE_16_ngx_clipboard__["a" /* ClipboardModule */]
            ],
            declarations: [
                __WEBPACK_IMPORTED_MODULE_6__app_component__["a" /* AppComponent */],
                __WEBPACK_IMPORTED_MODULE_10__layouts_admin_admin_layout_component__["a" /* AdminLayoutComponent */],
                __WEBPACK_IMPORTED_MODULE_11__layouts_auth_auth_layout_component__["a" /* AuthLayoutComponent */]
            ],
            providers: [
                __WEBPACK_IMPORTED_MODULE_18__app_page_app_page_component__["a" /* AppPageComponent */],
                __WEBPACK_IMPORTED_MODULE_13__rest_auth_service__["a" /* AuthService */],
                __WEBPACK_IMPORTED_MODULE_14__rest_rest_service__["d" /* RestService */],
                __WEBPACK_IMPORTED_MODULE_17__locale_locale__["a" /* Locale */],
                { provide: __WEBPACK_IMPORTED_MODULE_3__angular_common__["g" /* LocationStrategy */], useClass: __WEBPACK_IMPORTED_MODULE_3__angular_common__["d" /* HashLocationStrategy */] },
                //{provide: RequestOptions, useClass: CustomRequestOptions},
                {
                    provide: __WEBPACK_IMPORTED_MODULE_15__angular_common_http__["a" /* HTTP_INTERCEPTORS */],
                    useClass: __WEBPACK_IMPORTED_MODULE_14__rest_rest_service__["a" /* AuthInterceptor */],
                    multi: true
                }
            ],
            bootstrap: [__WEBPACK_IMPORTED_MODULE_6__app_component__["a" /* AppComponent */]]
        })
    ], AppModule);
    return AppModule;
}());



/***/ }),

/***/ "../../../../../src/app/app.page/app.page.component.css":
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__("../../../../css-loader/lib/css-base.js")(false);
// imports


// module
exports.push([module.i, "html,body{\n    height:100%;\n}", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ "../../../../../src/app/app.page/app.page.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"main-content\">\n    <div class=\"container-fluid\">\n        <div class=\"row\">\n            <div class=\"col-lg-12 col-sm-12\">\n                <div class=\"card\">\n                    <div class=\"card-content\">\n                        <div class=\"nav-tabs-navigation\">\n                            <div class=\"nav-tabs-wrapper\">\n                                <ul id=\"tabs\" class=\"nav nav-tabs\" data-tabs=\"tabs\">\n                                    <li class=\"active\">\n                                        <a href=\"#livestreams\" data-toggle=\"tab\" i18n=\"live stream|Live Streams tab header that lists live stream@@liveStreamTabHeader\">Live Streams</a>\n                                    </li>\n                                    <li>\n                                        <a href=\"#vod\" data-toggle=\"tab\" i18n=\"VoD|Vod tab header that lists vod@@vodTabHeader\">VoD</a>\n                                    </li>\n                                    <!--\n                                    <li>\n                                        <a href=\"#logs\" data-toggle=\"tab\" i18n=\"@@logTabHeader\">Log</a>\n                                    </li>\n                                -->\n                                    <li>\n                                        <a href=\"#settings\" data-toggle=\"tab\" i18n=\"@@settingsTabHeader\">Settings</a>\n                                    </li>\n                                </ul>\n                            </div>\n                        </div>\n                        <div id=\"my-tab-content\" class=\"tab-content text-center\">\n\n                            <div class=\"tab-pane active\" id=\"livestreams\">\n                                <!--\n                                <div class=\"text-right\">\n                                    <button class=\"btn btn-fill btn-success\" i18n=\"@@newLiveStreamButton\" (click)=\"newLiveStream()\">New Live Stream</button>\n                                </div>\n                                -->\n\n                                <div>\n\n                                    <div class=\"dropdown text-right\" style=\"float: right\">\n                                        <button class=\"btn btn-fill btn-success\" i18n=\"@@newLiveStreamButton\" data-toggle=\"dropdown\" role=\"button\">New Live Stream\n                                            <span class=\"caret\"></span>\n                                        </button>\n\n                                        <ul class=\"dropdown-menu dropdown-menu-right\">\n\n                                            <li>\n                                                <a (click)=\"newLiveStream()\" role=\"button\">Live Stream</a>\n                                            </li>\n                                            <li>\n                                                <a (click)=\"newIPCamera()\" role=\"button\">IP Camera</a>\n                                            </li>\n\n\n                                                        <li>\n                                                                <a (click)=\"newStreamSource()\" role=\"button\">Stream Source</a>\n                                                         </li>\n\n\n                                        </ul>\n\n                                    </div>\n\n                                    <br>\n                                    <br>\n\n                                    <!--\n                                    <div class=\"col-sm-6 col-lg-2 \" style=\"float: right\">\n                                        <select id=\"selectBox\"  class=\"selectpicker  \" data-style=\"btn btn-danger btn-block\"  title=\"Filter\" data-size=\"7\">\n                                            <option  value=\"liveStream\">Live Stream</option>\n                                            <option  value=\"ipCamera\">IP Camera</option>\n\n                                            <option  value=\"displayAll\">Display All</option>\n\n                                        </select>\n                                    </div>\n                                    -->\n                                </div>\n\n                                <div class=\"card\" *ngIf=\"newLiveStreamActive\">\n\n                                    <form method=\"post\" #f=\"ngForm\" validate (ngSubmit)=\"createLiveStream(f.valid)\">\n                                        <div class=\"card-header\">\n                                            <h4 class=\"card-title text-left\" i18n=\"@@newLiveStreamCardTitle\">\n                                                New Live Stream\n                                            </h4>\n                                        </div>\n                                        <div class=\"card-content\">\n                                            <div class=\"form-group text-left\">\n                                                <label i18n=\"@@newLiveStreamName\">Name</label>\n                                                <input type=\"text\" required minlength=\"4\" name=\"broadcastName\" i18n-placeholder=\"@@stream_name_place_holder\" placeholder=\"Stream name\"\n                                                       class=\"form-control\" [(ngModel)]=\"liveBroadcast.name\"\n                                                       #broadcastName=\"ngModel\">\n                                                <small [hidden]=\"broadcastName.valid || (!f.submitted)\" class=\"text-danger\" i18n=\"@@broadcastNameIsRequired\">\n                                                    Stream name should be at least 4 characters.\n                                                </small>\n                                            </div>\n\n                                            <div class=\"form-group text-left\" *ngIf=\"videoServiceEndpoints.length>0\">\n                                                <label  class=\"col-sm-12\" style=\"padding-left:0px\" i18n=\"@@newLiveStreamSocialShare\">Share</label>\n                                                <ng-container *ngFor=\"let endpoint of videoServiceEndpoints; let i = index\">\n\n                                                    <div class=\"col-sm-4 text-left checkbox vcenter\"style=\"margin-top:5px\">\n\n                                                        <input [id]=\"endpoint.id\" [name]=\"endpoint.id\" type=\"checkbox\" [(ngModel)]=\"shareEndpoint[i]\">\n                                                        <label [for]=\"endpoint.id\">\n                                                            <ng-container [ngSwitch]=\"endpoint.serviceName\">\n                                                                <ng-container *ngSwitchCase=\"'facebook'\">\n                                                                    <i class=\"ti-facebook\" style=\"color:#3b5998\">&nbsp;</i>\n                                                                </ng-container>\n                                                                <ng-container *ngSwitchCase=\"'youtube'\">\n                                                                    <i class=\"ti-youtube\" style=\"color:#e52d27\">&nbsp;</i>\n                                                                </ng-container>\n                                                                <ng-container *ngSwitchCase=\"'periscope'\">\n                                                                    <i class=\"ti-twitter-alt\" style=\"color:#55acee\">&nbsp;</i>\n                                                                </ng-container>\n                                                            </ng-container>\n                                                            {{endpoint.accountName}}\n                                                        </label>\n\n                                                    </div>\n                                                </ng-container>\n                                            </div>\n\n                                            <div class=\"form-group text-center\">\n\n                                                <button type=\"submit\" [disabled]='newLiveStreamCreating' class=\"btn btn-fill btn-success\" i18n=\"@@newLiveStreamCreateButton\">\n                                                    <i class=\"fa fa-spinner fa-pulse fa-1x fa-fw\" *ngIf=\"newLiveStreamCreating\" aria-hidden=\"true\"></i>Create</button>\n\n                                                <button type=\"button\" class=\"btn btn-simple\" i18n=\"@@newLiveStreamCancelButton\" (click)=\"cancelNewLiveStream()\">Cancel</button>\n                                            </div>\n\n                                        </div>\n\n                                    </form>\n                                </div>\n\n                                <div class=\"card\" *ngIf=\"newIPCameraActive\">\n\n                                    <form method=\"post\" #f=\"ngForm\" validate (ngSubmit)=\"addIPCamera(f.valid)\">\n                                        <div class=\"card-header\">\n                                            <h4 class=\"card-title text-left\" i18n=\"@@newIPCameraTitle\">\n                                                New IP Camera\n                                            </h4>\n                                        </div>\n                                        <div class=\"card-content\">\n                                            <div class=\"form-group text-left\">\n                                                <label i18n=\"@@newLiveStreamName\">Name</label>\n                                                <input type=\"text\" required minlength=\"4\" name=\"broadcastName\"\n                                                       placeholder=\"Camera Name\" class=\"form-control\"\n                                                       [(ngModel)]=\"liveBroadcast.name\"\n                                                       #broadcastName=\"ngModel\">\n                                                <small [hidden]=\"broadcastName.valid || (!f.submitted)\" class=\"text-danger\" i18n=\"@@broadcastNameIsRequired\">\n                                                    Camera name should be at least 4 characters.\n                                                </small>\n                                            </div>\n\n                                            <div class=\"form-group text-left\">\n                                                <label i18n=\"@@newOnvifIPUrl\">Onvif IP URL</label>\n                                                <button (click)=\"startDiscover()\"\n                                                        class=\"btn btn-success btn-simple btn-magnify btn-xs \"\n                                                        type=\"button\"> discover\n                                                </button>\n                                                <i class=\"fa fa-spinner fa-pulse fa-1x fa-fw\" *ngIf=\"discoveryStarted\"\n                                                   aria-hidden=\"true\"></i>\n                                                <small class=\"text-danger\" *ngIf=\"noCamWarning\">\n                                                    No unregistered camera found, write camera URL manually!\n                                                </small>\n                                                <input type=\"text\" name=\"onvifURL\"\n                                                       i18n-placeholder=\"@@onvifUrl_place_holder\"\n                                                       placeholder=\"Onvif Url\" class=\"form-control\"\n                                                       [(ngModel)]=\"liveBroadcast.ipAddr\" #onvifURL=\"ngModel\">\n\n                                            </div>\n\n                                            <div class=\"form-group text-left\">\n                                                <label i18n=\"@@newLiveStreamName\">User Name</label>\n                                                <input type=\"text\" name=\"username\"\n                                                       i18n-placeholder=\"@@username_place_holder\"\n                                                       placeholder=\"User Name\" class=\"form-control\"\n                                                       [(ngModel)]=\"liveBroadcast.username\" #username=\"ngModel\">\n\n                                            </div>\n\n                                            <div class=\"form-group text-left\">\n                                                <label i18n=\"@@newLiveStreamName\">Password</label>\n                                                <input type=\"text\" name=\"password\"\n                                                       i18n-placeholder=\"@@password_place_holder\" placeholder=\"Password\"\n                                                       class=\"form-control\"\n                                                       [(ngModel)]=\"liveBroadcast.password\" #password=\"ngModel\">\n\n                                            </div>\n\n\n                                            <div class=\"form-group text-center\">\n\n                                                <button type=\"submit\" [disabled]='newIPCameraAdding' class=\"btn btn-fill btn-success\" i18n=\"@@newLiveStreamCreateButton\">\n                                                    <i class=\"fa fa-spinner fa-pulse fa-1x fa-fw\" *ngIf=\"newIPCameraAdding\" aria-hidden=\"true\"></i>Add</button>\n\n                                                <button type=\"button\" class=\"btn btn-simple\" i18n=\"@@newLiveStreamCancelButton\" (click)=\"cancelNewIPCamera()\">Cancel</button>\n                                            </div>\n\n                                        </div>\n\n                                    </form>\n                                </div>\n\n\n\n\n                                <div class=\"card\" *ngIf=\"newStreamSourceActive\">\n\n                                    <form method=\"post\" #f=\"ngForm\" validate (ngSubmit)=\"addStreamSource(f.valid)\">\n                                        <div class=\"card-header\">\n                                            <h4 class=\"card-title text-left\" i18n=\"@@newLiveStreamCardTitle\">\n                                                New Stream Source\n                                            </h4>\n                                        </div>\n                                        <div class=\"card-content\">\n                                            <div class=\"form-group text-left\">\n                                                <label i18n=\"@@newLiveStreamName\">Name</label>\n                                                <input type=\"text\" required minlength=\"4\" name=\"broadcastName\"\n                                                       i18n-placeholder=\"@@stream_name_place_holder\"\n                                                       placeholder=\"Stream name\"\n                                                       class=\"form-control\" [(ngModel)]=\"liveBroadcast.name\"\n                                                       #broadcastName=\"ngModel\">\n                                                <small [hidden]=\"broadcastName.valid || (!f.submitted)\" class=\"text-danger\" i18n=\"@@broadcastNameIsRequired\">\n                                                    Stream name should be at least 4 characters.\n                                                </small>\n                                            </div>\n\n\n                                            <div class=\"form-group text-left\">\n                                                <label i18n=\"@@newLiveStreamUrl\">Stream Url</label>\n                                                <input type=\"text\" required minlength=\"4\" name=\"broadcasturl\"\n                                                       i18n-placeholder=\"@@stream_name_place_holder\"\n                                                       placeholder=\"Stream Url\"\n                                                       class=\"form-control\" [(ngModel)]=\"liveBroadcast.streamUrl\"\n                                                       #broadcastName=\"ngModel\">\n\n                                            </div>\n\n\n                                            <div class=\"form-group text-center\">\n\n                                                <button type=\"submit\" [disabled]='newStreamSourceWarn' class=\"btn btn-fill btn-success\" i18n=\"@@newLiveStreamCreateButton\">\n                                                    <i class=\"fa fa-spinner fa-pulse fa-1x fa-fw\" *ngIf=\"newStreamSourceAdding\" aria-hidden=\"true\"></i>Create</button>\n\n                                                <button type=\"button\" class=\"btn btn-simple\" i18n=\"@@newLiveStreamCancelButton\" (click)=\"cancelStreamSource()\">Cancel</button>\n                                            </div>\n\n                                        </div>\n\n                                    </form>\n                                </div>\n\n                                <br>\n\n                                <div style=\"display: flex; justify-content: flex-end\"\n                                     *ngIf=\"broadcastTableData.dataRows.length>0\">\n\n                                    <div class=\"mat-header\">\n\n                                        <input (keyup)=\"applyFilter($event.target.value)\" placeholder=\"Search\">\n\n                                    </div>\n                                    <div>\n                                        <button class=\"btn\" *ngIf=\"isGridView\" (click)=\"switchToListView()\">\n                                            <i class=\"fa fa-bars\"></i> List\n                                        </button>\n                                        <button class=\"btn\" *ngIf=\"!isGridView\" (click)=\"switchToGridView()\">\n                                            <i class=\"fa fa-th-large\"></i> Grid\n                                        </button>\n                                    </div>\n\n                                </div>\n\n\n                                <div id=\"broadcastTable\" style=\"text-align: left;\"\n                                     *ngIf=\"!isGridView && broadcastTableData.dataRows.length>0\">\n\n\n                                    <div class=\"mat-container mat-elevation-z0\">\n\n                                        <mat-table [dataSource]=\"dataSource\" matSort>\n\n\n                                            <ng-container matColumnDef=\"name\">\n                                                <mat-header-cell *matHeaderCellDef mat-sort-header> Stream Name </mat-header-cell>\n\n                                                <mat-cell *matCellDef=\"let row\">\n\n                                                    <ng-container [ngSwitch]=\"row.type\">\n                                                        <ng-container *ngSwitchCase=\"'liveStream'\">\n                                                            <img style=\"width: 10%\"\n                                                             src=\"/assets/img/icons/video-camera.svg\">\n                                                        </ng-container>\n                                                        <ng-container *ngSwitchCase=\"'ipCamera'\">\n                                                            <img style=\"width: 10%\"\n                                                             src=\"/assets/img/icons/cctv.svg\"> \n                                                        </ng-container>\n                                                        <ng-container *ngSwitchCase=\"'streamSource'\">\n                                                            <img style=\"width: 10%\"\n                                                             src=\"/assets/img/icons/video-camera.svg\"> \n                                                        </ng-container>\n                                                    </ng-container>    \n\n                                                    {{row.name}}\n                                                    <ng-container *ngIf=\"!row.name || row.name.length == 0\">\n                                                        {{row.streamId}}\n                                                    </ng-container>\n                                                   \n                                                    \n\n                                                </mat-cell>\n                                            </ng-container>\n\n\n                                            <ng-container matColumnDef=\"status\">\n                                                <mat-header-cell *matHeaderCellDef mat-sort-header>Status </mat-header-cell>\n                                                <mat-cell *matCellDef=\"let row\">\n\n\n                                                    <ng-container [ngSwitch]=\"row.status\">\n                                                        <ng-container *ngSwitchCase=\"'created'\">\n                                                            <i class=\"fa fa-circle text-muted\">&nbsp;</i>Offline\n                                                    \n                                                        </ng-container>\n                                                        <ng-container *ngSwitchCase=\"'broadcasting'\">\n                                                            <ng-container [ngSwitch]=\"row.quality\">\n\n                                                                <ng-container *ngSwitchCase=\"'poor'\">\n                                                                    <i class=\"ti-pulse\"\n                                                                       style=\"color:#98000b;font-size: 1.1em\"\n                                                                       data-toggle=\"tooltip\"\n                                                                       i18n-title\n                                                                       title=\"Quality: Poor\"> </i>\n                                                                </ng-container>\n                                                                <ng-container *ngSwitchCase=\"'average'\">\n                                                                    <i class=\"ti-pulse\"\n                                                                       style=\"color:#190e98;font-size: 1.1em\"\n                                                                       data-toggle=\"tooltip\" i18n-title\n                                                                       title=\"Quality: Average\"> </i></ng-container>\n                                                                <ng-container *ngSwitchCase=\"'good'\">\n                                                                    <i class=\"ti-pulse\"\n                                                                       style=\"color:#199826;font-size: 1.1em\"\n                                                                       data-toggle=\"tooltip\"\n                                                                       i18n-title\n                                                                       title=\"Quality: Good\"> </i>\n                                                                </ng-container>\n                                                                Broadcasting\n                                                            </ng-container>\n\n                                                            <ng-container *ngIf=\"row.speed > 0\">\n                                                                <i data-toggle=\"tooltip\"\n                                                                   i18n-title\n                                                                   title=\"Speed\">\n                                                                    {{row.speed| number : '1.2-2'}}x</i>\n                                                            </ng-container>\n\n\n                                                        </ng-container>\n                                                        <ng-container *ngSwitchCase=\"'finished'\">\n                                                            <i class=\"fa fa-circle text-muted\"\n                                                               *ngSwitchCase=\"'finished'\">&nbsp;</i>Offline\n                                                        </ng-container>\n                                                    </ng-container>\n\n                                                </mat-cell>\n\n\n\n                                            </ng-container>\n\n\n                                            <ng-container matColumnDef=\"social_media\">\n                                                <mat-header-cell *matHeaderCellDef> Social Share</mat-header-cell>\n                                                <mat-cell *matCellDef=\"let row\">\n\n                                                    <div *ngIf=\"!row.endPointList\">\n                                                        <i class=\"ti-facebook \" style=\"color:#dad9d2;\"> </i>\n                                                        <i class=\"ti-youtube \" style=\"color:#dad9d2;\"> </i>\n                                                        <i class=\"ti-twitter-alt \" style=\"color:#dad9d2;\"></i>\n\n                                                    </div>\n                                                    <ng-container *ngFor=\"let endpoint of row.endPointList\" class=\"text-center\">\n                                                        <ng-container [ngSwitch]=\"endpoint.type\">\n                                                            <i class=\"ti-facebook \" style=\"color:#3b5998;\" *ngSwitchCase=\"'facebook'\"> </i>\n                                                            <i class=\"ti-youtube \" style=\"color:#e52d27;\" *ngSwitchCase=\"'youtube'\"> </i>\n                                                            <i class=\"ti-twitter-alt \" style=\"color:#55acee;\" *ngSwitchCase=\"'periscope'\"></i>\n                                                        </ng-container>\n\n                                                    </ng-container>\n                                                </mat-cell>\n                                            </ng-container>\n\n\n                                            <ng-container matColumnDef=\"actions\">\n                                                <mat-header-cell *matHeaderCellDef class=\"text-right\"> Actions</mat-header-cell>\n                                                <mat-cell *matCellDef=\"let row\" class=\"text-right\">\n\n                                                    <div style=\"font-size: 1.1em\">\n                                                        <button *ngIf=\"row.status=='broadcasting' \"\n                                                                data-toggle=\"tooltip\" i18n-title title=\"Play Stream\"\n                                                                (click)=\"playLive(row.streamId)\"\n                                                                class=\"btn btn-success btn-simple btn-magnify btn-xs \"\n                                                                type=\"button\"\n                                                                style=\"font-size: 1.1em\">\n                                                            <span class=\"btn-label\">\n                                                                <i class=\"ti-control-play\"></i>\n                                                            </span>\n                                                            <ng-container i18n=\"@@tablePlayButton\"></ng-container>\n\n                                                        </button>\n\n                                                        <button *ngIf=\"row.status!='broadcasting'&& row.type=='liveStream'\"\n                                                                (click)=\"openBroadcastEditDialog(row)\" title=\"Edit\"\n                                                                data-toggle=\"tooltip\"\n                                                                class=\"btn btn-simple btn-magnify btn-info btn-xs \"\n                                                                type=\"button\"\n                                                                style=\"font-size: 1.1em\">\n                                                            <span class=\"btn-label\">\n                                                                <i class=\"ti-pencil\"></i>\n                                                            </span>\n                                                            <ng-container i18n=\"@@tableEditButton\"></ng-container>\n\n                                                        </button>\n\n\n                                                        <button *ngIf=\"row.type=='ipCamera'\"\n                                                                (click)=\"openSettingsDialog(row)\"\n                                                                class=\"btn btn-simple btn-magnify btn-info btn-xs \"\n                                                                type=\"button\" data-toggle=\"tooltip\" i18n-title\n                                                                title=\"Settings\" style=\"font-size: 1.1em\">\n                                                            <span class=\"btn-label\" style=\"float: right;\">\n                                                                <i class=\"ti-settings\"></i>\n                                                            </span>\n\n                                                            <ng-container i18n=\"@@tableSettings\"></ng-container>\n                                                        </button>\n\n\n                                                        <button (click)=\"copyLiveEmbedCode(row.streamId)\"\n                                                                data-toggle=\"tooltip\" i18n-title\n                                                                title=\"Copy Live Embed Code to Clipboard\"\n                                                                class=\"btn btn-simple btn-magnify btn-info btn-xs \"\n                                                                type=\"button\"\n                                                                style=\"font-size: 1.1em\">\n                                                            <span class=\"btn-label\">\n                                                                <i class=\"ti-share\"></i>\n                                                            </span>\n                                                            <ng-container i18n=\"@@tableEmbedCode\"></ng-container>\n                                                        </button>\n\n                                                        <button *ngIf=\"row.status!='broadcasting'\"\n                                                                id=\"copyPublishUrlButton\"\n                                                                (click)=\"copyPublishUrl(row.streamId)\"\n                                                                data-toggle=\"tooltip\"\n                                                                [title]=\"'Copy Publish URL to Clipboard : ' + getRtmpUrl(row.streamId)\"\n                                                                class=\"btn btn-simple btn-magnify btn-info btn-xs \"\n                                                                type=\"button\"\n                                                                style=\"font-size: 1.1em\">\n                                                            <span class=\"btn-label\">\n                                                                <i class=\"ti-clipboard\"></i>\n                                                            </span>\n                                                            <ng-container i18n=\"@@copyPublishUrl\"></ng-container>\n                                                        </button>\n                                                        <button (click)=\"deleteLiveBroadcast(row.streamId)\"\n                                                                class=\"btn btn-simple btn-magnify btn-danger btn-xs \"\n                                                                type=\"button\" data-toggle=\"tooltip\"\n                                                                i18n-title title=\"Delete\" style=\"font-size: 1.1em\">\n                                                            <span class=\"btn-label\">\n                                                                <i class=\"ti-close\"></i>\n                                                            </span>\n                                                            <ng-container i18n=\"@@tableDeleteButton\"></ng-container>\n                                                        </button>\n\n                                                    </div>\n\n\n                                                </mat-cell>\n\n                                            </ng-container>\n\n                                            <mat-header-row *matHeaderRowDef=\"displayedColumnsStreams\"></mat-header-row>\n                                            <mat-row *matRowDef=\"let row; columns: displayedColumnsStreams;\">\n                                            </mat-row>\n                                        </mat-table>\n\n                                        <mat-paginator\n                                                [pageSizeOptions]=\"pageSizeOptions\"\n                                                (page)=\"onListPaginateChange($event)\"\n                                                [length]=\"listLength\"\n                                                [pageSize]=\"pageSize\"\n                                        >\n                                        </mat-paginator>\n                                    </div>\n\n                                </div>\n\n\n                                <div class=\"main\">\n\n\n                                    <div id=\"cbp-vm\" class=\"cbp-vm-switcher cbp-vm-view-list\"\n                                         *ngIf=\"broadcastTableData.dataRows.length>0 && isGridView\">\n\n                                        <ul>\n                                            <div>\n\n                                                <div class=\"card col-md-6 col-lg-6 col-sm-12 col-xs-12\"\n                                                     *ngFor=\"let row of broadcastGridTableData.dataRows\">\n\n\n                                                    <div class=\"card-content\">\n\n                                                        <!--   <div id=\"{{row.streamId}}\"></div>\n                                                        src=\"http://localhost:5080/LiveApp/play.html?name=711790025373335366896087\"-->\n\n\n                                                        <div class=\"embed-container\">\n\n\n                                                            <iframe id=\"{{row.streamId}}\" allowfullscreen=\"true\" ></iframe>\n\n\n                                                        </div>\n                                                        <div *ngIf=\"row.type=='ipCamera' && row.status=='broadcasting'&&isMobileMenu()\" class=\"ptz-buttons\">\n\n\n                                                            <span (click)=\"moveUp(row)\" class=\"ti-arrow-circle-up\"\n                                                                  style=\"top: 8px; left: 40px; position: absolute\"></span>\n\n                                                            <span (click)=\"moveRight(row)\" class=\"ti-arrow-circle-right\"\n                                                                  style=\"top: 40px; left: 70px; position: absolute\"></span>\n\n                                                            <span (click)=\"moveLeft(row)\" class=\"ti-arrow-circle-left\"\n                                                                  style=\"top: 40px; left: 10px; position: absolute\"></span>\n\n                                                            <span (click)=\"moveDown(row)\" class=\"ti-arrow-circle-down\"\n                                                                  style=\"top: 70px; left: 40px; position: absolute\"></span>\n\n                                                        </div>\n                                                        <div class=\"conf-buttons\">\n\n\n                                                            <span style=\"float: left;\"> {{row.name}}</span>\n\n                                                            <button (click)=\"deleteLiveBroadcast(row.streamId)\"\n                                                                    class=\"btn btn-simple btn-magnify btn-danger btn-xs \"\n                                                                    type=\"button\" data-toggle=\"tooltip\"\n                                                                    i18n-title title=\"Delete\"\n                                                                    style=\"float: right;font-size: 1.2em;\">\n                                                                <span class=\"btn-label\">\n                                                                    <i class=\"ti-close\"></i>\n                                                                </span>\n                                                                <ng-container i18n=\"@@tableDeleteButton\"></ng-container>\n                                                            </button>\n\n\n                                                            <button (click)=\"copyLiveEmbedCode(row.streamId)\"\n                                                                    data-toggle=\"tooltip\" i18n-title\n                                                                    title=\"Copy Live Embed Code to Clipboard\"\n                                                                    class=\"btn btn-simple btn-magnify btn-info btn-xs \"\n                                                                    type=\"button\"\n                                                                    style=\"float: right;font-size: 1.2em;\">\n                                                                <span class=\"btn-label\">\n                                                                    <i class=\"ti-share\"></i>\n                                                                </span>\n                                                                <ng-container i18n=\"@@tableEmbedCode\"></ng-container>\n                                                            </button>\n                                                            <button *ngIf=\"row.status!='broadcasting'\" id=\"copyPublishUrlButton\" (click)=\"copyPublishUrl(row.streamId)\" data-toggle=\"tooltip\"\n                                                                    [title]=\"'Copy Publish URL to Clipboard : ' + getRtmpUrl(row.streamId)\"\n                                                                    class=\"btn btn-simple btn-magnify btn-info btn-xs \"\n                                                                    type=\"button\"\n                                                                    style=\"float: right;font-size: 1.2em;\">\n                                                                <span class=\"btn-label\">\n                                                                    <i class=\"ti-clipboard\"></i>\n                                                                </span>\n                                                                <ng-container i18n=\"@@copyPublishUrl\"></ng-container>\n                                                            </button>\n\n                                                            <button *ngIf=\"row.status!='broadcasting'&& row.type=='liveStream'\"\n                                                                    (click)=\"openBroadcastEditDialog(row)\" title=\"Edit\"\n                                                                    data-toggle=\"tooltip\"\n                                                                    class=\"btn btn-simple btn-magnify btn-info btn-xs \"\n                                                                    type=\"button\"\n                                                                    style=\"float: right;font-size: 1.2em;\">\n                                                                <span class=\"btn-label\">\n                                                                    <i class=\"ti-pencil\"></i>\n                                                                </span>\n                                                                <ng-container i18n=\"@@tableEditButton\"></ng-container>\n\n                                                            </button>\n\n                                                            <button *ngIf=\"row.type=='ipCamera'\"\n                                                                    (click)=\"openSettingsDialog(row)\"\n                                                                    class=\"btn btn-simple btn-magnify btn-info btn-xs \"\n                                                                    type=\"button\" data-toggle=\"tooltip\" i18n-title\n                                                                    title=\"Settings\"\n                                                                    style=\"float: right;font-size: 1.2em;\">\n                                                                <span class=\"btn-label\" style=\"float: right;\">\n                                                                    <i class=\"ti-settings\"></i>\n                                                                </span>\n\n                                                                <ng-container></ng-container>\n                                                            </button>\n\n                                                        </div>\n                                                    </div>\n\n                                                </div>\n                                            </div>\n                                        </ul>\n                                        <div>\n                                            <mat-paginator [pageSizeOptions]=\"[4, 10, 25]\"\n                                                           [length]=\"listLength\"\n                                                           (page)=\"onGridPaginateChange($event)\">\n\n\n                                            </mat-paginator>\n                                        </div>\n                                    </div>\n\n                                    <p *ngIf=\"broadcastTableData.dataRows.length == 0\" i18n=\"no live stream info | text messages appears when no live streams@@noLiveStreamExistsMessage\">\n                                        There is no stream at this time.\n                                    </p>\n\n\n                                </div>\n                                <!-- /main -->\n\n\n                            </div>\n\n                            <div class=\"tab-pane container-fluid\" id=\"vod\">\n\n                                <div class=\" text-right\">\n\n                                    <div>\n                                        <button class=\"btn btn-fill btn-success\" (click)=\"openVodUploadDialog()\"\n                                                data-toggle=\"dropdown\" role=\"button\">Upload VoD\n                                        </button>\n\n                                    </div>\n\n                                </div>\n\n                                <div id=\"vodTable\" style=\"text-align: left\"\n                                     *ngIf=\" vodTableData.dataRows.length>0\">\n\n                                    <br>\n                                    <div class=\"mat-container mat-elevation-z0\">\n\n                                        <mat-table [dataSource]=\"dataSourceVod\">\n\n\n                                            <ng-container matColumnDef=\"name\">\n                                                <mat-header-cell *matHeaderCellDef>Name</mat-header-cell>\n\n                                                <mat-cell *matCellDef=\"let row\">\n\n                                                    <ng-container [ngSwitch]=\"row.type\">\n                                                        <i *ngSwitchCase=\"'streamVod'\"> {{row.streamName}}</i>\n                                                        <i *ngSwitchCase=\"'uploadedVod'\">{{row.vodName}} </i>\n                                                        <i *ngSwitchCase=\"'userVod'\">{{row.vodName}}</i>\n                                                    </ng-container>\n\n                                                </mat-cell>\n\n\n                                            </ng-container>\n\n                                            <ng-container matColumnDef=\"type\">\n                                                <mat-header-cell *matHeaderCellDef> Type</mat-header-cell>\n\n                                                <mat-cell *matCellDef=\"let row\">\n                                                    {{row.type}}\n\n                                                </mat-cell>\n                                            </ng-container>\n\n                                            <ng-container matColumnDef=\"date\">\n                                                <mat-header-cell *matHeaderCellDef> Date </mat-header-cell>\n\n                                                <mat-cell *matCellDef=\"let row\">\n                                                    {{convertJavaTime(row.creationDate)}}\n\n                                                </mat-cell>\n                                            </ng-container>\n\n                                            <ng-container matColumnDef=\"actions\">\n                                                <mat-header-cell *matHeaderCellDef> Actions</mat-header-cell>\n\n                                                <mat-cell *matCellDef=\"let row\">\n                                                    <button (click)=\"playVoD(row.vodName,row.type)\"\n                                                            class=\"btn btn-success btn-simple btn-magnify\"\n                                                            type=\"button\">\n                                                        <span class=\"btn-label\">\n                                                            <i class=\"ti-control-play\"></i>\n                                                        </span>\n                                                        <ng-container i18n=\"@@tablePlayButton\">Play</ng-container>\n                                                    </button>\n                                                    <button (click)=\"deleteVoD(row.vodName,row.vodId)\" class=\"btn btn-simple btn-magnify btn-danger\" type=\"button\">\n                                                        <span class=\"btn-label\">\n                                                            <i class=\"ti-close\"></i>\n                                                        </span>\n                                                        <ng-container i18n=\"@@tableDeleteButton\">Delete</ng-container>\n                                                    </button>\n\n                                                </mat-cell>\n                                            </ng-container>\n\n                                            <mat-header-row *matHeaderRowDef=\"displayedColumnsVod\"></mat-header-row>\n                                            <mat-row *matRowDef=\"let row; columns: displayedColumnsVod;\">\n                                            </mat-row>\n                                        </mat-table>\n\n                                        <mat-paginator [length]=\"vodLength\"\n                                                       [pageSize]=\"pageSize\"\n                                                       [pageSizeOptions]=\"pageSizeOptions\"\n                                                       (page)=\"onPaginateChange($event)\"\n                                        >\n                                        </mat-paginator>\n                                    </div>\n\n                                </div>\n                                <p *ngIf=\"vodTableData.dataRows.length == 0\" i18n=\"no vod stream info | text messages appears when no vod streams@@noVoDStreamExistsMessage\">\n                                    There is no VoD stream in this app.\n                                </p>\n                            </div>\n                            <div class=\"tab-pane\" id=\"logs\">\n                                <p>Here are your messages.</p>\n                            </div>\n\n                            <div class=\"tab-pane\" id=\"settings\" *ngIf=\"appSettings\">\n                                <form method=\"post\" #f=\"ngForm\" (ngSubmit)=\"changeSettings(f.valid)\" class=\"form-horizontal\">\n\n                                    <fieldset>\n                                        <legend class=\"text-left\">Muxing</legend>\n                                        <div class=\"form-group\">\n                                            <label class=\"col-sm-2 control-label\">MP4</label>\n                                            <div class=\"col-sm-4 text-left\">\n                                                <div class=\"checkbox\">\n                                                    <input id=\"mp4MuxingEnabled\" name=\"mp4MuxingEnabled\" type=\"checkbox\" [(ngModel)]=\"appSettings.mp4MuxingEnabled\">\n                                                    <label for=\"mp4MuxingEnabled\" i18n=\"@@enableMP4Recording\">\n                                                        Enable MP4 Recording\n                                                    </label>\n                                                </div>\n                                            </div>\n                                            <div class=\"col-sm-4 text-left\">\n                                                <div class=\"checkbox\">\n                                                    <input id=\"addDateTimeToMp4FileName\" name=\"addDateTimeToMp4FileName\" type=\"checkbox\" [(ngModel)]=\"appSettings.addDateTimeToMp4FileName\">\n                                                    <label for=\"addDateTimeToMp4FileName\" i18n=\"@@addDateTimeToMP4Recordings\">\n                                                        Add Datetime to MP4 Filenames\n                                                    </label>\n                                                </div>\n                                            </div>\n                                        </div>\n\n\n                                        <div class=\"form-group\">\n                                            <label class=\"col-sm-2 control-label\">HLS</label>\n                                            <div class=\"col-sm-10 text-left\">\n                                                <div class=\"checkbox\">\n                                                    <input id=\"hlsMuxingEnabled\" name=\"hlsMuxingEnabled\" type=\"checkbox\" [(ngModel)]=\"appSettings.hlsMuxingEnabled\">\n                                                    <label for=\"hlsMuxingEnabled\" i18n=\"@@enableHLSLiveStreaming\">\n                                                        Enable HLS Live Streaming\n                                                    </label>\n                                                </div>\n                                                <div>\n                                                    <small [hidden]=\"hlsListSize.valid\" class=\"text-danger\">\n                                                        Segment list size is required\n                                                    </small>\n                                                    <input type=\"number\" name=\"hlsListSize\" class=\"form-control\" required #hlsListSize=\"ngModel\" [(ngModel)]=\"appSettings.hlsListSize\">\n                                                    <span class=\"help-block\" i18n=\"@@HLSSegmentListSize\">Segment List Size</span>\n\n                                                </div>\n                                                <div>\n                                                    <small [hidden]=\"hlsTime.valid\" class=\"text-danger\">\n                                                        Segment duration is required\n                                                    </small>\n                                                    <input type=\"number\" name=\"hlsTime\" class=\"form-control\" #hlsTime=\"ngModel\" required [(ngModel)]=\"appSettings.hlsTime\">\n                                                    <span class=\"help-block\" i18n=\"@@HLSSegmentDuration\">Segment Duration</span>\n\n                                                </div>\n\n                                            </div>\n                                        </div>\n                                    </fieldset>\n\n\n                                    <fieldset>\n                                        <legend class=\"text-left\" i18n=\"@@Security\">Security</legend>\n                                        <div class=\"form-group\">\n                                            <label class=\"col-sm-2 control-label\">Stream Publishing</label>\n                                            <div class=\"col-sm-10 text-left\">\n                                                <div class=\"radio col-sm-3\">\n                                                    <input type=\"radio\" name=\"acceptOnlyStreamsInDataStore\" id=\"acceptOnlyStreamsInDataStore\" [value]=\"false\" [(ngModel)]=\"appSettings.acceptOnlyStreamsInDataStore\">\n                                                    <label for=\"acceptOnlyStreamsInDataStore\">\n                                                        Allow All\n                                                    </label>\n                                                </div>\n\n                                                <div class=\"radio col-sm-3\" style=\"margin-top:13px\">\n                                                    <input type=\"radio\" name=\"acceptOnlyStreamsInDataStore\" id=\"acceptOnlyStreamsInDataStore2\" [value]=\"true\" [(ngModel)]=\"appSettings.acceptOnlyStreamsInDataStore\">\n                                                    <label for=\"acceptOnlyStreamsInDataStore2\">\n                                                        Allow Only In Database\n                                                    </label>\n                                                </div>\n                                            </div>\n                                        </div>\n\n                                    </fieldset>\n\n\n                                    <fieldset>\n                                        <legend class=\"text-left\" i18n=\"@@VoDFolder\">VoD Folder</legend>\n                                        <div class=\"form-group\">\n                                            <label class=\"col-sm-2 control-label\">Folder Path</label>\n                                            <div class=\"col-sm-10 text-left\">\n\n                                                <input type=\"text\" required minlength=\"4\" name=\"vodFolderPath\"\n                                                       placeholder=\"Write full path of VoD folder\"\n                                                       class=\"form-control\" [(ngModel)]=\"appSettings.vodFolder\"\n                                                       #vodFolderPath=\"ngModel\">\n                                                <small [hidden]=\"vodFolderPath.valid || (!f.submitted)\"\n                                                       class=\"text-danger\">\n                                                    Folder path should be at least 4 characters.\n                                                </small>\n\n                                            </div>\n                                        </div>\n                                    </fieldset>\n                                    <fieldset>\n                                        <legend class=\"text-left\" i18n=\"@@VoDFolder\">Ministra TV Platform</legend>\n                                        <div class=\"form-group\">\n                                            <label class=\"col-sm-2 control-label\">Options</label>\n                                            <div class=\"col-sm-10 text-left\">\n\n                                                <button class=\"col-sm-2 form-control-static btn btn-default btn-xs\"\n                                                        i18n=\"@@addNewStream\" type=\"button\"\n                                                        (click)=\"importLiveStreams2Stalker()\">\n                                                    <i *ngIf=\"importingLiveStreams\"\n                                                       class=\"fa fa-spinner fa-pulse fa-1x fa-fw\"\n                                                       aria-hidden=\"true\"></i>Import Live Streams\n                                                </button>\n                                                <div class=\"col-xs-1\"></div>\n                                                <button class=\" col-sm-2 form-control-static btn btn-default btn-xs\"\n                                                        i18n=\"@@addNewStream\" type=\"button\"\n                                                        (click)=\"importVoDStreams2Stalker()\">\n                                                    <i *ngIf=\"importingVoDStreams\"\n                                                       class=\"fa fa-spinner fa-pulse fa-1x fa-fw\"\n                                                       aria-hidden=\"true\"></i>Import VoD Streams\n                                                </button>\n\n                                            </div>\n                                        </div>\n\n                                    </fieldset>\n\n                                    <fieldset>\n                                        <legend class=\"text-left\">Adaptive Streaming</legend>\n                                        <div class=\"form-group\" *ngIf=isEnterpriseEdition>\n                                            <label class=\"col-sm-2 control-label\">Streams</label>\n                                            <div class=\"col-sm-10\">\n                                                <ng-container *ngIf=\"appSettings.encoderSettings\">\n                                                    <div class=\"row\" *ngFor=\"let encoderSetting of appSettings.encoderSettings; let i = index\">\n                                                        <div class=\"col-sm-3 text-left\">\n\n                                                            <input type=\"number\" [name]=\"'resolutionHeight'+i\" class=\"form-control\" [(ngModel)]=\"encoderSetting.height\">\n                                                            <span class=\"help-block\" i18n=\"@@ResolutionHeight\">Video Resolution Height</span>\n                                                        </div>\n                                                        <div class=\"col-sm-3 text-left\">\n                                                            <input type=\"number\" [name]=\"'videobitrate'+i\" class=\"form-control\" [(ngModel)]=\"encoderSetting.videoBitrate\">\n                                                            <span class=\"help-block\" i18n=\"@@VideoBitrate\">Video Bitrate(bps)</span>\n                                                        </div>\n                                                        <div class=\"col-sm-3 text-left\">\n                                                            <input type=\"number\" [name]=\"'audiobitrate'+i\" class=\"form-control\" [(ngModel)]=\"encoderSetting.audioBitrate\">\n                                                            <span class=\"help-block\" i18n=\"@@AudioBitrate\">Audio Bitrate(bps)</span>\n                                                        </div>\n\n                                                        <button type=\"button\" class=\"col-sm-1 btn btn-icon btn-simple btn-danger btn-minus\" (click)=\"deleteStream(i)\">\n                                                            <i class=\"ti-close\"></i>\n                                                        </button>\n                                                    </div>\n                                                </ng-container>\n                                                <button class=\"col-sm-3 form-control-static btn btn-default\" i18n=\"@@addNewStream\" type=\"button\" (click)=\"addNewStream()\">\n                                                    Add New Stream\n                                                </button>\n                                            </div>\n                                        </div>\n                                        <div class=\"form-group text-center\" *ngIf=!isEnterpriseEdition>\n                                            <p i18n=\"not Enterprise@@notEnterpriseEditionMessage\">\n                                                These features are enabled in\n                                                <a href=\"http://antmedia.io\">Ant Media Server Enterprise Edition.</a>\n                                            </p>\n                                        </div>\n                                    </fieldset>\n\n\n                                    <fieldset>\n                                        <legend class=\"text-left\" i18n=\"@@SocialSharing\">Social Network Sharing</legend>\n                                        <div class=\"form-group\">\n\n                                            <div class=\"row\"\n                                                 *ngFor=\"let endpoint of videoServiceEndpoints; let i = index\">\n\n                                                <label class=\"col-sm-2 control-label\">\n                                                    <ng-container [ngSwitch]=\"endpoint.serviceName\">\n                                                        <ng-container *ngSwitchCase=\"'facebook'\">\n                                                            <i class=\"ti-facebook\" style=\"color:#3b5998\">&nbsp;</i>Facebook\n                                                        </ng-container>\n                                                        <ng-container *ngSwitchCase=\"'youtube'\">\n                                                            <i class=\"ti-youtube\" style=\"color:#e52d27\">&nbsp;</i>Youtube\n                                                        </ng-container>\n                                                        <ng-container *ngSwitchCase=\"'periscope'\">\n                                                            <i class=\"ti-twitter-alt\" style=\"color:#55acee\">&nbsp;</i>Periscope\n                                                        </ng-container>\n                                                    </ng-container>\n                                                </label>\n\n                                                <div class=\"col-sm-3 form-control-static\">\n\n                                                    {{endpoint.accountName}}\n                                                    <button class=\"btn btn-simple btn-danger btn-xs\"\n                                                            style=\"text-align:left\" i18n=\"@@YoutubeAuthenticated\"\n                                                            type=\"button\" (click)=\"revokeSocialMediaAuth(endpoint.id)\">\n                                                        <i class=\"ti-unlink\"></i>Remove\n                                                    </button>\n\n\n                                                </div>\n\n                                            </div>\n\n                                        </div>\n\n                                        <div class=\"form-group\" style=\"margin-top:10px\">\n\n                                            <div class=\"row\">\n                                                <label class=\"col-sm-2 control-label\">\n                                                    Add Account\n                                                </label>\n\n                                                <button class=\"col-sm-2 form-control-static btn btn-simple btn-success\"\n                                                        style=\"margin-right:10px\" i18n=\"@@authenticate\" type=\"button\"\n                                                        (click)=\"getSocialMediaAuthParameters('facebook')\">\n\n                                                    <i class=\"ti-facebook\"></i> Add Facebook\n                                                </button>\n\n                                                <button class=\"col-sm-2 form-control-static btn btn-simple btn-success \"\n                                                        style=\"margin-right:10px\" i18n=\"@@authenticate\" type=\"button\"\n                                                        (click)=\"getSocialMediaAuthParameters('youtube')\">\n\n                                                    <i class=\"ti-youtube\"></i> Add Youtube\n                                                </button>\n\n                                                <button class=\"col-sm-2 form-control-static btn btn-simple btn-success \"\n                                                        style=\"margin-right:10px\" i18n=\"@@authenticate\" type=\"button\"\n                                                        (click)=\"getSocialMediaAuthParameters('periscope')\">\n\n\n                                                    <i class=\"ti-twitter-alt\"></i> Add Periscope\n                                                </button>\n                                            </div>\n                                            <div class=\"row\">\n                                                <a class=\"btn btn-simple col-sm-12\" *ngIf=\"gettingDeviceParameters\">\n                                                    <i class=\"fa fa-spinner fa-pulse fa-1x fa-fw\" aria-hidden=\"true\"></i>\n                                                </a>\n                                                <a class=\"btn btn-simple col-sm-12\" *ngIf=\"waitingForConfirmation\">\n                                                    <i class=\"fa fa-spinner fa-pulse fa-1x fa-fw\" aria-hidden=\"true\"></i>\n                                                    Waiting for confirmation\n                                                </a>\n                                            </div>\n\n                                        </div>\n\n                                    </fieldset>\n\n\n                                    <fieldset>\n                                        <div class=\"form-group text-center\">\n                                            <button type=\"submit\" class=\"btn btn-fill btn-info\" i18n=\"@@saveForm\">Save</button>\n                                        </div>\n                                    </fieldset>\n                                </form>\n\n\n\n                            </div>\n                        </div>\n\n\n\n                        <div class=\"footer\">\n                            <!--\n                        <hr />\n\n                        <div class=\"stats\">\n                               <button class=\"btn btn-primary btn-simple btn-sm\" (click)=\"updateCPULoad()\">\n                                   <i class=\"ti-reload\"></i>Update now</button>\n                        </div>\n                    -->\n                        </div>\n                    </div>\n                </div>\n            </div>\n\n\n        </div>\n    </div>\n\n\n\n</div>"

/***/ }),

/***/ "../../../../../src/app/app.page/app.page.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export HLSListType */
/* unused harmony export Camera */
/* unused harmony export AppSettings */
/* unused harmony export SocialNetworkChannel */
/* unused harmony export SearchParam */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AppPageComponent; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return CamSettinsDialogComponent; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return UploadVodDialogComponent; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return BroadcastEditComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_platform_browser__ = __webpack_require__("../../../platform-browser/esm5/platform-browser.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common_http__ = __webpack_require__("../../../common/esm5/http.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_router__ = __webpack_require__("../../../router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__ = __webpack_require__("../../../../../src/app/rest/rest.service.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_ngx_clipboard__ = __webpack_require__("../../../../ngx-clipboard/dist/ngx-clipboard.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__locale_locale__ = __webpack_require__("../../../../../src/app/locale/locale.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__angular_material__ = __webpack_require__("../../../material/esm5/material.es5.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8_rxjs_add_operator_toPromise__ = __webpack_require__("../../../../rxjs/_esm5/add/operator/toPromise.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8_rxjs_add_operator_toPromise___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_8_rxjs_add_operator_toPromise__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__dialog_detected_objects_list__ = __webpack_require__("../../../../../src/app/app.page/dialog/detected.objects.list.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var __generator = (this && this.__generator) || function (thisArg, body) {
    var _ = { label: 0, sent: function() { if (t[0] & 1) throw t[1]; return t[1]; }, trys: [], ops: [] }, f, y, t, g;
    return g = { next: verb(0), "throw": verb(1), "return": verb(2) }, typeof Symbol === "function" && (g[Symbol.iterator] = function() { return this; }), g;
    function verb(n) { return function (v) { return step([n, v]); }; }
    function step(op) {
        if (f) throw new TypeError("Generator is already executing.");
        while (_) try {
            if (f = 1, y && (t = y[op[0] & 2 ? "return" : op[0] ? "throw" : "next"]) && !(t = t.call(y, op[1])).done) return t;
            if (y = 0, t) op = [0, t.value];
            switch (op[0]) {
                case 0: case 1: t = op; break;
                case 4: _.label++; return { value: op[1], done: false };
                case 5: _.label++; y = op[1]; op = [0]; continue;
                case 7: op = _.ops.pop(); _.trys.pop(); continue;
                default:
                    if (!(t = _.trys, t = t.length > 0 && t[t.length - 1]) && (op[0] === 6 || op[0] === 2)) { _ = 0; continue; }
                    if (op[0] === 3 && (!t || (op[1] > t[0] && op[1] < t[3]))) { _.label = op[1]; break; }
                    if (op[0] === 6 && _.label < t[1]) { _.label = t[1]; t = op; break; }
                    if (t && _.label < t[2]) { _.label = t[2]; _.ops.push(op); break; }
                    if (t[2]) _.ops.pop();
                    _.trys.pop(); continue;
            }
            op = body.call(thisArg, _);
        } catch (e) { op = [6, e]; y = 0; } finally { f = t = 0; }
        if (op[0] & 5) throw op[1]; return { value: op[0] ? op[1] : void 0, done: true };
    }
};










var ERROR_SOCIAL_ENDPOINT_UNDEFINED_CLIENT_ID = -1;
var ERROR_SOCIAL_ENDPOINT_UNDEFINED_ENDPOINT = -2;
var flowplayer = __webpack_require__("../../../../flowplayer/dist/flowplayer.js");
var engine = __webpack_require__("../../../../flowplayer-hlsjs/flowplayer.hlsjs.js");
engine(flowplayer);
var LIVE_STREAMING_NOT_ENABLED = "LIVE_STREAMING_NOT_ENABLED";
var AUTHENTICATION_TIMEOUT = "AUTHENTICATION_TIMEOUT";
var HLSListType = /** @class */ (function () {
    function HLSListType(name, value) {
        this.name = name;
        this.value = value;
    }
    return HLSListType;
}());

var Camera = /** @class */ (function () {
    function Camera(name, ipAddr, username, password, streamUrl, type) {
        this.name = name;
        this.ipAddr = ipAddr;
        this.username = username;
        this.password = password;
        this.streamUrl = streamUrl;
        this.type = type;
    }
    return Camera;
}());

var AppSettings = /** @class */ (function () {
    function AppSettings(mp4MuxingEnabled, addDateTimeToMp4FileName, hlsMuxingEnabled, hlsListSize, hlsTime, hlsPlayListType, facebookClientId, facebookClientSecret, youtubeClientId, youtubeClientSecret, periscopeClientId, periscopeClientSecret, encoderSettings, acceptOnlyStreamsInDataStore, vodFolder) {
        this.mp4MuxingEnabled = mp4MuxingEnabled;
        this.addDateTimeToMp4FileName = addDateTimeToMp4FileName;
        this.hlsMuxingEnabled = hlsMuxingEnabled;
        this.hlsListSize = hlsListSize;
        this.hlsTime = hlsTime;
        this.hlsPlayListType = hlsPlayListType;
        this.facebookClientId = facebookClientId;
        this.facebookClientSecret = facebookClientSecret;
        this.youtubeClientId = youtubeClientId;
        this.youtubeClientSecret = youtubeClientSecret;
        this.periscopeClientId = periscopeClientId;
        this.periscopeClientSecret = periscopeClientSecret;
        this.encoderSettings = encoderSettings;
        this.acceptOnlyStreamsInDataStore = acceptOnlyStreamsInDataStore;
        this.vodFolder = vodFolder;
    }
    return AppSettings;
}());

var SocialNetworkChannel = /** @class */ (function () {
    function SocialNetworkChannel() {
    }
    return SocialNetworkChannel;
}());

var SearchParam = /** @class */ (function () {
    function SearchParam() {
    }
    return SearchParam;
}());

var AppPageComponent = /** @class */ (function () {
    function AppPageComponent(http, route, restService, clipBoardService, renderer, router, zone, dialog, sanitizer, cdr, matpage) {
        this.http = http;
        this.route = route;
        this.restService = restService;
        this.clipBoardService = clipBoardService;
        this.renderer = renderer;
        this.router = router;
        this.zone = zone;
        this.dialog = dialog;
        this.sanitizer = sanitizer;
        this.cdr = cdr;
        this.matpage = matpage;
        this.newLiveStreamCreating = false;
        this.newIPCameraAdding = false;
        this.newStreamSourceAdding = false;
        this.newStreamSourceWarn = false;
        this.discoveryStarted = false;
        this.newSourceAdding = false;
        this.isEnterpriseEdition = false;
        this.gettingDeviceParameters = false;
        this.waitingForConfirmation = false;
        this.noCamWarning = false;
        this.isGridView = false;
        this.searchWarning = false;
        this.showVodButtons = false;
        this.userFBPagesLoading = false;
        this.liveStreamUpdating = false;
        this.listTypes = [
            new HLSListType('None', ''),
            new HLSListType('Event', 'event'),
        ];
        this.displayedColumnsStreams = ['name', 'status', 'social_media', 'actions'];
        this.displayedColumnsVod = ['name', 'type', 'date', 'actions'];
        this.displayedColumnsUserVod = ['name', 'date', 'actions'];
        this.streamsPageSize = 10;
        this.vodPageSize = 10;
        this.pageSize = 10;
        this.pageSizeOptions = [10, 25, 50];
        this.streamListOffset = 0;
        this.vodListOffset = 0;
        this.importingLiveStreams = false;
        this.importingVoDStreams = false;
        this.dataSource = new __WEBPACK_IMPORTED_MODULE_7__angular_material__["I" /* MatTableDataSource */]();
        this.dataSourceVod = new __WEBPACK_IMPORTED_MODULE_7__angular_material__["I" /* MatTableDataSource */]();
    }
    AppPageComponent.prototype.setPageSizeOptions = function (setPageSizeOptionsInput) {
        this.pageSizeOptions = setPageSizeOptionsInput.split(',').map(function (str) { return +str; });
    };
    AppPageComponent.prototype.ngOnInit = function () {
        var _this = this;
        //  Init Bootstrap Select Picker
        if ($(".selectpicker").length != 0) {
            $(".selectpicker").selectpicker({
                iconBase: "ti",
                tickIcon: "ti-check"
            });
        }
        $('.datepicker').datetimepicker({
            format: 'YYYY-MM-DD',
            icons: {
                time: "fa fa-clock-o",
                date: "fa fa-calendar",
                up: "fa fa-chevron-up",
                down: "fa fa-chevron-down",
                previous: 'fa fa-chevron-left',
                next: 'fa fa-chevron-right',
                today: 'fa fa-screenshot',
                clear: 'fa fa-trash',
                close: 'fa fa-remove'
            }
        });
        var self = this;
        this.zone.run(function () {
            $('#selectBox').change(function () {
                var val = $(this).val();
                console.log(val);
                self.filterAppLiveStreams(val);
            });
        });
        this.broadcastTableData = {
            dataRows: [],
        };
        this.gridTableData = {
            list: []
        };
        this.vodTableData = {
            dataRows: []
        };
        this.broadcastTempTable = {
            dataRows: [],
        };
        this.broadcastGridTableData = {
            dataRows: [],
        };
        this.liveBroadcast = new __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["c" /* LiveBroadcast */]();
        this.selectedBroadcast = new __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["c" /* LiveBroadcast */]();
        this.liveBroadcast.name = "";
        this.liveBroadcast.type = "";
        this.liveBroadcastShareFacebook = false;
        this.liveBroadcastShareYoutube = false;
        this.liveBroadcastSharePeriscope = false;
        this.searchParam = new SearchParam();
        this.appSettings = null;
        this.newLiveStreamActive = false;
        this.camera = new Camera("", "", "", "", "", "");
        this.timerId = window.setInterval(function () {
            _this.getAppLiveStreams(_this.streamListOffset, _this.pageSize);
            _this.getVoDStreams();
        }, 10000);
    };
    AppPageComponent.prototype.onPaginateChange = function (event) {
        var _this = this;
        console.log("page index:" + event.pageIndex);
        console.log("length:" + event.length);
        console.log("page size:" + event.pageSize);
        this.vodListOffset = event.pageIndex * event.pageSize;
        this.pageSize = event.pageSize;
        this.keyword = null;
        this.restService.getVodList(this.appName, this.vodListOffset, this.pageSize).subscribe(function (data) {
            _this.vodTableData.dataRows = [];
            for (var i in data) {
                _this.vodTableData.dataRows.push(data[i]);
            }
            _this.dataSourceVod = new __WEBPACK_IMPORTED_MODULE_7__angular_material__["I" /* MatTableDataSource */](_this.vodTableData.dataRows);
        });
    };
    AppPageComponent.prototype.onListPaginateChange = function (event) {
        console.log("page index:" + event.pageIndex);
        console.log("length:" + event.length);
        console.log("page size:" + event.pageSize);
        this.pageSize = event.pageSize;
        this.streamListOffset = event.pageIndex;
        this.getAppLiveStreams(event.pageIndex, this.pageSize);
    };
    AppPageComponent.prototype.onGridPaginateChange = function (event) {
        console.log("page index:" + event.pageIndex);
        console.log("length:" + event.length);
        console.log("page size:" + event.pageSize);
        this.pageSize = event.pageSize;
        this.openGridPlayers(event.pageIndex, this.pageSize);
    };
    AppPageComponent.prototype.ngAfterViewInit = function () {
        var _this = this;
        this.timerId = null;
        this.cdr.detectChanges();
        this.sub = this.route.params.subscribe(function (params) {
            _this.appName = params['appname']; // (+) converts string 'id' to a number
            if (typeof _this.appName == "undefined") {
                _this.restService.getApplications().subscribe(function (data) {
                    //second element is the Applications. It is not safe to make static binding.
                    for (var i in data['applications']) {
                        //console.log(data['applications'][i]);
                        _this.router.navigateByUrl("/applications/" + data['applications'][i]);
                        break;
                    }
                });
                return;
            }
            _this.getSettings();
            _this.restService.isEnterpriseEdition().subscribe(function (data) {
                _this.isEnterpriseEdition = data["success"];
            });
            _this.getAppLiveStreamsNumber();
            _this.getVoDStreams();
            _this.getAppLiveStreams(0, _this.pageSize);
        });
    };
    AppPageComponent.prototype.changeApplication = function () {
        this.clearTimer();
        this.getAppLiveStreamsNumber();
        this.getVoDStreams();
        this.getAppLiveStreams(0, this.pageSize);
    };
    AppPageComponent.prototype.applyFilter = function (filterValue) {
        filterValue = filterValue.trim(); // Remove whitespace
        filterValue = filterValue.toLowerCase(); // Datasource defaults to lowercase matches
        this.dataSource.filter = filterValue;
    };
    AppPageComponent.prototype.applyFilterVod = function (filterValue) {
        filterValue = filterValue.trim(); // Remove whitespace
        filterValue = filterValue.toLowerCase(); // Datasource defaults to lowercase matches
        this.dataSourceVod.filter = filterValue;
    };
    AppPageComponent.prototype.openSettingsDialog = function (selected) {
        var _this = this;
        this.selectedBroadcast = selected;
        var dialogRef = this.dialog.open(CamSettinsDialogComponent, {
            width: '300px',
            data: {
                name: this.selectedBroadcast.name, url: this.selectedBroadcast.ipAddr,
                username: this.selectedBroadcast.username, pass: this.selectedBroadcast.password, id: this.selectedBroadcast.streamId,
                status: this.selectedBroadcast.status, appName: this.appName
            }
        });
        dialogRef.afterClosed().subscribe(function (result) {
            console.log('The dialog was closed');
            _this.getAppLiveStreams(_this.streamListOffset, _this.pageSize);
            _this.getAppLiveStreamsNumber();
        });
    };
    AppPageComponent.prototype.openVodUploadDialog = function () {
        var _this = this;
        var dialogRef = this.dialog.open(UploadVodDialogComponent, {
            data: { appName: this.appName },
            width: '300px'
        });
        dialogRef.afterClosed().subscribe(function (result) {
            console.log('The dialog was closed');
            _this.getVoDStreams();
        });
    };
    AppPageComponent.prototype.openBroadcastEditDialog = function (stream) {
        var _this = this;
        if (stream.endPointList != null) {
            this.editBroadcastShareFacebook = false;
            this.editBroadcastShareYoutube = false;
            this.editBroadcastSharePeriscope = false;
            stream.endPointList.forEach(function (element) {
                switch (element.type) {
                    case "facebook":
                        _this.editBroadcastShareFacebook = true;
                        break;
                    case "youtube":
                        _this.editBroadcastShareYoutube = true;
                        break;
                    case "periscope":
                        _this.editBroadcastSharePeriscope = true;
                        break;
                }
            });
        }
        if (this.liveStreamEditing == null || stream.streamId != this.liveStreamEditing.streamId || stream.name != this.liveStreamEditing.name) {
            this.liveStreamEditing = new __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["c" /* LiveBroadcast */]();
            this.liveStreamEditing.streamId = stream.streamId;
            this.liveStreamEditing.name = stream.name;
            this.liveStreamEditing.description = "";
        }
        if (this.liveStreamEditing) {
            var dialogRef = this.dialog.open(BroadcastEditComponent, {
                data: {
                    name: this.liveStreamEditing.name,
                    streamId: this.liveStreamEditing.streamId,
                    appName: this.appName,
                    endpointList: stream.endPointList,
                    videoServiceEndpoints: this.videoServiceEndpoints,
                    editBroadcastShareFacebook: this.editBroadcastShareFacebook,
                    editBroadcastShareYoutube: this.editBroadcastShareYoutube,
                    editBroadcastSharePeriscope: this.editBroadcastSharePeriscope,
                }
            });
            dialogRef.afterClosed().subscribe(function (result) {
                console.log('The dialog was closed');
                _this.getAppLiveStreams(_this.streamListOffset, _this.pageSize);
                _this.getAppLiveStreamsNumber();
            });
        }
    };
    AppPageComponent.prototype.test = function () {
        alert("test");
    };
    AppPageComponent.prototype.getAppLiveStreams = function (offset, size) {
        var _this = this;
        offset = offset * size;
        this.restService.getAppLiveStreams(this.appName, offset, size).subscribe(function (data) {
            _this.broadcastTableData.dataRows = [];
            for (var i in data) {
                var endpoint = [];
                for (var j in data[i].endPointList) {
                    endpoint.push(data[i].endPointList[j]);
                }
                _this.broadcastTableData.dataRows.push(data[i]);
                _this.broadcastTableData.dataRows[i].iframeSource = __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["b" /* HTTP_SERVER_ROOT */] + _this.appName + "/play.html?name=" + _this.broadcastTableData.dataRows[i].streamId + "&autoplay=true";
            }
            _this.dataSource = new __WEBPACK_IMPORTED_MODULE_7__angular_material__["I" /* MatTableDataSource */](_this.broadcastTableData.dataRows);
        });
    };
    AppPageComponent.prototype.cleanURL = function (oldURL) {
        console.log("clean url");
        return this.sanitizer.bypassSecurityTrustResourceUrl(oldURL);
    };
    AppPageComponent.prototype.filterAppLiveStreams = function (type) {
        var _this = this;
        if (type == "displayAll") {
            this.getAppLiveStreams(0, 50);
        }
        else {
            this.restService.filterAppLiveStreams(this.appName, 0, 10, type).subscribe(function (data) {
                //console.log(data);
                _this.broadcastTableData.dataRows = [];
                console.log("type of data -> " + typeof data);
                for (var i in data) {
                    _this.broadcastTableData.dataRows.push(data[i]);
                }
                if (_this.isGridView) {
                    setTimeout(function () {
                        _this.openGridPlayers(0, 4);
                    }, 500);
                }
                setTimeout(function () {
                    $('[data-toggle="tooltip"]').tooltip();
                }, 500);
            });
        }
    };
    AppPageComponent.prototype.getAppLiveStreamsNumber = function () {
        var _this = this;
        this.restService.getTotalBroadcastNumber(this.appName).subscribe(function (data) {
            _this.listLength = data;
        });
    };
    AppPageComponent.prototype.getVoDStreams = function () {
        var _this = this;
        this.searchWarning = false;
        this.keyword = null;
        //this for getting full length of vod streams for paginations
        this.restService.getTotalVodNumber(this.appName).subscribe(function (data) {
            _this.vodLength = data;
            console.log("vod table length: " + _this.vodLength);
        });
        this.restService.getVodList(this.appName, this.vodListOffset, this.pageSize).subscribe(function (data) {
            _this.vodTableData.dataRows = [];
            for (var i in data) {
                _this.vodTableData.dataRows.push(data[i]);
            }
            _this.dataSourceVod = new __WEBPACK_IMPORTED_MODULE_7__angular_material__["I" /* MatTableDataSource */](_this.vodTableData.dataRows);
        });
    };
    AppPageComponent.prototype.clearTimer = function () {
        if (this.timerId) {
            clearInterval(this.timerId);
        }
    };
    AppPageComponent.prototype.ngOnDestroy = function () {
        this.sub.unsubscribe();
        if (this.timerId) {
            clearInterval(this.timerId);
        }
    };
    AppPageComponent.prototype.getVoD = function () {
        this.getVoDStreams();
    };
    AppPageComponent.prototype.isMobileMenu = function () {
        if ($(window).width() > 991) {
            return true;
        }
        return false;
    };
    AppPageComponent.prototype.importLiveStreams2Stalker = function () {
        var _this = this;
        this.importingLiveStreams = true;
        this.restService.importLiveStreams2Stalker(this.appName).subscribe(function (data) {
            console.log(data);
            _this.importingLiveStreams = false;
            var message = __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().streams_imported_successfully;
            var type = "success";
            var delay = 500;
            var icon = "ti-save";
            if (!data["success"]) {
                icon = "ti-alert";
                if (data["errorId"] == 404) {
                    message = __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().missing_configuration_parameter_for_stalker;
                }
                else {
                    message = __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().error_occured;
                }
                type = "warning";
                delay = 1900;
            }
            $.notify({
                icon: icon,
                message: message
            }, {
                type: type,
                delay: delay,
                placement: {
                    from: 'top',
                    align: 'right'
                }
            });
        });
    };
    AppPageComponent.prototype.importVoDStreams2Stalker = function () {
        var _this = this;
        this.importingVoDStreams = true;
        this.restService.importVoDStreams2Stalker(this.appName).subscribe(function (data) {
            console.log(data);
            _this.importingVoDStreams = false;
            var message = __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().streams_imported_successfully;
            var type = "success";
            var delay = 500;
            var icon = "ti-save";
            if (!data["success"]) {
                icon = "ti-alert";
                if (data["errorId"] == 404) {
                    message = __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().missing_configuration_parameter_for_stalker;
                }
                else {
                    message = __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().error_occured;
                }
                type = "warning";
                delay = 1900;
            }
            $.notify({
                icon: icon,
                message: message
            }, {
                type: type,
                delay: delay,
                placement: {
                    from: 'top',
                    align: 'right'
                }
            });
        });
    };
    AppPageComponent.prototype.checkAndPlayLive = function (videoUrl) {
        var _this = this;
        this.http.get(videoUrl, { responseType: 'text' }).subscribe(function (data) {
            console.log("loaded...");
            $("#playerLoading").hide();
            flowplayer('#player', {
                autoplay: true,
                clip: {
                    sources: [{
                            type: 'application/x-mpegurl',
                            src: videoUrl
                        }]
                }
            });
        }, function (error) {
            console.log("error...");
            setTimeout(function () {
                _this.checkAndPlayLive(videoUrl);
            }, 5000);
        });
    };
    AppPageComponent.prototype.showDetectedObject = function (streamId) {
        var dialogRef = this.dialog.open(__WEBPACK_IMPORTED_MODULE_9__dialog_detected_objects_list__["a" /* DetectedObjectListDialog */], {
            width: '800px',
            data: {
                streamId: streamId,
                appName: this.appName
            }
        });
    };
    AppPageComponent.prototype.playLive = function (streamId) {
        var _this = this;
        var id, name, srcFile, iframeSource;
        var htmlCode = '<iframe id="' + streamId + '"frameborder="0" allowfullscreen="true"  seamless="seamless" style="display:block; width:100%; height:400px;"></iframe>';
        console.log(htmlCode);
        swal({
            html: htmlCode,
            showConfirmButton: false,
            width: '800px',
            height: '400px',
            padding: 10,
            animation: false,
            showCloseButton: true,
            onOpen: function () {
            },
            onClose: function () {
                var ifr = document.getElementById(streamId);
                ifr.parentNode.removeChild(ifr);
            }
        }).then(function () { }, function () { });
        setTimeout(function () {
            iframeSource = __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["b" /* HTTP_SERVER_ROOT */] + _this.appName + "/play.html?name=" + streamId + "&autoplay=true";
            var $iframe = $('#' + streamId);
            $iframe.prop('src', iframeSource);
        }, 1500);
    };
    AppPageComponent.prototype.openGridPlayers = function (index, size) {
        var _this = this;
        var id, name, srcFile, iframeSource;
        index = index * size;
        this.restService.getAppLiveStreams(this.appName, index, size).subscribe(function (data) {
            //console.log(data);
            _this.broadcastGridTableData.dataRows = [];
            //console.log("type of data -> " + typeof data);
            for (var i in data) {
                var endpoint = [];
                for (var j in data[i].endPointList) {
                    endpoint.push(data[i].endPointList[j]);
                }
                _this.broadcastGridTableData.dataRows.push(data[i]);
                // console.log("iframe source:  "+this.broadcastTableData.dataRows[i].iframeSource);
            }
        });
        setTimeout(function () {
            for (var i in _this.broadcastGridTableData.dataRows) {
                id = _this.broadcastGridTableData.dataRows[i]['streamId'];
                iframeSource = __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["b" /* HTTP_SERVER_ROOT */] + _this.appName + "/play.html?name=" + id + "&autoplay=true";
                var $iframe = $('#' + id);
                $iframe.prop('src', iframeSource);
            }
        }, 1500);
    };
    AppPageComponent.prototype.closeGridPlayers = function () {
        var id;
        for (var i in this.broadcastGridTableData.dataRows) {
            id = this.broadcastGridTableData.dataRows[i]['streamId'];
            var container = document.getElementById(id);
            flowplayer(container).shutdown();
            $("#" + id).html("").attr('class', +'');
        }
    };
    AppPageComponent.prototype.playVoD = function (streamName, type) {
        // var container = document.getElementById("player");
        // install flowplayer into selected container
        var srcFile = null;
        if (type == "streamVod" || type == "uploadedVod") {
            srcFile = __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["b" /* HTTP_SERVER_ROOT */] + this.appName + '/streams/' + streamName;
        }
        else if (type == "userVod") {
            var lastSlashIndex = this.appSettings.vodFolder.lastIndexOf("/");
            var folderName = this.appSettings.vodFolder.substring(lastSlashIndex);
            srcFile = __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["b" /* HTTP_SERVER_ROOT */] + this.appName + '/streams/' + folderName + '/' + streamName;
        }
        if (srcFile != null) {
            swal({
                html: '<div id="player"></div>',
                showConfirmButton: false,
                width: '800px',
                animation: false,
                onOpen: function () {
                    flowplayer('#player', {
                        autoplay: true,
                        clip: {
                            sources: [{
                                    type: 'video/mp4',
                                    src: srcFile
                                }
                            ]
                        }
                    });
                },
                onClose: function () {
                    flowplayer("#player").shutdown();
                }
            });
        }
        else {
            console.error("Undefined type");
        }
    };
    //file with extension
    AppPageComponent.prototype.deleteVoD = function (fileName, vodId) {
        var _this = this;
        var VoDName = fileName.substring(0, fileName.lastIndexOf("."));
        swal({
            title: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().are_you_sure,
            text: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().wont_be_able_to_revert,
            type: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#3085d6',
            cancelButtonColor: '#d33',
            confirmButtonText: 'Yes, delete it!'
        }).then(function () {
            _this.restService.deleteVoDFile(_this.appName, VoDName, vodId).subscribe(function (data) {
                if (data["success"] == true) {
                }
                else {
                    _this.showVoDFileNotDeleted();
                }
                ;
                _this.getVoDStreams();
            });
        }).catch(function () {
        });
    };
    AppPageComponent.prototype.showVoDFileNotDeleted = function () {
        $.notify({
            icon: "ti-save",
            message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().vodFileNotDeleted
        }, {
            type: "warning",
            delay: 900,
            placement: {
                from: 'top',
                align: 'right'
            }
        });
    };
    AppPageComponent.prototype.editLiveBroadcast = function (stream) {
        var _this = this;
        if (stream.endPointList != null) {
            this.editBroadcastShareFacebook = false;
            this.editBroadcastShareYoutube = false;
            this.editBroadcastSharePeriscope = false;
            stream.endPointList.forEach(function (element) {
                switch (element.type) {
                    case "facebook":
                        _this.editBroadcastShareFacebook = true;
                        break;
                    case "youtube":
                        _this.editBroadcastShareYoutube = true;
                        break;
                    case "periscope":
                        _this.editBroadcastSharePeriscope = true;
                        break;
                }
            });
        }
        if (this.liveStreamEditing == null || stream.streamId != this.liveStreamEditing.streamId) {
            this.liveStreamEditing = new __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["c" /* LiveBroadcast */]();
            this.liveStreamEditing.streamId = stream.streamId;
            this.liveStreamEditing.name = stream.name;
            this.liveStreamEditing.description = "";
        }
        else {
            this.liveStreamEditing = null;
        }
    };
    AppPageComponent.prototype.updateLiveStream = function (isValid) {
        var _this = this;
        if (!isValid) {
            return;
        }
        this.liveStreamUpdating = true;
        var socialNetworks = [];
        if (this.editBroadcastShareFacebook) {
            socialNetworks.push("facebook");
        }
        if (this.editBroadcastShareYoutube == true) {
            socialNetworks.push("youtube");
        }
        if (this.editBroadcastSharePeriscope == true) {
            socialNetworks.push("periscope");
        }
        this.restService.updateLiveStream(this.appName, this.liveStreamEditing, socialNetworks).subscribe(function (data) {
            _this.liveStreamUpdating = false;
            console.log(data["success"]);
            if (data["success"]) {
                _this.liveStreamEditing = null;
                //update the rows
                _this.getAppLiveStreams(_this.streamListOffset, _this.pageSize);
                _this.getAppLiveStreamsNumber();
                $.notify({
                    icon: "ti-save",
                    message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().broadcast_updated
                }, {
                    type: "success",
                    delay: 900,
                    placement: {
                        from: 'top',
                        align: 'right'
                    }
                });
            }
            else {
                $.notify({
                    icon: "ti-alert",
                    message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().broadcast_not_updated + " " + data["message"] + " " + data["errorId"]
                }, {
                    type: "warning",
                    delay: 900,
                    placement: {
                        from: 'top',
                        align: 'right'
                    }
                });
            }
        });
    };
    AppPageComponent.prototype.deleteLiveBroadcast = function (streamId) {
        var _this = this;
        swal({
            title: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().are_you_sure,
            text: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().wont_be_able_to_revert,
            type: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#3085d6',
            cancelButtonColor: '#d33',
            confirmButtonText: 'Yes, delete it!'
        }).then(function (data) {
            _this.restService.deleteBroadcast(_this.appName, streamId)
                .subscribe(function (data) {
                if (data["success"] == true) {
                    $.notify({
                        icon: "ti-save",
                        message: "Successfully deleted"
                    }, {
                        type: "success",
                        delay: 900,
                        placement: {
                            from: 'top',
                            align: 'right'
                        }
                    });
                }
                else {
                    $.notify({
                        icon: "ti-save",
                        message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().broadcast_not_deleted
                    }, {
                        type: "warning",
                        delay: 900,
                        placement: {
                            from: 'top',
                            align: 'right'
                        }
                    });
                }
                ;
                _this.getAppLiveStreams(_this.streamListOffset, _this.pageSize);
                _this.getAppLiveStreamsNumber();
                if (_this.isGridView) {
                    setTimeout(function () {
                        _this.switchToGridView();
                    }, 500);
                }
            });
        });
    };
    AppPageComponent.prototype.addNewStream = function () {
        if (!this.appSettings.encoderSettings) {
            this.appSettings.encoderSettings = [];
        }
        this.appSettings.encoderSettings.push({
            height: 0,
            videoBitrate: 0,
            audioBitrate: 0
        });
    };
    AppPageComponent.prototype.deleteStream = function (index) {
        this.appSettings.encoderSettings.splice(index, 1);
    };
    AppPageComponent.prototype.setSocialNetworkChannel = function (endpointId, type, value) {
        var _this = this;
        this.restService.setSocialNetworkChannel(this.appName, endpointId, type, value).subscribe(function (data) {
            console.log("set social network channel: " + data["success"]);
            if (data["success"]) {
                _this.getSocialEndpoints();
            }
        });
    };
    AppPageComponent.prototype.showChannelChooserDialog = function (options, endpointId, type) {
        return __awaiter(this, void 0, void 0, function () {
            var _this = this;
            var id;
            return __generator(this, function (_a) {
                switch (_a.label) {
                    case 0: return [4 /*yield*/, swal({
                            title: 'Select the target to publish',
                            input: 'select',
                            inputOptions: options,
                            inputPlaceholder: 'Select the Page',
                            showCancelButton: true,
                            inputValidator: function (value) {
                                return new Promise(function (resolve) {
                                    if (value) {
                                        console.log("selected id: " + value);
                                        _this.setSocialNetworkChannel(endpointId, type, value);
                                        resolve();
                                    }
                                    else {
                                        console.log("not item selected");
                                        resolve();
                                    }
                                });
                            },
                        })];
                    case 1:
                        id = (_a.sent()).value;
                        return [2 /*return*/, null];
                }
            });
        });
    };
    AppPageComponent.prototype.showNetworkChannelList = function (endpointId, type) {
        var _this = this;
        this.userFBPagesLoading = true;
        this.restService.getSocialNetworkChannelList(this.appName, endpointId, type).subscribe(function (data) {
            console.log(data);
            var options = {};
            for (var i in data) {
                options[data[i]["id"]] = data[i]["name"];
            }
            _this.userFBPagesLoading = false;
            _this.showChannelChooserDialog(options, endpointId, type);
        });
    };
    AppPageComponent.prototype.getSocialEndpoints = function () {
        var _this = this;
        this.restService.getSocialEndpoints(this.appName).subscribe(function (data) {
            _this.videoServiceEndpoints = [];
            for (var i in data) {
                console.log(data[i]);
                _this.videoServiceEndpoints.push(data[i]);
            }
        });
    };
    AppPageComponent.prototype.getSettings = function () {
        var _this = this;
        this.restService.getSettings(this.appName).subscribe(function (data) {
            _this.appSettings = data;
        });
        this.getSocialEndpoints();
    };
    AppPageComponent.prototype.changeSettings = function (valid) {
        if (!valid) {
            return;
        }
        this.restService.changeSettings(this.appName, this.appSettings).subscribe(function (data) {
            if (data["success"] == true) {
                $.notify({
                    icon: "ti-save",
                    message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().settings_saved
                }, {
                    type: "success",
                    delay: 900,
                    placement: {
                        from: 'top',
                        align: 'right'
                    }
                });
            }
            else {
                $.notify({
                    icon: "ti-alert",
                    message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().settings_not_saved
                }, {
                    type: 'warning',
                    delay: 1900,
                    placement: {
                        from: 'top',
                        align: 'right'
                    }
                });
            }
        });
    };
    AppPageComponent.prototype.newLiveStream = function () {
        this.shareEndpoint = [];
        this.newLiveStreamActive = true;
        this.newIPCameraActive = false;
        this.newStreamSourceActive = false;
    };
    AppPageComponent.prototype.newIPCamera = function () {
        this.newLiveStreamActive = false;
        this.newIPCameraActive = true;
        this.newStreamSourceActive = false;
    };
    AppPageComponent.prototype.newStreamSource = function () {
        this.newLiveStreamActive = false;
        this.newIPCameraActive = false;
        this.newStreamSourceActive = true;
    };
    AppPageComponent.prototype.addIPCamera = function (isValid) {
        var _this = this;
        if (!isValid) {
            //not valid form return directly
            return;
        }
        this.newIPCameraAdding = true;
        this.liveBroadcast.type = "ipCamera";
        this.restService.addStreamSource(this.appName, this.liveBroadcast)
            .subscribe(function (data) {
            //console.log("data :" + JSON.stringify(data));
            if (data["success"] == true) {
                console.log("success" + data["success"]);
                console.log("error" + data["message"]);
                _this.newIPCameraAdding = false;
                $.notify({
                    icon: "ti-save",
                    message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().new_broadcast_created
                }, {
                    type: "success",
                    delay: 1000,
                    placement: {
                        from: 'top',
                        align: 'right'
                    }
                });
                _this.getAppLiveStreams(_this.streamListOffset, _this.pageSize);
                _this.getAppLiveStreamsNumber();
            }
            else {
                console.log("success" + data["success"]);
                console.log("error" + data["message"]);
                _this.newIPCameraAdding = false;
                if (data["message"].includes("401")) {
                    swal({
                        title: "Authorization Error",
                        text: "Please Check Username and/or Password",
                        type: 'error',
                        confirmButtonColor: '#3085d6',
                        confirmButtonText: 'OK'
                    }).then(function () {
                    }).catch(function () {
                    });
                }
                $.notify({
                    icon: "ti-save",
                    message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().new_broadcast_error
                }, {
                    type: "warning",
                    delay: 2000,
                    placement: {
                        from: 'top',
                        align: 'right'
                    }
                });
                _this.getAppLiveStreams(_this.streamListOffset, _this.pageSize);
                _this.getAppLiveStreamsNumber();
            }
            //swal.close();
            _this.newIPCameraAdding = false;
            _this.newIPCameraActive = false;
            _this.liveBroadcast.name = "";
            _this.liveBroadcast.ipAddr = "";
            _this.liveBroadcast.username = "";
            _this.liveBroadcast.password = "";
            if (_this.isGridView) {
                setTimeout(function () {
                    _this.switchToGridView();
                }, 500);
            }
        });
    };
    AppPageComponent.prototype.addStreamSource = function (isValid) {
        var _this = this;
        if (!isValid) {
            //not valid form return directly
            return;
        }
        this.newStreamSourceAdding = true;
        this.liveBroadcast.type = "streamSource";
        this.restService.addStreamSource(this.appName, this.liveBroadcast)
            .subscribe(function (data) {
            //console.log("data :" + JSON.stringify(data));
            if (data["success"] == true) {
                _this.newStreamSourceAdding = false;
                $.notify({
                    icon: "ti-save",
                    message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().new_broadcast_created
                }, {
                    type: "success",
                    delay: 1000,
                    placement: {
                        from: 'top',
                        align: 'right'
                    }
                });
                _this.getAppLiveStreams(_this.streamListOffset, _this.pageSize);
                _this.getAppLiveStreamsNumber();
            }
            else {
                _this.newIPCameraAdding = false;
                $.notify({
                    icon: "ti-save",
                    message: "Error: Not added"
                }, {
                    type: "error",
                    delay: 2000,
                    placement: {
                        from: 'top',
                        align: 'right'
                    }
                });
                _this.getAppLiveStreams(_this.streamListOffset, _this.pageSize);
                _this.getAppLiveStreamsNumber();
            }
            //swal.close();
            _this.newStreamSourceAdding = false;
            _this.newStreamSourceActive = false;
            _this.liveBroadcast.name = "";
            _this.liveBroadcast.ipAddr = "";
            _this.liveBroadcast.username = "";
            _this.liveBroadcast.password = "";
            if (_this.isGridView) {
                setTimeout(function () {
                    _this.switchToGridView();
                }, 500);
            }
        });
    };
    AppPageComponent.prototype.startDiscover = function () {
        var _this = this;
        this.discoveryStarted = true;
        this.onvifURLs = this.getDiscoveryList();
        this.noCamWarning = false;
        setTimeout(function () {
            if (_this.onvifURLs) {
                for (var i = 0; i < _this.broadcastTableData.dataRows.length; i++) {
                    for (var j = 0; j < _this.onvifURLs.length; j++) {
                        if (_this.broadcastTableData.dataRows[i].type == "ipCamera") {
                            if (_this.onvifURLs[j] == _this.broadcastTableData.dataRows[i].ipAddr) {
                                console.log("found:  " + _this.onvifURLs[j]);
                                // if camera is already registered then remove it from aray
                                var x = _this.onvifURLs.indexOf(_this.onvifURLs[j]);
                                _this.onvifURLs.splice(x, 1);
                            }
                        }
                    }
                }
            }
            if (_this.onvifURLs) {
                //if all cameras are added, onvif array may still be alive, then length control should be done
                if (_this.onvifURLs.length > 0) {
                    console.log(_this.onvifURLs[0]);
                    console.log(_this.onvifURLs.length);
                    _this.discoveryStarted = false;
                    swal({
                        type: 'info',
                        title: "Onvif Camera(s) ",
                        input: 'radio',
                        inputOptions: _this.onvifURLs,
                        width: '355px',
                        inputValidator: function (value) {
                            return new Promise(function (resolve, reject) {
                                if (value !== '') {
                                    resolve();
                                }
                                else {
                                    reject('Select Camera');
                                }
                            });
                        },
                    }).then(function (result) {
                        if (result) {
                            _this.liveBroadcast.ipAddr = _this.onvifURLs[result].toString();
                        }
                    });
                }
                else {
                    _this.discoveryStarted = false;
                    _this.noCamWarning = true;
                    _this.camera.ipAddr = "";
                }
            }
            else {
                _this.discoveryStarted = false;
                _this.noCamWarning = true;
                _this.camera.ipAddr = "";
            }
        }, 6000);
    };
    AppPageComponent.prototype.getDiscoveryList = function () {
        var _this = this;
        this.onvifURLs = null;
        this.restService.autoDiscover(this.appName).subscribe(function (streams) {
            if (streams.length != 0) {
                _this.onvifURLs = streams;
                console.log('result: ' + _this.onvifURLs[0]);
            }
        }, function (error) {
            console.log('!!!Error!!! ' + error);
        });
        return this.onvifURLs;
    };
    AppPageComponent.prototype.toConsole = function (val) {
        console.log(val);
    };
    AppPageComponent.prototype.createLiveStream = function (isValid) {
        var _this = this;
        if (!isValid) {
            //not valid form return directly
            return;
        }
        this.liveBroadcast.type = "liveStream";
        var socialNetworks = [];
        this.shareEndpoint.forEach(function (value, index) {
            if (value === true) {
                socialNetworks.push(_this.videoServiceEndpoints[index].id);
            }
        });
        this.newLiveStreamCreating = true;
        this.restService.createLiveStream(this.appName, this.liveBroadcast, socialNetworks.join(","))
            .subscribe(function (data) {
            //console.log("data :" + JSON.stringify(data));
            if (data["streamId"] != null) {
                _this.newLiveStreamActive = false;
                $.notify({
                    icon: "ti-save",
                    message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().new_broadcast_created
                }, {
                    type: "success",
                    delay: 900,
                    placement: {
                        from: 'top',
                        align: 'right'
                    }
                });
                _this.getAppLiveStreams(_this.streamListOffset, _this.pageSize);
                _this.liveBroadcast.name = "";
            }
            _this.newLiveStreamCreating = false;
            _this.getAppLiveStreamsNumber();
            if (_this.isGridView) {
                setTimeout(function () {
                    _this.switchToGridView();
                }, 500);
            }
        });
    };
    AppPageComponent.prototype.switchToListView = function () {
        this.isGridView = false;
        this.getAppLiveStreams(0, 5);
        var container = document.getElementById('cbp-vm'), optionSwitch = Array.prototype.slice.call(container.querySelectorAll('div.cbp-vm-options > a'));
        optionSwitch.forEach(function (el, i) {
            el.addEventListener('click', function () {
                change(this);
            });
        });
        function change(opt) {
            // remove other view classes and any selected option
            optionSwitch.forEach(function (el) {
                classie.remove(container, el.getAttribute('data-view'));
                classie.remove(el, 'cbp-vm-selected');
            });
            // add the view class for this option
            classie.add(container, opt.getAttribute('data-view'));
            // this option stays selected
            classie.add(opt, 'cbp-vm-selected');
        }
        // this.closeGridPlayers();
    };
    AppPageComponent.prototype.switchToGridView = function () {
        var _this = this;
        this.isGridView = true;
        setTimeout(function () {
            _this.openGridPlayers(0, 4);
        }, 500);
    };
    AppPageComponent.prototype.getSocialMediaAuthParameters = function (networkName) {
        var _this = this;
        this.gettingDeviceParameters = true;
        this.restService.getDeviceAuthParameters(this.appName, networkName).subscribe(function (data) {
            if (data['verification_url']) {
                if (!data['verification_url'].startsWith("http")) {
                    data['verification_url'] = "http://" + data['verification_url'];
                }
                var message = __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().copy_this_code_and_enter_the_url.replace("CODE_KEY", data['user_code']);
                message = message.replace("URL_KEY", data['verification_url']); //this is for url
                message = message.replace("URL_KEY", data['verification_url']); //this is for string
                var typem = 'info';
                _this.gettingDeviceParameters = false;
                swal({
                    html: message,
                    type: typem,
                    // showConfirmButton: false,
                    showCancelButton: true,
                    // width: '800px',
                    onOpen: function () {
                        console.log("onopen");
                    },
                    onClose: function () {
                        console.log("onclose");
                    }
                }).then(function () {
                    _this.waitingForConfirmation = true;
                    _this.checkAuthStatus(data['user_code'], networkName);
                });
            }
            else if (_this.isEnterpriseEdition == false
                && data['errorId'] == ERROR_SOCIAL_ENDPOINT_UNDEFINED_ENDPOINT) {
                message = __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().notEnterprise;
                typem = 'error';
                _this.gettingDeviceParameters = false;
                swal({
                    html: message,
                    type: typem,
                    // showConfirmButton: false,
                    showCancelButton: false,
                    // width: '800px',
                    onOpen: function () {
                        console.log("onopen");
                    },
                    onClose: function () {
                        console.log("onclose");
                    }
                });
            }
            else if (_this.isEnterpriseEdition == true && data['errorId'] == ERROR_SOCIAL_ENDPOINT_UNDEFINED_CLIENT_ID) {
                message = __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().ketNotdefined;
                ;
                typem = 'error';
                _this.gettingDeviceParameters = false;
                swal({
                    html: message,
                    type: typem,
                    // showConfirmButton: false,
                    showCancelButton: false,
                    // width: '800px',
                    onOpen: function () {
                        console.log("onopen");
                    },
                    onClose: function () {
                        console.log("onclose");
                    }
                });
            }
        });
    };
    AppPageComponent.prototype.cancelNewLiveStream = function () {
        this.newLiveStreamActive = false;
    };
    AppPageComponent.prototype.cancelNewIPCamera = function () {
        this.newIPCameraActive = false;
    };
    AppPageComponent.prototype.cancelStreamSource = function () {
        this.newStreamSourceActive = false;
    };
    AppPageComponent.prototype.copyPublishUrl = function (streamUrl) {
        this.clipBoardService.copyFromContent(this.getRtmpUrl(streamUrl));
        $.notify({
            message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().publish_url_copied_to_clipboard
        }, {
            type: "success",
            delay: 400,
            timer: 500,
            placement: {
                from: 'top',
                align: 'right'
            }
        });
    };
    AppPageComponent.prototype.copyLiveEmbedCode = function (streamUrl) {
        //if (this.isEnterpriseEdition) {
        //  streamUrl += "_adaptive";
        //}
        var embedCode = '<iframe width="560" height="315" src="'
            + __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["b" /* HTTP_SERVER_ROOT */] + this.appName + "/play.html?name=" + streamUrl
            + '" frameborder="0" allowfullscreen></iframe>';
        this.clipBoardService.copyFromContent(embedCode);
        $.notify({
            message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().embed_code_copied_to_clipboard
        }, {
            type: "success",
            delay: 400,
            timer: 500,
            placement: {
                from: 'top',
                align: 'right'
            }
        });
    };
    AppPageComponent.prototype.getRtmpUrl = function (streamUrl) {
        return "rtmp://" + __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["e" /* SERVER_ADDR */] + "/" + this.appName + "/" + streamUrl;
    };
    AppPageComponent.prototype.revokeSocialMediaAuth = function (endpointId) {
        var _this = this;
        this.restService.revokeSocialNetwork(this.appName, endpointId)
            .subscribe(function (data) {
            if (data["success"] == true) {
                _this.videoServiceEndpoints = _this.videoServiceEndpoints.filter(function (element) {
                    return element.id != endpointId;
                });
            }
        });
    };
    AppPageComponent.prototype.checkAuthStatus = function (userCode, networkName) {
        var _this = this;
        this.restService.checkAuthStatus(userCode, this.appName).subscribe(function (data) {
            if (data["success"] != true) {
                if (data["message"] == null) {
                    _this.checkAuthStatusTimerId = setTimeout(function () {
                        _this.checkAuthStatus(userCode, networkName);
                    }, 5000);
                }
                else {
                    _this.waitingForConfirmation = false;
                    var message = __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().error_occured;
                    if (data["message"] == LIVE_STREAMING_NOT_ENABLED) {
                        message = __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().live_streaming_not_enabled_message;
                    }
                    else if (data["message"] == AUTHENTICATION_TIMEOUT) {
                        message = __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().authentication_timeout_message;
                    }
                    swal({
                        type: "warning",
                        //title: Locale.getLocaleInterface().congrats,
                        text: message,
                    });
                }
            }
            else {
                if (_this.checkAuthStatusTimerId) {
                    clearInterval(_this.checkAuthStatusTimerId);
                }
                _this.getSocialEndpoints();
                _this.waitingForConfirmation = false;
                if (networkName == "facebook") {
                    _this.showNetworkChannelList(data["dataId"], "all");
                }
                else {
                    swal({
                        type: "success",
                        title: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().congrats,
                        text: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().authentication_is_done,
                    });
                }
            }
        });
    };
    AppPageComponent.prototype.filterVod = function () {
        var _this = this;
        this.searchWarning = false;
        if ($("#start").val()) {
            this.requestedStartDate = this.convertStartUnixTime($("#start").val());
        }
        else {
            this.requestedStartDate = 0;
        }
        if ($("#end").val()) {
            this.requestedEndDate = this.convertEndUnixTime($("#end").val());
        }
        else {
            this.requestedEndDate = 9999999999999;
        }
        this.searchParam.keyword = this.keyword;
        this.searchParam.endDate = this.requestedEndDate;
        this.searchParam.startDate = this.requestedStartDate;
        if (this.searchParam.endDate > this.searchParam.startDate) {
            console.log("");
            this.restService.filterVod(this.appName, 0, 10, this.searchParam).subscribe(function (data) {
                _this.vodTableData.dataRows = [];
                for (var i in data) {
                    _this.vodTableData.dataRows.push(data[i]);
                }
                console.log("filtered vod:  " + _this.vodTableData.dataRows.length.toString());
                _this.dataSourceVod = new __WEBPACK_IMPORTED_MODULE_7__angular_material__["I" /* MatTableDataSource */](_this.vodTableData.dataRows);
            });
        }
        else if (this.searchParam.endDate < this.searchParam.startDate) {
            this.searchWarning = true;
        }
        console.log("search param start:  " + this.searchParam.startDate);
        console.log("search param end:  " + this.searchParam.endDate);
        console.log("search param keyword:  " + this.searchParam.keyword);
        console.log("req start: " + this.requestedStartDate);
        console.log("req end: " + this.requestedEndDate);
        console.log("req keyword: " + this.keyword);
        if (!$("#keyword").val() || $("#keyword").val() == " ") {
            this.keyword = null;
        }
    };
    AppPageComponent.prototype.convertStartUnixTime = function (date) {
        var d = date + 'T00:00:00.000Z';
        var convertedTime = new Date(d).valueOf();
        console.log(new Date(d).valueOf());
        return convertedTime;
    };
    AppPageComponent.prototype.convertEndUnixTime = function (date) {
        var d = date + 'T23:59:59.000Z';
        var convertedTime = new Date(d).valueOf();
        console.log(new Date(d).valueOf());
        return convertedTime;
    };
    AppPageComponent.prototype.convertJavaTime = function (unixtimestamp) {
        // Months array
        var months_arr = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        // Convert timestamp to milliseconds
        var date = new Date(unixtimestamp);
        // Year
        var year = date.getFullYear();
        // Month
        var month = months_arr[date.getMonth()];
        // Day
        var day = date.getDate();
        // Hours
        var hours = date.getHours();
        // Minutes
        var minutes = "0" + date.getMinutes();
        // Seconds
        var seconds = "0" + date.getSeconds();
        // Display date time in MM-dd-yyyy h:m:s format
        var convdataTime = month + '-' + day + '-' + year + ' ' + hours + ':' + minutes.substr(-2) + ':' + seconds.substr(-2);
        return convdataTime;
    };
    AppPageComponent.prototype.moveDown = function (camera) {
        this.restService.moveDown(camera, this.appName).subscribe(function (result) {
            console.log('result!!!: ' + result);
        }, function (error) {
            console.log('!!!Error!!! ' + error);
        });
    };
    AppPageComponent.prototype.moveUp = function (camera) {
        this.restService.moveUp(camera, this.appName).subscribe(function (result) {
            console.log('result!!!: ' + result);
        }, function (error) {
            console.log('!!!Error!!! ' + error);
        });
    };
    AppPageComponent.prototype.moveRight = function (camera) {
        this.restService.moveRight(camera, this.appName).subscribe(function (result) {
            console.log('result!!!: ' + result);
        }, function (error) {
            console.log('!!!Error!!! ' + error);
        });
    };
    AppPageComponent.prototype.moveLeft = function (camera) {
        this.restService.moveLeft(camera, this.appName).subscribe(function (result) {
            console.log('result!!!: ' + result);
        }, function (error) {
            console.log('!!!Error!!! ' + error);
        });
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["G" /* Input */])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_7__angular_material__["N" /* PageEvent */])
    ], AppPageComponent.prototype, "pageEvent", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["S" /* Output */])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["y" /* EventEmitter */])
    ], AppPageComponent.prototype, "pageChange", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_12" /* ViewChild */])(__WEBPACK_IMPORTED_MODULE_7__angular_material__["F" /* MatSort */]),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_7__angular_material__["F" /* MatSort */])
    ], AppPageComponent.prototype, "sort", void 0);
    AppPageComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            selector: 'manage-app-cmp',
            moduleId: module.i,
            template: __webpack_require__("../../../../../src/app/app.page/app.page.component.html"),
            styles: [__webpack_require__("../../../../../src/app/app.page/app.page.component.css")],
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_2__angular_common_http__["b" /* HttpClient */], __WEBPACK_IMPORTED_MODULE_3__angular_router__["a" /* ActivatedRoute */],
            __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["d" /* RestService */],
            __WEBPACK_IMPORTED_MODULE_5_ngx_clipboard__["b" /* ClipboardService */],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["Y" /* Renderer */],
            __WEBPACK_IMPORTED_MODULE_3__angular_router__["c" /* Router */],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["Q" /* NgZone */],
            __WEBPACK_IMPORTED_MODULE_7__angular_material__["i" /* MatDialog */],
            __WEBPACK_IMPORTED_MODULE_1__angular_platform_browser__["c" /* DomSanitizer */],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["l" /* ChangeDetectorRef */],
            __WEBPACK_IMPORTED_MODULE_7__angular_material__["u" /* MatPaginatorIntl */]])
    ], AppPageComponent);
    return AppPageComponent;
}());

/** Builds and returns a new User. */
function createNewUser(id) {
    var name = NAMES[Math.round(Math.random() * (NAMES.length - 1))] + ' ' +
        NAMES[Math.round(Math.random() * (NAMES.length - 1))].charAt(0) + '.';
    return {
        id: id.toString(),
        name: name,
        progress: Math.round(Math.random() * 100).toString(),
        color: COLORS[Math.round(Math.random() * (COLORS.length - 1))]
    };
}
/** Constants used to fill up our data base. */
var COLORS = ['maroon', 'red', 'orange', 'yellow', 'olive', 'green', 'purple',
    'fuchsia', 'lime', 'teal', 'aqua', 'blue', 'navy', 'black', 'gray'];
var NAMES = ['Maia', 'Asher', 'Olivia', 'Atticus', 'Amelia', 'Jack',
    'Charlotte', 'Theodore', 'Isla', 'Oliver', 'Isabella', 'Jasper',
    'Cora', 'Levi', 'Violet', 'Arthur', 'Mia', 'Thomas', 'Elizabeth'];
var CamSettinsDialogComponent = /** @class */ (function () {
    function CamSettinsDialogComponent(dialogRef, restService, data) {
        this.dialogRef = dialogRef;
        this.restService = restService;
        this.data = data;
        this.loadingSettings = false;
    }
    CamSettinsDialogComponent.prototype.onNoClick = function () {
        this.dialogRef.close();
    };
    CamSettinsDialogComponent.prototype.cancelEditLiveStream = function () {
        this.dialogRef.close();
    };
    CamSettinsDialogComponent.prototype.editCamSettings = function (isValid) {
        var _this = this;
        if (!isValid) {
            return;
        }
        this.loadingSettings = true;
        console.log(this.dialogRef.componentInstance.data.status + this.dialogRef.componentInstance.data.id + this.dialogRef.componentInstance.data.name + this.dialogRef.componentInstance.data.url + this.dialogRef.componentInstance.data.username);
        this.camera = new __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["c" /* LiveBroadcast */]();
        this.camera.name = this.dialogRef.componentInstance.data.name;
        this.camera.ipAddr = this.dialogRef.componentInstance.data.url;
        this.camera.username = this.dialogRef.componentInstance.data.username;
        this.camera.password = this.dialogRef.componentInstance.data.pass;
        this.camera.streamId = this.dialogRef.componentInstance.data.id;
        this.camera.status = this.dialogRef.componentInstance.data.status;
        this.restService.editCameraInfo(this.camera, this.dialogRef.componentInstance.data.appName).subscribe(function (data) {
            if (data["success"]) {
                _this.dialogRef.close();
                $.notify({
                    icon: "ti-save",
                    message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().broadcast_updated
                }, {
                    type: "success",
                    delay: 900,
                    placement: {
                        from: 'top',
                        align: 'right'
                    }
                });
            }
            else {
                $.notify({
                    icon: "ti-alert",
                    message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().broadcast_not_updated + " " + data["message"] + " " + data["errorId"]
                }, {
                    type: "warning",
                    delay: 900,
                    placement: {
                        from: 'top',
                        align: 'right'
                    }
                });
            }
        });
    };
    CamSettinsDialogComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            selector: 'dialog-overview-example-dialog',
            template: __webpack_require__("../../../../../src/app/app.page/cam-settings-dialog.html"),
        }),
        __param(2, Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["C" /* Inject */])(__WEBPACK_IMPORTED_MODULE_7__angular_material__["a" /* MAT_DIALOG_DATA */])),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_7__angular_material__["k" /* MatDialogRef */], __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["d" /* RestService */], Object])
    ], CamSettinsDialogComponent);
    return CamSettinsDialogComponent;
}());

var UploadVodDialogComponent = /** @class */ (function () {
    function UploadVodDialogComponent(dialogRef, restService, data) {
        this.dialogRef = dialogRef;
        this.restService = restService;
        this.data = data;
        this.uploading = false;
        this.fileToUpload = null;
        this.fileselected = false;
    }
    UploadVodDialogComponent.prototype.onNoClick = function () {
        this.dialogRef.close();
    };
    UploadVodDialogComponent.prototype.handleFileInput = function (files) {
        this.fileToUpload = files.item(0);
        this.fileselected = true;
        this.fileName = this.fileToUpload.name.replace(/\s/g, '_');
        console.log(this.fileName);
    };
    UploadVodDialogComponent.prototype.submitUpload = function () {
        var _this = this;
        if (this.fileToUpload) {
            this.uploading = true;
            var formData = new FormData();
            formData.append('file', this.fileToUpload);
            console.log("file upload" + this.fileToUpload.name);
            if (!this.fileName || this.fileName.length == 0) {
                this.fileName = this.fileToUpload.name.substring(0, this.fileToUpload.name.lastIndexOf("."));
                ;
            }
            this.fileName = this.fileName.replace(/\s/g, '_');
            this.restService.uploadVod(this.fileName, formData, this.dialogRef.componentInstance.data.appName).subscribe(function (data) {
                if (data["success"] == true) {
                    _this.uploading = false;
                    _this.dialogRef.close();
                    swal({
                        type: "success",
                        title: " File is successfully uploaded!",
                        buttonsStyling: false,
                        confirmButtonClass: "btn btn-success"
                    });
                }
                else if (data["message"] == "notMp4File") {
                    _this.uploading = false;
                    swal({
                        type: "error",
                        title: "Only Mp4 files are accepted!",
                        buttonsStyling: false,
                        confirmButtonClass: "btn btn-error"
                    });
                }
                else {
                    _this.uploading = false;
                    _this.dialogRef.close();
                    swal({
                        type: "error",
                        title: "An Error Occured!",
                        buttonsStyling: false,
                        confirmButtonClass: "btn btn-error"
                    });
                }
            });
        }
    };
    UploadVodDialogComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            selector: 'upload-vod-dialog',
            template: __webpack_require__("../../../../../src/app/app.page/upload-vod-dialog.html"),
        }),
        __param(2, Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["C" /* Inject */])(__WEBPACK_IMPORTED_MODULE_7__angular_material__["a" /* MAT_DIALOG_DATA */])),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_7__angular_material__["k" /* MatDialogRef */], __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["d" /* RestService */], Object])
    ], UploadVodDialogComponent);
    return UploadVodDialogComponent;
}());

var BroadcastEditComponent = /** @class */ (function () {
    function BroadcastEditComponent(dialogRef, restService, data) {
        var _this = this;
        this.dialogRef = dialogRef;
        this.restService = restService;
        this.data = data;
        this.loading = false;
        this.liveStreamUpdating = false;
        this.shareEndpoint = [];
        this.videoServiceEndPoints = data.videoServiceEndpoints;
        var endpointList = data.endpointList;
        this.videoServiceEndPoints.forEach(function (item, index) {
            var foundService = false;
            for (var i in endpointList) {
                if (endpointList[i].endpointServiceId == item.id) {
                    _this.shareEndpoint.push(true);
                    foundService = true;
                    break;
                }
            }
            if (foundService == false) {
                _this.shareEndpoint.push(false);
            }
        });
    }
    BroadcastEditComponent.prototype.onNoClick = function () {
        this.dialogRef.close();
    };
    BroadcastEditComponent.prototype.updateLiveStream = function (isValid) {
        var _this = this;
        if (!isValid) {
            return;
        }
        this.liveStreamEditing = new __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["c" /* LiveBroadcast */]();
        this.liveStreamEditing.name = this.dialogRef.componentInstance.data.name;
        this.liveStreamEditing.streamId = this.dialogRef.componentInstance.data.streamId;
        this.liveStreamUpdating = true;
        var socialNetworks = [];
        this.shareEndpoint.forEach(function (value, index) {
            if (value === true) {
                socialNetworks.push(_this.videoServiceEndPoints[index].id);
            }
        });
        this.restService.updateLiveStream(this.dialogRef.componentInstance.data.appName, this.liveStreamEditing, socialNetworks).subscribe(function (data) {
            _this.liveStreamUpdating = false;
            console.log(data["success"]);
            if (data["success"]) {
                _this.dialogRef.close();
                $.notify({
                    icon: "ti-save",
                    message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().broadcast_updated
                }, {
                    type: "success",
                    delay: 900,
                    placement: {
                        from: 'top',
                        align: 'right'
                    }
                });
            }
            else {
                $.notify({
                    icon: "ti-alert",
                    message: __WEBPACK_IMPORTED_MODULE_6__locale_locale__["a" /* Locale */].getLocaleInterface().broadcast_not_updated + " " + data["message"] + " " + data["errorId"]
                }, {
                    type: "warning",
                    delay: 900,
                    placement: {
                        from: 'top',
                        align: 'right'
                    }
                });
            }
        });
    };
    BroadcastEditComponent.prototype.cancelEditLiveStream = function () {
        this.dialogRef.close();
    };
    BroadcastEditComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            selector: 'broadcast-edit-dialog',
            template: __webpack_require__("../../../../../src/app/app.page/broadcast-edit-dialog.html"),
        }),
        __param(2, Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["C" /* Inject */])(__WEBPACK_IMPORTED_MODULE_7__angular_material__["a" /* MAT_DIALOG_DATA */])),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_7__angular_material__["k" /* MatDialogRef */], __WEBPACK_IMPORTED_MODULE_4__rest_rest_service__["d" /* RestService */], Object])
    ], BroadcastEditComponent);
    return BroadcastEditComponent;
}());



/***/ }),

/***/ "../../../../../src/app/app.page/broadcast-edit-dialog.html":
/***/ (function(module, exports) {

module.exports = "\n\n<div >\n    <td colspan=\"4\" style=\"border-top:0px\">\n        <form method=\"post\" #f=\"ngForm\" validate (ngSubmit)=\"updateLiveStream(f.valid)\">\n\n            <div class=\"card-content\" style=\"padding-top:0px\">\n                <h4 class=\"card-title text-left\" i18n=\"@@editLiveStreamCardTitle\">\n                    Edit Stream\n                </h4>\n                <div class=\"form-group text-left\">\n                    <label i18n=\"@@newLiveStreamName\">Name</label>\n                    <input type=\"text\" required minlength=\"4\" name=\"broadcastName\" i18n-placeholder=\"@@stream_name_place_holder\" placeholder=\"Stream name\"\n                           class=\"form-control\" [(ngModel)]=\"data.name\" #broadcastName=\"ngModel\">\n                    <small [hidden]=\"broadcastName.valid || (!f.submitted)\" class=\"text-danger\"\n                           i18n=\"@@broadcastNameIsRequired\">\n                        Stream name should be at least 4 characters.\n                    </small>\n                </div>\n                <div class=\"form-group text-left\" *ngIf=\"data.videoServiceEndpoints.length>0\">\n                    <label class=\"col-sm-12\" style=\"padding-left:0px\" i18n=\"@@newLiveStreamSocialShare\">Share</label>\n\n                    <ng-container *ngFor=\"let endpoint of data.videoServiceEndpoints; let i = index\">\n\n                        <div class=\"col-sm-4 text-left checkbox vcenter\" style=\"margin-top:5px\">\n\n                            <input [id]=\"endpoint.id\" [name]=\"endpoint.id\" type=\"checkbox\"\n                                   [(ngModel)]=\"shareEndpoint[i]\">\n                            <label [for]=\"endpoint.id\">\n                                <ng-container [ngSwitch]=\"endpoint.serviceName\">\n                                    <ng-container *ngSwitchCase=\"'facebook'\">\n                                        <i class=\"ti-facebook\" style=\"color:#3b5998\">&nbsp;</i>\n                                    </ng-container>\n                                    <ng-container *ngSwitchCase=\"'youtube'\">\n                                        <i class=\"ti-youtube\" style=\"color:#e52d27\">&nbsp;</i>\n                                    </ng-container>\n                                    <ng-container *ngSwitchCase=\"'periscope'\">\n                                        <i class=\"ti-twitter-alt\" style=\"color:#55acee\">&nbsp;</i>\n                                    </ng-container>\n                                </ng-container>\n                                {{endpoint.accountName}}\n                            </label>\n\n                        </div>\n                    </ng-container>\n\n                </div>\n\n                <div class=\"form-group text-center\">\n\n                    <button type=\"submit\" [disabled]='liveStreamUpdating' class=\"btn btn-fill btn-success\" i18n=\"@@newLiveStreamSaveButton\">\n                        <i class=\"fa fa-spinner fa-pulse fa-1x fa-fw\" *ngIf=\"liveStreamUpdating\" aria-hidden=\"true\"></i>Save</button>\n\n                    <button type=\"button\" class=\"btn btn-simple\" i18n=\"@@newLiveStreamCancelButton\" (click)=\"cancelEditLiveStream()\">Cancel</button>\n                </div>\n\n            </div>\n\n        </form>\n    </td>\n</div>\n\n"

/***/ }),

/***/ "../../../../../src/app/app.page/cam-settings-dialog.html":
/***/ (function(module, exports) {

module.exports = "\n<div >\n    <td colspan=\"4\" style=\"border-top:0px\">\n        <form method=\"post\" #f=\"ngForm\" validate (ngSubmit)=\"editCamSettings(f.valid)\">\n\n            <div class=\"card-content\" style=\"padding-top:0px\">\n                <h4 class=\"card-title text-left\" i18n=\"@@editLiveStreamCardTitle\">\n                    Edit Camera Settings\n                </h4>\n                <div class=\"form-group text-left\">\n                    <label i18n=\"@@newCamName\">Name</label>\n                    <input type=\"text\" required minlength=\"4\" name=\"broadcastName\"\n                           i18n-placeholder=\"@@stream_name_place_holder\" placeholder=\"Stream name\"\n                           class=\"form-control\" [(ngModel)]=\"data.name\" #broadcastName=\"ngModel\">\n                    <small [hidden]=\"broadcastName.valid || (!f.submitted)\" class=\"text-danger\"\n                           i18n=\"@@broadcastNameIsRequired\">\n                        Camera name should be at least 4 characters.\n                    </small>\n                </div>\n\n                <div class=\"form-group text-left\">\n                    <label i18n=\"@@newOnvifUrl\">Onvif Url</label>\n                    <input type=\"text\" name=\"onvifUrl\" i18n-placeholder=\"@@onvif_url_place_holder\" placeholder=\"Onvif url\"\n                           class=\"form-control\" [(ngModel)]=\"data.url\" #onvifUrl=\"ngModel\">\n\n                </div>\n\n                <div class=\"form-group text-left\">\n                    <label i18n=\"@@newUserName\">Username</label>\n                    <input type=\"text\" name=\"username\" i18n-placeholder=\"@@username_place_holder\" placeholder=\"Username\"\n                           class=\"form-control\" [(ngModel)]=\"data.username\" #username=\"ngModel\">\n\n                </div>\n\n                <div class=\"form-group text-left\">\n                    <label i18n=\"@@newPassword\">Password</label>\n                    <input type=\"text\" name=\"password\" i18n-placeholder=\"@@password_place_holder\" placeholder=\"Password\"\n                           class=\"form-control\" [(ngModel)]=\"data.pass\" #password=\"ngModel\">\n\n                </div>\n\n\n                <div>\n\n                    <button type=\"submit\" class=\"btn btn-fill btn-success\" i18n=\"@@newLiveStreamSaveButton\">\n                        <i class=\"fa fa-spinner fa-pulse fa-1x fa-fw\" *ngIf=\"loadingSettings\" aria-hidden=\"true\"></i>Save\n                    </button>\n\n                    <button type=\"button\" class=\"btn btn-simple\" i18n=\"@@newLiveStreamCancelButton\" (click)=\"cancelEditLiveStream()\">Cancel</button>\n                </div>\n\n            </div>\n\n        </form>\n    </td>\n</div>\n\n"

/***/ }),

/***/ "../../../../../src/app/app.page/dialog/detected.objects.list.html":
/***/ (function(module, exports) {

module.exports = "<h2 mat-dialog-title>Object Detection List</h2>\n<mat-table [dataSource]=\"dataSource\" matSort>\n\n\n    <ng-container matColumnDef=\"image\">\n        <mat-header-cell *matHeaderCellDef> Image </mat-header-cell>\n\n        <mat-cell *matCellDef=\"let row\" style=\"padding:10px\">\n\n            <img style=\"width: 100%;\" [src]=\"getImageURL(row.imageId)\">\n            {{row.objectName}} - {{row.detectionTime | date:'medium'}}\n\n        </mat-cell>\n    </ng-container>\n\n    <ng-container matColumnDef=\"time\">\n            <mat-header-cell *matHeaderCellDef > Time </mat-header-cell>\n    \n            <mat-cell *matCellDef=\"let row\" style=\"padding:10px\">\n    \n                {{row.detectionTime}}\n    \n            </mat-cell>\n        </ng-container>\n\n\n    <mat-header-row *matHeaderRowDef=\"displayedColumnsStreams\"></mat-header-row>\n    <mat-row *matRowDef=\"let row; columns: displayedColumnsStreams;\">\n    </mat-row>\n</mat-table>\n<!--\n<mat-paginator #paginator [pageSize]=\"100\" [pageSizeOptions]=\"[5, 10, 20]\" [showFirstLastButtons]=\"true\">\n</mat-paginator>\n-->\n<mat-dialog-actions>\n    <button mat-button mat-dialog-close>Close</button>\n</mat-dialog-actions>"

/***/ }),

/***/ "../../../../../src/app/app.page/dialog/detected.objects.list.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export DetectedObject */
/* unused harmony export DetectedObjectTable */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return DetectedObjectListDialog; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_material__ = __webpack_require__("../../../material/esm5/material.es5.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__rest_rest_service__ = __webpack_require__("../../../../../src/app/rest/rest.service.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};



var DetectedObject = /** @class */ (function () {
    function DetectedObject() {
    }
    return DetectedObject;
}());

var DetectedObjectTable = /** @class */ (function () {
    function DetectedObjectTable() {
    }
    return DetectedObjectTable;
}());

var DetectedObjectListDialog = /** @class */ (function () {
    function DetectedObjectListDialog(dialogRef, restService, data) {
        var _this = this;
        this.dialogRef = dialogRef;
        this.restService = restService;
        this.data = data;
        this.displayedColumnsStreams = ['image'];
        this.dataSource = new __WEBPACK_IMPORTED_MODULE_1__angular_material__["I" /* MatTableDataSource */]();
        this.appName = data.appName;
        this.getDetectionList(this.appName, data.streamId, 0, 100);
        this.timerId = window.setInterval(function () {
            _this.getDetectionList(_this.appName, data.streamId, 0, 100);
        }, 3000);
        this.dialogRef.afterClosed().subscribe(function (result) {
            clearInterval(_this.timerId);
        });
    }
    DetectedObjectListDialog.prototype.getDetectionList = function (appName, streamId, offset, batch) {
        var _this = this;
        this.restService.getDetectionList(appName, streamId, offset, batch).subscribe(function (data) {
            _this.dataSource = null;
            var dataRows = [];
            for (var i in data) {
                dataRows.push(data[i]);
            }
            _this.dataSource = new __WEBPACK_IMPORTED_MODULE_1__angular_material__["I" /* MatTableDataSource */](dataRows);
        });
    };
    DetectedObjectListDialog.prototype.onNoClick = function () {
        this.dialogRef.close();
    };
    DetectedObjectListDialog.prototype.getImageURL = function (imageId) {
        return __WEBPACK_IMPORTED_MODULE_2__rest_rest_service__["b" /* HTTP_SERVER_ROOT */] + this.appName + '/previews/' + imageId + '.jpeg';
    };
    DetectedObjectListDialog = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            selector: 'detected-objects-list',
            template: __webpack_require__("../../../../../src/app/app.page/dialog/detected.objects.list.html"),
        }),
        __param(2, Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["C" /* Inject */])(__WEBPACK_IMPORTED_MODULE_1__angular_material__["a" /* MAT_DIALOG_DATA */])),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__angular_material__["k" /* MatDialogRef */], __WEBPACK_IMPORTED_MODULE_2__rest_rest_service__["d" /* RestService */], Object])
    ], DetectedObjectListDialog);
    return DetectedObjectListDialog;
}());



/***/ }),

/***/ "../../../../../src/app/app.page/upload-vod-dialog.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"upload-card\">\n\n    Upload VoD <span class=\"btn-label\" style=\"float: right;font-size: 1.2em\">\n            <i class=\"ti-upload\"></i>\n        </span>\n\n    <br>\n\n    <br>\n\n\n    <div class=\"form-group text-left\" style=\"float: left\">\n        <label i18n=\"@@newVoDName\">Name</label>\n            <input type=\"text\" name=\"vodName\" i18n-placeholder=\"@@onvif_url_place_holder\" placeholder=\"VoD Name\"\n                   class=\"form-control\" [(ngModel)]=\"fileName\" #onvifUrl=\"ngModel\">\n\n        </div>\n\n    <br><br>\n\n\n    <div class=\"fileUpload btn\">\n        <span>Choose File</span>\n        <input type=\"file\" class=\"upload\" (change)=\"handleFileInput($event.target.files)\"/>\n    </div>\n\n\n    <br><br><br>\n\n        <div class=\"form-group text-center\">\n\n            <button [disabled]='!fileselected' (click)=\"submitUpload()\" class=\"btn btn-fill btn-success\"\n                    i18n=\"@@newLiveStreamSaveButton\">\n                <i class=\"fa fa-spinner fa-pulse fa-1x fa-fw\" *ngIf=\"uploading\" aria-hidden=\"true\"></i>Save\n            </button>\n\n            <button type=\"button\" class=\"btn btn-simple\" i18n=\"@@newLiveStreamCancelButton\" (click)=\"onNoClick()\">\n                Cancel\n            </button>\n        </div>\n\n\n</div>"

/***/ }),

/***/ "../../../../../src/app/app.routing.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AppRoutes; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__layouts_admin_admin_layout_component__ = __webpack_require__("../../../../../src/app/layouts/admin/admin-layout.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__layouts_auth_auth_layout_component__ = __webpack_require__("../../../../../src/app/layouts/auth/auth-layout.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__rest_auth_service__ = __webpack_require__("../../../../../src/app/rest/auth.service.ts");



var AppRoutes = [{
        path: '',
        //   redirectTo: 'dashboard/overview',
        redirectTo: 'applications',
        pathMatch: 'full',
    }, {
        path: '',
        component: __WEBPACK_IMPORTED_MODULE_0__layouts_admin_admin_layout_component__["a" /* AdminLayoutComponent */],
        children: [{
                path: 'dashboard',
                loadChildren: './dashboard/dashboard.module#DashboardModule',
                canActivate: [__WEBPACK_IMPORTED_MODULE_2__rest_auth_service__["a" /* AuthService */]]
            }, {
                path: 'applications',
                loadChildren: './app.page/app.page.module#AppPageModule',
                canActivate: [__WEBPACK_IMPORTED_MODULE_2__rest_auth_service__["a" /* AuthService */]]
            }, {
                path: 'forms',
                loadChildren: './forms/forms.module#Forms',
                canActivate: [__WEBPACK_IMPORTED_MODULE_2__rest_auth_service__["a" /* AuthService */]]
            }, {
                path: 'tables',
                loadChildren: './tables/tables.module#TablesModule',
                canActivate: [__WEBPACK_IMPORTED_MODULE_2__rest_auth_service__["a" /* AuthService */]]
            }, {
                path: 'maps',
                loadChildren: './maps/maps.module#MapsModule',
                canActivate: [__WEBPACK_IMPORTED_MODULE_2__rest_auth_service__["a" /* AuthService */]]
            }, {
                path: 'charts',
                loadChildren: './charts/charts.module#ChartsModule',
                canActivate: [__WEBPACK_IMPORTED_MODULE_2__rest_auth_service__["a" /* AuthService */]]
            }, {
                path: 'calendar',
                loadChildren: './calendar/calendar.module#CalendarModule',
                canActivate: [__WEBPACK_IMPORTED_MODULE_2__rest_auth_service__["a" /* AuthService */]]
            },
            {
                path: '',
                loadChildren: './userpage/user.module#UserModule',
                canActivate: [__WEBPACK_IMPORTED_MODULE_2__rest_auth_service__["a" /* AuthService */]]
            },
            {
                path: '',
                loadChildren: './timeline/timeline.module#TimelineModule',
                canActivate: [__WEBPACK_IMPORTED_MODULE_2__rest_auth_service__["a" /* AuthService */]]
            }]
    }, {
        path: '',
        component: __WEBPACK_IMPORTED_MODULE_1__layouts_auth_auth_layout_component__["a" /* AuthLayoutComponent */],
        children: [{
                path: 'pages',
                loadChildren: './pages/pages.module#PagesModule'
            }]
    }
];


/***/ }),

/***/ "../../../../../src/app/layouts/admin/admin-layout.component.html":
/***/ (function(module, exports) {

module.exports = "\n<div class=\"wrapper\">\n    <div class=\"sidebar\" data-background-color=\"white\" data-active-color=\"danger\">\n        <sidebar-cmp></sidebar-cmp>\n    </div>\n    <div class=\"main-panel\">\n        <navbar-cmp></navbar-cmp>\n        <router-outlet></router-outlet>\n        <!-- <div *ngIf=\"!isMap()\"> -->\n            <footer-cmp></footer-cmp>\n        <!-- </div> -->\n    </div>\n\n</div>\n"

/***/ }),

/***/ "../../../../../src/app/layouts/admin/admin-layout.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AdminLayoutComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__("../../../router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__("../../../common/esm5/common.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_add_operator_filter__ = __webpack_require__("../../../../rxjs/_esm5/add/operator/filter.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__shared_navbar_navbar_component__ = __webpack_require__("../../../../../src/app/shared/navbar/navbar.component.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};





var AdminLayoutComponent = /** @class */ (function () {
    function AdminLayoutComponent(router, location) {
        this.router = router;
        this.location = location;
    }
    AdminLayoutComponent.prototype.ngOnInit = function () {
        var _this = this;
        this._router = this.router.events.filter(function (event) { return event instanceof __WEBPACK_IMPORTED_MODULE_1__angular_router__["b" /* NavigationEnd */]; }).subscribe(function (event) {
            //   this.url = event.url;
            _this.navbar.sidebarClose();
        });
        var isWindows = navigator.platform.indexOf('Win') > -1 ? true : false;
        if (isWindows) {
            // if we are on windows OS we activate the perfectScrollbar function
            var $main_panel = $('.main-panel');
            $main_panel.perfectScrollbar();
        }
    };
    AdminLayoutComponent.prototype.isMap = function () {
        // console.log(this.location.prepareExternalUrl(this.location.path()));
        if (this.location.prepareExternalUrl(this.location.path()) == '/maps/fullscreen') {
            return true;
        }
        else {
            return false;
        }
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_12" /* ViewChild */])('sidebar'),
        __metadata("design:type", Object)
    ], AdminLayoutComponent.prototype, "sidebar", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_12" /* ViewChild */])(__WEBPACK_IMPORTED_MODULE_4__shared_navbar_navbar_component__["a" /* NavbarComponent */]),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_4__shared_navbar_navbar_component__["a" /* NavbarComponent */])
    ], AdminLayoutComponent.prototype, "navbar", void 0);
    AdminLayoutComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            selector: 'app-layout',
            template: __webpack_require__("../../../../../src/app/layouts/admin/admin-layout.component.html")
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__angular_router__["c" /* Router */], __WEBPACK_IMPORTED_MODULE_2__angular_common__["f" /* Location */]])
    ], AdminLayoutComponent);
    return AdminLayoutComponent;
}());



/***/ }),

/***/ "../../../../../src/app/layouts/auth/auth-layout.component.html":
/***/ (function(module, exports) {

module.exports = "\n  <router-outlet></router-outlet>\n"

/***/ }),

/***/ "../../../../../src/app/layouts/auth/auth-layout.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AuthLayoutComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var AuthLayoutComponent = /** @class */ (function () {
    function AuthLayoutComponent() {
    }
    AuthLayoutComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            selector: 'app-layout',
            template: __webpack_require__("../../../../../src/app/layouts/auth/auth-layout.component.html")
        })
    ], AuthLayoutComponent);
    return AuthLayoutComponent;
}());



/***/ }),

/***/ "../../../../../src/app/locale/locale.en.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return Locale_English; });
var Locale_English = {
    notEnterprise: 'This is an Enterprise function.'
        + 'Please  visit <a href="https://antmedia.io/" target="_blank"><b>antmedia.io</b></a> for detailed information',
    ketNotdefined: "Please enter service client id and client secret in app configuration.",
    vodFileNotDeleted: "VoD file is not deleted",
    broadcast_not_deleted: "Broadcast is not deleted",
    settings_saved: "Settings saved",
    settings_not_saved: "Settings could not be saved.",
    new_broadcast_created: "New broadcast is created",
    new_broadcast_error: "New broadcast is not created.",
    publish_url_copied_to_clipboard: "Publish URL is copied to clipboard",
    embed_code_copied_to_clipboard: "Embed Code is copied to clipboard",
    congrats: "Congratulations",
    authentication_is_done: "Authentication is done",
    are_you_sure: 'Are you sure?',
    wont_be_able_to_revert: "You won't be able to revert this!",
    live_stream_will_be_deleted: "Live stream will be deleted!",
    copy_this_code_and_enter_the_url: 'Copy this code <b>CODE_KEY</b>'
        + ' and enter it to '
        + ' <a href="URL_KEY" target="_blank"><b>URL_KEY</b></a> address',
    dashboard: 'Dashboard',
    turkish_language: "Türkçe",
    english_language: "English",
    broadcast_updated: "Stream is updated.",
    broadcast_not_updated: "Stream is not updated. Please send below message to contact@antmedia.io",
    error_occured: "An error is occured. Please try again later",
    authentication_timeout_message: "Authentication timeout. Please try again with a faster manner",
    live_streaming_not_enabled_message: "Live streaming is not enabled in your account. Please enable it.",
    streams_imported_successfully: "Streams are imported successfully",
    missing_configuration_parameter_for_stalker: "Configuration params are missing. Please contact system admin",
};


/***/ }),

/***/ "../../../../../src/app/locale/locale.tr.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return Locale_Turkish; });
var Locale_Turkish = {
    notEnterprise: 'Bu özellik bir kurumsal özelliktir.'
        + 'Detaylı bilgi için lütfen <a href="https://antmedia.io/" target="_blank"><b>antmedia.io</b></a> sitesini ziyaret ediniz',
    ketNotdefined: "Lütfen servisle ilgili istemci id ve şifresini tanımlayınız.",
    vodFileNotDeleted: "Video maalesef silinemedi.",
    broadcast_not_deleted: "Yayın maalesef silinemedi.",
    settings_saved: "Ayarlar kaydedildi.",
    settings_not_saved: "Ayarlar maalesef kayıt edilemedi.",
    new_broadcast_created: "Yeni yayın oluşturuldu.",
    new_broadcast_error: "Yeni yayın oluşturulamadı.",
    publish_url_copied_to_clipboard: "Yayın URL'i panoya kopyalandı.",
    embed_code_copied_to_clipboard: "Gömülü kod panoya kopyalandı.",
    congrats: "Tebrikler",
    authentication_is_done: "Yetkilendirme tamamlandı.",
    are_you_sure: 'Bunu yapmak istediğinizden emin misiniz ?',
    wont_be_able_to_revert: "Bu işlemi geri alamayacaksınız",
    copy_this_code_and_enter_the_url: 'Bu kodu (<b>CODE_KEY</b>) kopyalayın'
        + ' ve bu adresteki '
        + ' (<a href="URL_KEY" target="_blank"><b>URL_KEY</b></a>) ilgili kutuya girin.',
    dashboard: 'Kontrol Paneli',
    turkish_language: "Türkçe",
    english_language: "English",
    live_stream_will_be_deleted: "Yayın silinecek ve bu işlemi geri alamayacaksınız!",
    broadcast_updated: "Yayın güncellendi.",
    broadcast_not_updated: "Yayın güncellenemedi. Lütfen aşağıdaki mesajı contact@antmedia.io'ya gönderiniz.",
    error_occured: "Bir hata oluştu. Lütfen sonra tekrar deneyiniz",
    authentication_timeout_message: "Yetkilendirme zaman aşımına uğradı. Lütfen işlemi biraz daha hızlı bir şekilde tekrar deneyiniz.",
    live_streaming_not_enabled_message: "Canlı Yayın özelliği hesabınızda etkin değil. Lütfen ilgili servis üzerinden etkinleştirin",
    streams_imported_successfully: "Yayınlar başarıyla aktarıldı",
    missing_configuration_parameter_for_stalker: "Konfigurasyon parametreleri girilmemiş. Lütfen sistem yönetici ile görüşünüz."
};


/***/ }),

/***/ "../../../../../src/app/locale/locale.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return Locale; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__locale_en__ = __webpack_require__("../../../../../src/app/locale/locale.en.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_app_locale_locale_tr__ = __webpack_require__("../../../../../src/app/locale/locale.tr.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};



var Locale = /** @class */ (function () {
    function Locale(locale) {
        if (locale == "tr") {
            Locale_1.localeObject = __WEBPACK_IMPORTED_MODULE_2_app_locale_locale_tr__["a" /* Locale_Turkish */];
        }
        else {
            Locale_1.localeObject = __WEBPACK_IMPORTED_MODULE_1__locale_en__["a" /* Locale_English */];
        }
    }
    Locale_1 = Locale;
    Locale.getLocaleInterface = function () {
        return Locale_1.localeObject;
    };
    Locale = Locale_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["D" /* Injectable */])(),
        __param(0, Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["C" /* Inject */])(__WEBPACK_IMPORTED_MODULE_0__angular_core__["J" /* LOCALE_ID */])),
        __metadata("design:paramtypes", [String])
    ], Locale);
    return Locale;
    var Locale_1;
}());



/***/ }),

/***/ "../../../../../src/app/rest/auth.service.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AuthService; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__("../../../router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__rest_service__ = __webpack_require__("../../../../../src/app/rest/rest.service.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var AuthService = /** @class */ (function () {
    function AuthService(restService, router) {
        var _this = this;
        this.restService = restService;
        this.router = router;
        this.isAuthenticated = true;
        setInterval(function () {
            _this.checkServerIsAuthenticated();
        }, 10000);
    }
    AuthService.prototype.test = function () {
        return 'working';
    };
    AuthService.prototype.login = function (email, password) {
        var user = new __WEBPACK_IMPORTED_MODULE_2__rest_service__["f" /* User */](email, password);
        return this.restService.authenticateUser(user);
    };
    AuthService.prototype.changeUserPassword = function (email, password, newPassword) {
        var user = new __WEBPACK_IMPORTED_MODULE_2__rest_service__["f" /* User */](email, password);
        user.newPassword = newPassword;
        return this.restService.changePassword(user);
    };
    AuthService.prototype.isFirstLogin = function () {
        return this.restService.isFirstLogin();
    };
    AuthService.prototype.createFirstAccount = function (user) {
        return this.restService.createFirstAccount(user);
    };
    AuthService.prototype.checkServerIsAuthenticated = function () {
        var _this = this;
        if (localStorage.getItem('authenticated')) {
            this.restService.isAuthenticated().subscribe(function (data) {
                _this.isAuthenticated = data["success"];
                console.log("data success --> " + data["success"]);
                if (!_this.isAuthenticated) {
                    _this.router.navigateByUrl('/pages/login');
                }
            }, function (error) {
                _this.isAuthenticated = false;
                _this.router.navigateByUrl('/pages/login');
            });
        }
    };
    AuthService.prototype.canActivate = function () {
        /*
        
        */
        if (localStorage.getItem('authenticated') && this.isAuthenticated) {
            return true;
        }
        else {
            this.router.navigateByUrl('/pages/login');
            return false;
        }
    };
    AuthService = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["D" /* Injectable */])(),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_2__rest_service__["d" /* RestService */], __WEBPACK_IMPORTED_MODULE_1__angular_router__["c" /* Router */]])
    ], AuthService);
    return AuthService;
}());



/***/ }),

/***/ "../../../../../src/app/rest/rest.service.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "f", function() { return User; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "e", function() { return SERVER_ADDR; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return HTTP_SERVER_ROOT; });
/* unused harmony export REST_SERVICE_ROOT */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return LiveBroadcast; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AuthInterceptor; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return RestService; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__("../../../router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_Observable__ = __webpack_require__("../../../../rxjs/_esm5/Observable.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__environments_environment__ = __webpack_require__("../../../../../src/environments/environment.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_rxjs_add_operator_map__ = __webpack_require__("../../../../rxjs/_esm5/add/operator/map.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_rxjs_add_operator_catch__ = __webpack_require__("../../../../rxjs/_esm5/add/operator/catch.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_rxjs_add_operator_toPromise__ = __webpack_require__("../../../../rxjs/_esm5/add/operator/toPromise.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_rxjs_add_operator_toPromise___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_6_rxjs_add_operator_toPromise__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__angular_common_http__ = __webpack_require__("../../../common/esm5/http.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};








var User = /** @class */ (function () {
    function User(email, password) {
        this.email = email;
        this.password = password;
    }
    return User;
}());

var SERVER_ADDR = location.hostname;
var HTTP_SERVER_ROOT;
if (__WEBPACK_IMPORTED_MODULE_3__environments_environment__["a" /* environment */].production) {
    HTTP_SERVER_ROOT = "//" + location.hostname + ":" + location.port + "/";
}
else {
    HTTP_SERVER_ROOT = "//" + location.hostname + ":5080/";
}
var REST_SERVICE_ROOT = HTTP_SERVER_ROOT + "ConsoleApp/rest";
var LiveBroadcast = /** @class */ (function () {
    function LiveBroadcast() {
    }
    return LiveBroadcast;
}());

var AuthInterceptor = /** @class */ (function () {
    function AuthInterceptor() {
    }
    AuthInterceptor.prototype.intercept = function (req, next) {
        req = req.clone({
            withCredentials: true
        });
        return next.handle(req);
    };
    AuthInterceptor = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["D" /* Injectable */])(),
        __metadata("design:paramtypes", [])
    ], AuthInterceptor);
    return AuthInterceptor;
}());

var RestService = /** @class */ (function () {
    function RestService(http, router) {
        this.http = http;
        this.router = router;
    }
    RestService.prototype.getCPULoad = function () {
        return this.http.get(REST_SERVICE_ROOT + '/getCPUInfo');
    };
    RestService.prototype.checkAuthStatus = function (networkName, appName) {
        return this.http.post(REST_SERVICE_ROOT + "/request?_path=" + appName + "/rest/broadcast/checkDeviceAuthStatus/" + networkName, {});
    };
    RestService.prototype.getDetectionList = function (appName, streamId, offset, size) {
        return this.http.get(REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/detection/getList/' + offset + "/" + size + "?id=" + streamId);
    };
    RestService.prototype.getAppLiveStreams = function (appName, offset, size) {
        return this.http.get(REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/broadcast/getList/' + offset + "/" + size);
    };
    RestService.prototype.getBroadcast = function (appName, id) {
        return this.http.get(REST_SERVICE_ROOT + "/request?_path=" + appName + "/rest/broadcast/get?id=" + id);
    };
    RestService.prototype.createLiveStream = function (appName, liveBroadcast, socialNetworks) {
        return this.http.post(REST_SERVICE_ROOT + "/request?_path=" + appName + "/rest/broadcast/createWithSocial?socialNetworks=" + socialNetworks, liveBroadcast);
    };
    RestService.prototype.updateLiveStream = function (appName, broadcast, socialNetworks) {
        return this.http.post(REST_SERVICE_ROOT + "/request?_path=" + appName + "/rest/broadcast/update?socialNetworks=" + socialNetworks, broadcast);
    };
    RestService.prototype.importLiveStreams2Stalker = function (appName) {
        return this.http.post(REST_SERVICE_ROOT + "/request?_path=" + appName + "/rest/importLiveStreamsToStalker", {});
    };
    RestService.prototype.importVoDStreams2Stalker = function (appName) {
        return this.http.post(REST_SERVICE_ROOT + "/request?_path=" + appName + "/rest/importVoDsToStalker", {});
    };
    RestService.prototype.deleteBroadcast = function (appName, streamId) {
        return this.http.post(REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/broadcast/delete/' + streamId, {});
    };
    RestService.prototype.deleteVoDFile = function (appName, vodName, id) {
        return this.http.post(REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/broadcast/deleteVoDFile/' + vodName + '/' + id, {});
    };
    RestService.prototype.revokeSocialNetwork = function (appName, serviceName) {
        return this.http.post(REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/broadcast/revokeSocialNetwork/' + serviceName, {});
    };
    RestService.prototype.isEnterpriseEdition = function () {
        return this.http.get(REST_SERVICE_ROOT + "/isEnterpriseEdition");
    };
    RestService.prototype.handleError = function (error) {
        if (error.status == 401) {
            console.log("error -response: --> " + error);
            this.router.navigateByUrl("/pages/login");
        }
        return __WEBPACK_IMPORTED_MODULE_2_rxjs_Observable__["a" /* Observable */].throw(error || 'Server error');
    };
    RestService.prototype.getApplications = function () {
        return this.http.get(REST_SERVICE_ROOT + '/getApplications');
    };
    RestService.prototype.authenticateUser = function (user) {
        return this.http.post(REST_SERVICE_ROOT + "/authenticateUser", user);
    };
    RestService.prototype.changePassword = function (user) {
        return this.http.post(REST_SERVICE_ROOT + "/changeUserPassword", user);
    };
    RestService.prototype.isFirstLogin = function () {
        return this.http.get(REST_SERVICE_ROOT + "/isFirstLogin");
    };
    RestService.prototype.createFirstAccount = function (user) {
        return this.http.post(REST_SERVICE_ROOT + "/addInitialUser", user);
    };
    RestService.prototype.isAuthenticated = function () {
        return this.http.get(REST_SERVICE_ROOT + "/isAuthenticated");
    };
    RestService.prototype.getVoDStreams = function (appName) {
        return this.http.get(REST_SERVICE_ROOT + '/getAppVoDStreams/' + appName);
    };
    RestService.prototype.get = function (url, options) {
        return this.http.get(url, options);
    };
    RestService.prototype.getSocialEndpoints = function (appName) {
        return this.http.get(REST_SERVICE_ROOT + "/request?_path=" + appName + "/rest/broadcast/getSocialEndpoints/0/20");
    };
    RestService.prototype.setSocialNetworkChannel = function (appName, serviceName, type, value) {
        return this.http.post(REST_SERVICE_ROOT + "/request?_path=" + appName + "/rest/broadcast/setSocialNetworkChannel/"
            + serviceName + "/" + type + "/" + value, {});
    };
    RestService.prototype.getSocialNetworkChannelList = function (appName, serviceName, type) {
        return this.http.get(REST_SERVICE_ROOT + "/request?_path=" + appName + "/rest/broadcast/getSocialNetworkChannelList/" + serviceName + "/" + type);
    };
    RestService.prototype.getSocialNetworkChannel = function (appName, serviceName) {
        return this.http.get(REST_SERVICE_ROOT + "/request?_path=" + appName + "/rest/broadcast/getSocialNetworkChannel/" + serviceName, {});
    };
    RestService.prototype.getSettings = function (appName) {
        return this.http.get(REST_SERVICE_ROOT + "/getSettings/" + appName);
    };
    RestService.prototype.checkDeviceAuthStatus = function (appName, serviceName) {
        return this.http.post(REST_SERVICE_ROOT + "/request?_path=" + appName + "/rest/broadcast/checkDeviceAuthStatus/" + serviceName, {});
    };
    RestService.prototype.changeSettings = function (appName, appSettings) {
        return this.http.post(REST_SERVICE_ROOT + '/changeSettings/' + appName, appSettings);
    };
    RestService.prototype.getDeviceAuthParameters = function (appName, networkName) {
        return this.http.post(REST_SERVICE_ROOT + "/request?_path=" + appName + "/rest/broadcast/getDeviceAuthParameters/" + networkName, {});
    };
    RestService.prototype.getLiveClientsSize = function () {
        return this.http.get(REST_SERVICE_ROOT + '/getLiveClientsSize');
    };
    RestService.prototype.getSystemMemoryInfo = function () {
        return this.http.get(REST_SERVICE_ROOT + '/getSystemMemoryInfo');
    };
    RestService.prototype.getFileSystemInfo = function () {
        return this.http.get(REST_SERVICE_ROOT + '/getFileSystemInfo');
    };
    RestService.prototype.getJVMMemoryInfo = function () {
        return this.http.get(REST_SERVICE_ROOT + '/getJVMMemoryInfo');
    };
    RestService.prototype.getApplicationsInfo = function () {
        return this.http.get(REST_SERVICE_ROOT + '/getApplicationsInfo');
    };
    RestService.prototype.getVodList = function (appName, offset, size) {
        return this.http.get(REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/broadcast/getVodList/' + offset + "/" + size, {});
    };
    RestService.prototype.synchUserVodList = function (appName) {
        return this.http.get(REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/streamSource/synchUserVoDList', {});
    };
    RestService.prototype.getTotalVodNumber = function (appName) {
        return this.http.get(REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/broadcast/getTotalVodNumber', {});
    };
    RestService.prototype.getTotalBroadcastNumber = function (appName) {
        return this.http.get(REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/broadcast/getTotalBroadcastNumber', {});
    };
    RestService.prototype.filterAppLiveStreams = function (appName, offset, size, type) {
        return this.http.get(REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/broadcast/filterList/' + offset + "/" + size + "/" + type, {});
    };
    RestService.prototype.filterVod = function (appName, offset, size, searchParam) {
        return this.http.post(REST_SERVICE_ROOT + "/request?_path=" + appName + "/rest/broadcast/filterVoD?offset=" + offset + "&size=" + size, searchParam);
    };
    RestService.prototype.uploadVod = function (fileName, formData, appName) {
        return this.http.post(REST_SERVICE_ROOT + "/request?_path=" + appName + "/rest/broadcast/uploadVoDFile/" + fileName, formData);
    };
    RestService.prototype.createLiveStreamSocialNetworks = function (appName, liveBroadcast, socialNetworks) {
        return this.http.post(REST_SERVICE_ROOT + "/request?_path=" + appName + "/rest/broadcast/createWithSocial?socialNetworks=" + socialNetworks, liveBroadcast);
    };
    RestService.prototype.deleteIPCamera = function (appName, streamId) {
        return this.http.get(REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/streamSource/deleteCamera?ipAddr=' + streamId, {});
    };
    RestService.prototype.addStreamSource = function (appName, stream) {
        var url = REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/streamSource/addStreamSource';
        return this.http.post(url, stream);
    };
    RestService.prototype.autoDiscover = function (appName) {
        var streamInfoUrl = REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/streamSource/searchOnvifDevices';
        console.log('URL ' + streamInfoUrl);
        return this.http.get(streamInfoUrl);
    };
    RestService.prototype.getCamList = function (appName) {
        var url = REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/streamSource/getList';
        console.log('URL ' + url);
        return this.http.get(url);
    };
    RestService.prototype.moveLeft = function (camera, appName) {
        var streamInfoUrl = REST_SERVICE_ROOT + "/request?_path=" + "/request?_path=" + appName + '/rest/streamSource/moveLeft?id=' + camera.streamId;
        console.log('URL ' + streamInfoUrl);
        return this.http.get(streamInfoUrl);
    };
    RestService.prototype.moveRight = function (camera, appName) {
        var streamInfoUrl = REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/streamSource/moveRight?id=' + camera.streamId;
        console.log('URL ' + streamInfoUrl);
        return this.http.get(streamInfoUrl);
    };
    RestService.prototype.moveUp = function (camera, appName) {
        var streamInfoUrl = REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/streamSource/moveUp?id=' + camera.streamId;
        console.log('URL ' + streamInfoUrl);
        return this.http.get(streamInfoUrl);
    };
    RestService.prototype.moveDown = function (camera, appName) {
        var streamInfoUrl = REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/streamSource/moveDown?id=' + camera.streamId;
        console.log('URL ' + streamInfoUrl);
        return this.http.get(streamInfoUrl);
    };
    RestService.prototype.editCameraInfo = function (camera, appName) {
        var streamInfoUrl = REST_SERVICE_ROOT + "/request?_path=" + appName + '/rest/streamSource/updateCamInfo';
        console.log('URL ' + streamInfoUrl);
        return this.http.post(streamInfoUrl, camera);
    };
    RestService.prototype.extractData = function (res) {
        var body = res.json();
        return body || {};
    };
    RestService = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["D" /* Injectable */])(),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_7__angular_common_http__["b" /* HttpClient */], __WEBPACK_IMPORTED_MODULE_1__angular_router__["c" /* Router */]])
    ], RestService);
    return RestService;
}());



/***/ }),

/***/ "../../../../../src/app/shared/footer/footer.component.html":
/***/ (function(module, exports) {

module.exports = "<footer class=\"footer\">\n    <div class=\"container-fluid\">\n        <nav class=\"pull-left\">\n            <ul>\n                <li>\n                    <a href=\"http://antmedia.io/blog\">\n                       Blog\n                    </a>\n                </li>\n                \n            </ul>\n        </nav>\n\n        <div class=\"copyright pull-right\">\n                &copy; {{test | date: 'yyyy'}} <i class=\"fa fa-heart heart\"></i> by <a  href=\"https://antmedia.io\">Ant Media</a>\n            </div>\n\n            <!--\n        <nav class=\"pull-right\">\n               \n                <ul>\n                <li>\n                    <a href=\"./tr\" *ngIf=\"target_language=='Türkçe'\" >\n                       {{target_language}}\n                    </a>\n                    <a href=\"/\" *ngIf=\"target_language=='English'\" >\n                        {{target_language}}\n                     </a>\n                </li>\n                \n            </ul>\n        </nav>\n    -->\n        \n    </div>\n</footer>\n"

/***/ }),

/***/ "../../../../../src/app/shared/footer/footer.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return FooterComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_app_locale_locale__ = __webpack_require__("../../../../../src/app/locale/locale.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};


var FooterComponent = /** @class */ (function () {
    function FooterComponent(locale) {
        this.test = new Date();
        if (locale == "en-US") {
            this.target_language = __WEBPACK_IMPORTED_MODULE_1_app_locale_locale__["a" /* Locale */].getLocaleInterface().turkish_language;
        }
        else if (locale == "tr") {
            this.target_language = __WEBPACK_IMPORTED_MODULE_1_app_locale_locale__["a" /* Locale */].getLocaleInterface().english_language;
        }
    }
    FooterComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'footer-cmp',
            template: __webpack_require__("../../../../../src/app/shared/footer/footer.component.html")
        }),
        __param(0, Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["C" /* Inject */])(__WEBPACK_IMPORTED_MODULE_0__angular_core__["J" /* LOCALE_ID */])),
        __metadata("design:paramtypes", [String])
    ], FooterComponent);
    return FooterComponent;
}());



/***/ }),

/***/ "../../../../../src/app/shared/footer/footer.module.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return FooterModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common__ = __webpack_require__("../../../common/esm5/common.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_router__ = __webpack_require__("../../../router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__footer_component__ = __webpack_require__("../../../../../src/app/shared/footer/footer.component.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};




var FooterModule = /** @class */ (function () {
    function FooterModule() {
    }
    FooterModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["L" /* NgModule */])({
            imports: [__WEBPACK_IMPORTED_MODULE_2__angular_router__["d" /* RouterModule */], __WEBPACK_IMPORTED_MODULE_1__angular_common__["b" /* CommonModule */]],
            declarations: [__WEBPACK_IMPORTED_MODULE_3__footer_component__["a" /* FooterComponent */]],
            exports: [__WEBPACK_IMPORTED_MODULE_3__footer_component__["a" /* FooterComponent */]]
        })
    ], FooterModule);
    return FooterModule;
}());



/***/ }),

/***/ "../../../../../src/app/shared/navbar/navbar.component.html":
/***/ (function(module, exports) {

module.exports = "<nav #navbar class=\"navbar navbar-default\">\n    <div class=\"container-fluid\">\n        <div class=\"navbar-minimize\">\n            <button id=\"minimizeSidebar\" class=\"btn btn-fill btn-icon\">\n                <i class=\"ti-more-alt\"></i>\n            </button>\n        </div>\n        <div class=\"navbar-header\">\n            <button type=\"button\" class=\"navbar-toggle\" (click)=\"sidebarToggle()\">\n                <span class=\"sr-only\" i18n=\"@@toggleNavigation\">Toggle navigation</span>\n                <span class=\"icon-bar bar1\"></span>\n                <span class=\"icon-bar bar2\"></span>\n                <span class=\"icon-bar bar3\"></span>\n            </button>\n            <a class=\"navbar-brand\">{{getTitle()}}</a>\n        </div>\n        <div class=\"collapse navbar-collapse\">\n            <div class=\"\" *ngIf=\"isMobileMenu()\">\n\n                <ul class=\"nav navbar-nav navbar-right\">\n                    <li class=\"dropdown\">\n                        <a href=\"#notifications\" class=\"dropdown-toggle btn-magnify\" data-toggle=\"dropdown\">\n                            <i class=\"ti-user\"></i>\n                            <span class=\"notification\"></span>\n                            <b class=\"caret\"></b>\n                            <p class=\"hidden-md hidden-lg\">\n\n\n                            </p>\n                        </a>\n                        <ul class=\"dropdown-menu\">\n                            <li>\n                                <a routerLink=\"/pages/changepass\" routerLinkActive=\"active\">\n                                    <i class=\"ti-angle-double-right\"></i>\n                                    <ng-container i18n=\"@@changePassword\">Change Password</ng-container>\n                                </a>\n\n                            </li>\n                            <li class=\"text-left\">\n                                <a routerLink=\"/pages/login\" routerLinkActive=\"active\">\n\n                                    <i class=\"ti-angle-double-right\"></i>\n                                    <ng-container i18n=\"@@logout\">Logout</ng-container>\n\n                                </a>\n\n                            </li>\n                            <!--\n                            <li>\n                                <a >\n                                    <button (click)=\"logout()\" class=\"btn btn-primary btn-simple btn-small\">\n                                        <i class=\"ti-angle-double-right\"></i>\n                                        Logout\n                                    </button>\n                                </a>\n\n                            </li>\n                        -->\n                        </ul>\n                    </li>\n\n                    <!--\n                    <li>\n                        <a class=\"btn-rotate\">\n                            <i class=\"ti-settings\"></i>\n                            <p class=\"hidden-md hidden-lg\">\n                                Settings\n                            </p>\n                        </a>\n                    </li>\n                -->\n                </ul>\n\n            </div>\n\n        </div>\n    </div>\n</nav>"

/***/ }),

/***/ "../../../../../src/app/shared/navbar/navbar.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return NavbarComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__sidebar_sidebar_component__ = __webpack_require__("../../../../../src/app/sidebar/sidebar.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_router__ = __webpack_require__("../../../router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_common__ = __webpack_require__("../../../common/esm5/common.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_app_locale_locale__ = __webpack_require__("../../../../../src/app/locale/locale.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};





var misc = {
    navbar_menu_visible: 0,
    active_collapse: true,
    disabled_collapse_init: 0,
};
var NavbarComponent = /** @class */ (function () {
    function NavbarComponent(location, renderer, element, router) {
        this.renderer = renderer;
        this.element = element;
        this.router = router;
        this.location = location;
        this.nativeElement = element.nativeElement;
        this.sidebarVisible = false;
    }
    NavbarComponent.prototype.ngOnInit = function () {
        this.listTitles = __WEBPACK_IMPORTED_MODULE_1__sidebar_sidebar_component__["a" /* ROUTES */].filter(function (listTitle) { return listTitle; });
        var navbar = this.element.nativeElement;
        this.toggleButton = navbar.getElementsByClassName('navbar-toggle')[0];
        if ($('body').hasClass('sidebar-mini')) {
            misc.sidebar_mini_active = true;
        }
        $('#minimizeSidebar').click(function () {
            var $btn = $(this);
            if (misc.sidebar_mini_active == true) {
                $('body').removeClass('sidebar-mini');
                misc.sidebar_mini_active = false;
            }
            else {
                setTimeout(function () {
                    $('body').addClass('sidebar-mini');
                    misc.sidebar_mini_active = true;
                }, 300);
            }
            // we simulate the window Resize so the charts will get updated in realtime.
            var simulateWindowResize = setInterval(function () {
                window.dispatchEvent(new Event('resize'));
            }, 180);
            // we stop the simulation of Window Resize after the animations are completed
            setTimeout(function () {
                clearInterval(simulateWindowResize);
            }, 1000);
        });
    };
    NavbarComponent.prototype.isMobileMenu = function () {
        if ($(window).width() < 991) {
            return false;
        }
        return true;
    };
    NavbarComponent.prototype.sidebarOpen = function () {
        var toggleButton = this.toggleButton;
        var body = document.getElementsByTagName('body')[0];
        setTimeout(function () {
            toggleButton.classList.add('toggled');
        }, 500);
        body.classList.add('nav-open');
        this.sidebarVisible = true;
    };
    NavbarComponent.prototype.sidebarClose = function () {
        var body = document.getElementsByTagName('body')[0];
        this.toggleButton.classList.remove('toggled');
        this.sidebarVisible = false;
        body.classList.remove('nav-open');
    };
    NavbarComponent.prototype.sidebarToggle = function () {
        // var toggleButton = this.toggleButton;
        // var body = document.getElementsByTagName('body')[0];
        if (this.sidebarVisible == false) {
            this.sidebarOpen();
        }
        else {
            this.sidebarClose();
        }
    };
    NavbarComponent.prototype.getTitle = function () {
        var titlee = this.location.prepareExternalUrl(this.location.path());
        if (titlee.charAt(0) === '#') {
            titlee = titlee.slice(2);
        }
        for (var item = 0; item < this.listTitles.length; item++) {
            var parent = this.listTitles[item];
            if (parent.path === titlee) {
                return parent.title;
            }
            else if (parent.children) {
                var first_child = titlee.split("/")[0];
                if (first_child == "dashboard") {
                    return __WEBPACK_IMPORTED_MODULE_4_app_locale_locale__["a" /* Locale */].getLocaleInterface().dashboard;
                }
                if (first_child == "applications") {
                    return titlee.split("/")[1];
                }
                /*
                var children_from_url = titlee.split("/")[1];
                
                for(var current = 0; current < parent.children.length; current++){
                    if(parent.children[current].path === children_from_url ){
                        return parent.children[current].title;
                    }
                }
                */
            }
        }
        return 'Dashboard';
    };
    NavbarComponent.prototype.getPath = function () {
        // console.log(this.location);
        return this.location.prepareExternalUrl(this.location.path());
    };
    NavbarComponent.prototype.logout = function () {
        console.log("log out---");
        // localStorage.setItem("authenticated", null);
        localStorage.clear();
        this.router.navigateByUrl("/pages/login");
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_12" /* ViewChild */])("navbar-cmp"),
        __metadata("design:type", Object)
    ], NavbarComponent.prototype, "button", void 0);
    NavbarComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'navbar-cmp',
            template: __webpack_require__("../../../../../src/app/shared/navbar/navbar.component.html")
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_3__angular_common__["f" /* Location */], __WEBPACK_IMPORTED_MODULE_0__angular_core__["Y" /* Renderer */], __WEBPACK_IMPORTED_MODULE_0__angular_core__["v" /* ElementRef */], __WEBPACK_IMPORTED_MODULE_2__angular_router__["c" /* Router */]])
    ], NavbarComponent);
    return NavbarComponent;
}());



/***/ }),

/***/ "../../../../../src/app/shared/navbar/navbar.module.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return NavbarModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common__ = __webpack_require__("../../../common/esm5/common.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_router__ = __webpack_require__("../../../router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__navbar_component__ = __webpack_require__("../../../../../src/app/shared/navbar/navbar.component.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};




var NavbarModule = /** @class */ (function () {
    function NavbarModule() {
    }
    NavbarModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["L" /* NgModule */])({
            imports: [__WEBPACK_IMPORTED_MODULE_2__angular_router__["d" /* RouterModule */], __WEBPACK_IMPORTED_MODULE_1__angular_common__["b" /* CommonModule */]],
            declarations: [__WEBPACK_IMPORTED_MODULE_3__navbar_component__["a" /* NavbarComponent */]],
            exports: [__WEBPACK_IMPORTED_MODULE_3__navbar_component__["a" /* NavbarComponent */]]
        })
    ], NavbarModule);
    return NavbarModule;
}());



/***/ }),

/***/ "../../../../../src/app/sidebar/sidebar.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"logo\">\n    <a href=\"/\" class=\"simple-text logo-mini\">\n        <div class=\"logo-img\">\n            <img src=\"/assets/img/ant-media-min-logo.png\" />\n        </div>\n    </a>\n\n    <a href=\"/\" class=\"simple-text logo-normal\" style=\"text-transform:none\" i18n=\"@@antMediaServer\">\n\t\tAnt Media Server\n\t</a>\n</div>\n\n\n<div class=\"sidebar-wrapper\">\n    <!--\n\t<div class=\"user\">\n        <div class=\"photo\">\n            <img src=\"../assets/img/faces/face-2.jpg\" />\n        </div>\n        <div class=\"info\">\n            <a data-toggle=\"collapse\" href=\"#collapseExample\" class=\"collapsed\">\n                <span>\n                    Chet Faker\n                    <b class=\"caret\"></b>\n                </span>\n            </a>\n            <div class=\"clearfix\"></div>\n\n            <div class=\"collapse\" id=\"collapseExample\">\n                <ul class=\"nav\">\n                    <li>\n                        <a href=\"#profile\">\n                            <span class=\"sidebar-mini\">Mp</span>\n                            <span class=\"sidebar-normal\">My Profile</span>\n                        </a>\n                    </li>\n                    <li>\n                        <a href=\"#edit\">\n                            <span class=\"sidebar-mini\">Ep</span>\n                            <span class=\"sidebar-normal\">Edit Profile</span>\n                        </a>\n                    </li>\n                    <li>\n                        <a href=\"#settings\">\n                            <span class=\"sidebar-mini\">S</span>\n                            <span class=\"sidebar-normal\">Settings</span>\n                        </a>\n                    </li>\n                </ul>\n            </div>\n        </div>\n    </div>\n-->\n    <div *ngIf=\"isNotMobileMenu()\">\n        <!--\n        <form class=\"navbar-form navbar-left navbar-search-form\" role=\"search\">\n            <div class=\"input-group\">\n                <span class=\"input-group-addon\"><i class=\"fa fa-search\"></i></span>\n                <input type=\"text\" value=\"\" class=\"form-control\" placeholder=\"Search...\">\n            </div>\n        </form>\n        <ul class=\"nav nav-mobile-menu\">\n\n            <li>\n                <a href=\"#stats\" class=\"dropdown-toggle btn-magnify\" data-toggle=\"dropdown\">\n                    <i class=\"ti-panel\"></i>\n                    <p>Stats</p>\n                </a>\n            </li>\n            <li class=\"dropdown\">\n                <a href=\"#notifications\" class=\"dropdown-toggle btn-rotate\" data-toggle=\"dropdown\">\n                    <i class=\"ti-bell\"></i>\n                    <span class=\"notification\">5</span>\n                    <p class=\"hidden-md hidden-lg\">\n                        Notifications\n                        <b class=\"caret\"></b>\n                    </p>\n                </a>\n                <ul class=\"dropdown-menu\">\n                    <li><a href=\"#not1\">Notification 1</a></li>\n                    <li><a href=\"#not2\">Notification 2</a></li>\n                    <li><a href=\"#not3\">Notification 3</a></li>\n                    <li><a href=\"#not4\">Notification 4</a></li>\n                    <li><a href=\"#another\">Another notification</a></li>\n                </ul>\n            </li>\n            <li>\n                <a class=\"btn-rotate\">\n                    <i class=\"ti-settings\"></i>\n                    <p class=\"hidden-md hidden-lg\">\n                        Settings\n                    </p>\n                </a>\n            </li>\n        </ul>\n        -->\n    </div>\n\n\n    <ul class=\"nav\">\n\n        <li routerLinkActive=\"active\">\n            <a routerLink=\"/dashboard\">\n                <i class=\"ti-panel\"></i>\n                <p i18n=\"@@dashboardMenuItem\">Dashboard</p>\n            </a>\n        </li>\n\n        <li>\n            <!--If it have a submenu-->\n            <a data-toggle=\"collapse\" href=\"#Applications\">\n                <i class=\"ti-package\"></i>\n                <p>\n                    <ng-container i18n=\"@@applicationsMenuItem\">Applications</ng-container>\n                    <b class=\"caret\"></b>\n                </p>\n            </a>\n            <!--Display the submenu items-->\n            <div id=\"Applications\" class=\"collapse\">\n                <ul class=\"nav\">\n                    <li routerLinkActive=\"active\" *ngFor=\"let app of getApps\">\n\n                        <a [routerLink]=\"['/applications/' +  app]\" routerLinkActive=\"active\">\n                            <i class=\"ti-file\"></i>\n                            <span class=\"sidebar-mini\"></span>\n                            <span class=\"sidebar-normal\">{{app}}</span>\n                        </a>\n                    </li>\n                </ul>\n            </div>\n        </li>\n\n        <li *ngIf=\"isNotMobileMenu()\">\n            <a data-toggle=\"collapse\" href=\"#UserAccount\">\n                <i class=\"ti-user\"></i> <p>\n                        <ng-container i18n=\"@@myAccountMenuItem\">My Account</ng-container>\n                        <b class=\"caret\"></b>\n                        </p>\n            </a>\n            <div id=\"UserAccount\" class=\"collapse\">\n                <ul class=\"nav\">\n                    <li>\n                        <a routerLink=\"/pages/changepass\" routerLinkActive=\"active\">\n                            <i class=\"ti-angle-double-right\"></i>\n                            <ng-container i18n=\"@@changePassword\">Change Password</ng-container>\n                        </a>\n\n                    </li>\n                    <li class=\"text-left\">\n                        <a routerLink=\"/pages/login\" routerLinkActive=\"active\">\n\n                            <i class=\"ti-angle-double-right\"></i>\n                            <ng-container i18n=\"@@logout\">Logout</ng-container>\n\n                        </a>\n                    </li>\n                </ul>\n            </div>\n\n        </li>\n\n\n    </ul>\n\n    \n\n</div>"

/***/ }),

/***/ "../../../../../src/app/sidebar/sidebar.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ROUTES; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return SidebarComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common_http__ = __webpack_require__("../../../common/esm5/http.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__rest_rest_service__ = __webpack_require__("../../../../../src/app/rest/rest.service.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



//Menu Items
var ROUTES = [{
        path: '/dashboard',
        title: 'Dashboard',
        type: 'link',
        icontype: 'ti-panel',
    }, {
        path: '/applications',
        title: 'Applications',
        type: 'sub',
        icontype: 'ti-package',
        children: []
    },
    {
        path: '/settings',
        title: 'Settings',
        type: 'link',
        icontype: 'ti-settings',
    }
];
var SidebarComponent = /** @class */ (function () {
    function SidebarComponent(http, restService) {
        this.http = http;
        this.restService = restService;
    }
    SidebarComponent_1 = SidebarComponent;
    SidebarComponent.prototype.isNotMobileMenu = function () {
        if ($(window).width() > 991) {
            return false;
        }
        return true;
    };
    SidebarComponent.prototype.ngOnInit = function () {
        var _this = this;
        var isWindows = navigator.platform.indexOf('Win') > -1 ? true : false;
        this.menuItems = ROUTES.filter(function (menuItem) { return menuItem; });
        isWindows = navigator.platform.indexOf('Win') > -1 ? true : false;
        if (isWindows) {
            // if we are on windows OS we activate the perfectScrollbar function
            $('.sidebar .sidebar-wrapper, .main-panel').perfectScrollbar();
            $('html').addClass('perfect-scrollbar-on');
        }
        else {
            $('html').addClass('perfect-scrollbar-off');
        }
        this.restService.getApplications().subscribe(function (data) {
            SidebarComponent_1.apps = [];
            //second element is the Applications. It is not safe to make static binding.
            _this.menuItems[1].children = [];
            for (var i in data['applications']) {
                //console.log(data['applications'][i]);
                _this.menuItems[1].children.push({ path: data['applications'][i], title: data['applications'][i], icontype: 'ti-file' });
                SidebarComponent_1.apps.push(data['applications'][i]);
            }
        });
    };
    SidebarComponent.prototype.ngAfterViewInit = function () {
        $("#Applications").collapse("show");
    };
    Object.defineProperty(SidebarComponent.prototype, "getApps", {
        get: function () {
            return SidebarComponent_1.apps;
        },
        enumerable: true,
        configurable: true
    });
    SidebarComponent = SidebarComponent_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'sidebar-cmp',
            template: __webpack_require__("../../../../../src/app/sidebar/sidebar.component.html"),
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__angular_common_http__["b" /* HttpClient */], __WEBPACK_IMPORTED_MODULE_2__rest_rest_service__["d" /* RestService */]])
    ], SidebarComponent);
    return SidebarComponent;
    var SidebarComponent_1;
}());



/***/ }),

/***/ "../../../../../src/app/sidebar/sidebar.module.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return SidebarModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common__ = __webpack_require__("../../../common/esm5/common.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_router__ = __webpack_require__("../../../router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__sidebar_component__ = __webpack_require__("../../../../../src/app/sidebar/sidebar.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__angular_common_http__ = __webpack_require__("../../../common/esm5/http.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};





var SidebarModule = /** @class */ (function () {
    function SidebarModule() {
    }
    SidebarModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["L" /* NgModule */])({
            imports: [__WEBPACK_IMPORTED_MODULE_2__angular_router__["d" /* RouterModule */], __WEBPACK_IMPORTED_MODULE_1__angular_common__["b" /* CommonModule */], __WEBPACK_IMPORTED_MODULE_4__angular_common_http__["c" /* HttpClientModule */]],
            declarations: [__WEBPACK_IMPORTED_MODULE_3__sidebar_component__["b" /* SidebarComponent */]],
            exports: [__WEBPACK_IMPORTED_MODULE_3__sidebar_component__["b" /* SidebarComponent */]]
        })
    ], SidebarModule);
    return SidebarModule;
}());



/***/ }),

/***/ "../../../../../src/environments/environment.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return environment; });
// The file contents for the current environment will overwrite these during build.
// The build system defaults to the dev environment which uses `environment.ts`, but if you do
// `ng build --env=prod` then `environment.prod.ts` will be used instead.
// The list of which env maps to which file can be found in `.angular-cli.json`.
var environment = {
    production: false
};


/***/ }),

/***/ "../../../../../src/main.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_platform_browser_dynamic__ = __webpack_require__("../../../platform-browser-dynamic/esm5/platform-browser-dynamic.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_hammerjs__ = __webpack_require__("../../../../hammerjs/hammer.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_hammerjs___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_hammerjs__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__app_app_module__ = __webpack_require__("../../../../../src/app/app.module.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__environments_environment__ = __webpack_require__("../../../../../src/environments/environment.ts");





if (__WEBPACK_IMPORTED_MODULE_4__environments_environment__["a" /* environment */].production) {
    Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_18" /* enableProdMode */])();
}
Object(__WEBPACK_IMPORTED_MODULE_1__angular_platform_browser_dynamic__["a" /* platformBrowserDynamic */])().bootstrapModule(__WEBPACK_IMPORTED_MODULE_3__app_app_module__["a" /* AppModule */]);


/***/ }),

/***/ 0:
/***/ (function(module, exports, __webpack_require__) {

module.exports = __webpack_require__("../../../../../src/main.ts");


/***/ })

},[0]);
//# sourceMappingURL=main.bundle.js.map