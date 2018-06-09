webpackJsonp(["dashboard.module"],{

/***/ "./src/app/dashboard/dashboard.module.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "DashboardModule", function() { return DashboardModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__("./node_modules/@angular/router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__("./node_modules/@angular/common/esm5/common.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_forms__ = __webpack_require__("./node_modules/@angular/forms/esm5/forms.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__overview_overview_component__ = __webpack_require__("./src/app/dashboard/overview/overview.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__dashboard_routing__ = __webpack_require__("./src/app/dashboard/dashboard.routing.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__angular_common_http__ = __webpack_require__("./node_modules/@angular/common/esm5/http.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};







var DashboardModule = /** @class */ (function () {
    function DashboardModule() {
    }
    DashboardModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["L" /* NgModule */])({
            imports: [
                __WEBPACK_IMPORTED_MODULE_2__angular_common__["b" /* CommonModule */],
                __WEBPACK_IMPORTED_MODULE_1__angular_router__["d" /* RouterModule */].forChild(__WEBPACK_IMPORTED_MODULE_5__dashboard_routing__["a" /* DashboardRoutes */]),
                __WEBPACK_IMPORTED_MODULE_3__angular_forms__["c" /* FormsModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_common_http__["c" /* HttpClientModule */],
            ],
            declarations: [__WEBPACK_IMPORTED_MODULE_4__overview_overview_component__["a" /* OverviewComponent */]],
        })
    ], DashboardModule);
    return DashboardModule;
}());



/***/ }),

/***/ "./src/app/dashboard/dashboard.routing.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return DashboardRoutes; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__overview_overview_component__ = __webpack_require__("./src/app/dashboard/overview/overview.component.ts");

var DashboardRoutes = [
    {
        path: '',
        children: [
            {
                path: '',
                redirectTo: 'overview'
            },
            {
                path: 'overview',
                component: __WEBPACK_IMPORTED_MODULE_0__overview_overview_component__["a" /* OverviewComponent */]
            }
        ]
    }
];


/***/ }),

/***/ "./src/app/dashboard/overview/overview.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"main-content\">\n    <div class=\"container-fluid\">\n        <div class=\"row\">\n            <div class=\"col-lg-6 col-sm-6\">\n                <div class=\"card\">\n                    <div class=\"card-content\">\n                        <div class=\"row\">\n                            <div class=\"col-xs-5\">\n                                <div class=\"icon-big icon-warning text-center\">\n                                    <i class=\"ti-server\"></i>\n                                </div>\n                            </div>\n                            <div class=\"col-xs-7\">\n                                <div class=\"numbers\">\n                                    <p i18n=\"@@cpuLoadLabel\">CPU Load</p>\n                                    {{cpuLoad}}%\n                                </div>\n                            </div>\n                        </div>\n                    </div>\n                    <div class=\"card-footer\">\n                        <hr />\n                        <!--\n                        <div class=\"stats\">\n                            <i class=\"ti-reload\"></i> Updated now\n                        </div>\n                    -->\n                    </div>\n                </div>\n            </div>\n            <div class=\"col-lg-6 col-sm-6\">\n                <div class=\"card\">\n                    <div class=\"card-content\">\n                        <div class=\"row\">\n                            <div class=\"col-xs-5\">\n                                <div class=\"icon-big icon-success text-center\">\n                                    <i class=\"ti-wallet\"></i>\n                                </div>\n                            </div>\n                            <div class=\"col-xs-7\">\n                                <div class=\"numbers\">\n                                    <p i18n=\"@@liveStreamCountLabel\">Active Live Streams</p>\n                                    {{liveStreamSize}}\n                                </div>\n                            </div>\n                        </div>\n                    </div>\n                    <div class=\"card-footer\">\n                        <hr />\n                        <!--\n                        <div class=\"stats\">\n                            <i class=\"ti-calendar\"></i> Last day\n                        </div>\n                    -->\n                    </div>\n                </div>\n            </div>\n            <!--\n            <div class=\"col-lg-4 col-sm-6\">\n                <div class=\"card\">\n                    <div class=\"card-content\">\n                        <div class=\"row\">\n                            <div class=\"col-xs-5\">\n                                <div class=\"icon-big icon-danger text-center\">\n                                    <i class=\"ti-pulse\"></i>\n                                </div>\n                            </div>\n                            <div class=\"col-xs-7\">\n                                <div class=\"numbers\">\n                                    <p i18n=\"@@liveWatchersCountLabel\">Live Watchers</p>\n                                    {{watcherSize}}\n                                </div>\n                            </div>\n                        </div>\n                    </div>\n                    <div class=\"card-footer\">\n                        <hr />\n                  \n                    </div>\n                </div>\n            </div>\n        -->\n        </div>\n        <div class=\"row\">\n            <div class=\"col-lg-6 col-sm-6\">\n                <div class=\"card card-circle-chart\">\n                    <div class=\"card-header text-center\">\n                        <h5 class=\"card-title\" i18n=\"@@diskUsageLabel\">Disk Usage</h5>\n                        <p class=\"description\">{{niceBytes(diskInUseSpace)}} / {{niceBytes(diskTotalSpace)}}</p>\n                    </div>\n                    <div class=\"card-content\">\n                        <div id=\"chartDiskUsage\" class=\"chart-circle\">{{diskUsagePercent}}%</div>\n                    </div>\n                </div>\n            </div>\n            <div class=\"col-lg-6 col-sm-6\">\n                <div class=\"card card-circle-chart\">\n                    <div class=\"card-header text-center\">\n                        <h5 class=\"card-title\" i18n=\"@@serverMemoryUsageLabel\">Memory Usage</h5>\n                        <p class=\"description\">{{niceBytes(memoryInUseSpace)}} / {{niceBytes(memoryTotalSpace)}}</p>\n                    </div>\n                    <div class=\"card-content\">\n                        <div id=\"chartMemoryUsage\" class=\"chart-circle\">{{memoryUsagePercent}}%</div>\n                    </div>\n                </div>\n            </div>\n            <!--\n            <div class=\"col-lg-4 col-sm-6\">\n                <div class=\"card card-circle-chart\">\n                    <div class=\"card-header text-center\">\n                        <h5 class=\"card-title\" i18n=\"@@systemMemoryUsageLabel\">System Memory Usage</h5>\n                        <p class=\"description\">{{niceBytes(systemMemoryInUse)}} / {{niceBytes(systemMemoryTotal)}}</p>\n                    </div>\n                    <div class=\"card-content\">\n                        <div id=\"chartSystemMemory\" class=\"chart-circle\">{{systemMemoryUsagePercent}}%</div>\n                    </div>\n                </div>\n            </div>\n        -->\n        </div>\n        <div class=\"row\">\n            <div class=\"col-md-12\">\n\n                <div class=\"card\">\n                    <div class=\"card-header\">\n                        <h4 class=\"card-title\" i18n=\"@@applicationsTitle\">Applications</h4>\n                    </div>\n                    <div class=\"card-content table-full-width\">\n                        <table class=\"table table-striped\">\n                            <thead>\n                                <tr>\n                                    <th class=\"text-center\">#</th>\n                                    <th i18n=\"@@appName\">Name</th>\n                                    <th class=\"text-center\" i18n=\"@@liveStreamCountLabel\" >Live Stream</th>\n                                    <th class=\"text-center\" i18n=\"@@vodCountLabel\"*ngIf=\"isMobileMenu()\">VoD Count</th>\n                                    <th class=\"text-center\" i18n=\"@@appStorageSizeLabel\">Storage</th>\n                                </tr>\n                            </thead>\n                            <tbody>\n                                <!---->\n                                <tr *ngFor=\"let row of appTableData.dataRows; let i=index\" >\n                                   \n                                      <td class=\"text-center\">{{i+1}}</td>\n                                      <td>{{row.name}}</td>\n                                      <td class=\"text-center\">{{row.liveStreamCount}}</td>\n                                      <td class=\"text-center\"*ngIf=\"isMobileMenu()\">{{row.vodCount}}</td>\n                                      <td class=\"text-center\">{{niceBytes(row.storage)}}</td>\n                               \n                                </tr>\n                            </tbody>\n                        </table>\n                    </div>\n                </div>\n            </div>\n        </div>\n    </div>\n</div>"

/***/ }),

/***/ "./src/app/dashboard/overview/overview.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return OverviewComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__rest_rest_service__ = __webpack_require__("./src/app/rest/rest.service.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_router__ = __webpack_require__("./node_modules/@angular/router/esm5/router.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var OverviewComponent = /** @class */ (function () {
    function OverviewComponent(/*private http: HttpClient,*/ restService, router) {
        this.restService = restService;
        this.router = router;
        this.cpuLoadIntervalId = 0;
        this.systemMemoryUsagePercent = 0;
        this.units = ['bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
    }
    OverviewComponent.prototype.niceBytes = function (x) {
        var l = 0, n = parseInt(x, 10) || 0;
        while (n >= 1000) {
            n = n / 1000;
            l++;
        }
        return (n.toFixed(n >= 10 || l < 1 ? 0 : 1) + ' ' + this.units[l]);
    };
    OverviewComponent.prototype.initCirclePercentage = function () {
        $('#chartSystemMemory, #chartDiskUsage, #chartMemoryUsage').easyPieChart({
            lineWidth: 9,
            size: 160,
            scaleColor: false,
            trackColor: '#BBDEFB',
            barColor: '#1565C0',
            animate: false,
        });
    };
    OverviewComponent.prototype.ngOnInit = function () {
        this.appTableData = {
            dataRows: []
        };
    };
    OverviewComponent.prototype.ngAfterViewInit = function () {
        var _this = this;
        this.initCirclePercentage();
        this.updateCPULoad();
        this.getLiveClientsSize();
        this.getSystemMemoryInfo();
        this.getFileSystemInfo();
        this.getJVMMemoryInfo();
        this.getApplicationsInfo();
        this.timerId = window.setInterval(function () {
            _this.updateCPULoad();
            _this.getLiveClientsSize();
            _this.getSystemMemoryInfo();
            _this.getFileSystemInfo();
            _this.getJVMMemoryInfo();
            _this.getApplicationsInfo();
        }, 5000);
    };
    OverviewComponent.prototype.ngOnDestroy = function () {
        if (this.timerId) {
            clearInterval(this.timerId);
        }
    };
    OverviewComponent.prototype.updateCPULoad = function () {
        var _this = this;
        this.restService.getCPULoad().subscribe(function (data) {
            _this.cpuLoad = Number(data["systemCPULoad"]);
            //["systemCPULoad"]
        }, this.handleError);
    };
    OverviewComponent.prototype.handleError = function (error) {
        console.log("error status: " + error.status);
        if (error.status == 401) {
            console.log(this.router);
            //this.router.navigateByUrl("/pages/login");
        }
    };
    OverviewComponent.prototype.getLiveClientsSize = function () {
        var _this = this;
        this.restService.getLiveClientsSize().subscribe(function (data) {
            _this.liveStreamSize = Number(data["totalLiveStreamSize"]);
            _this.watcherSize = Number(data["totalConnectionSize"]) - _this.liveStreamSize;
        });
    };
    OverviewComponent.prototype.getSystemMemoryInfo = function () {
        var _this = this;
        this.restService.getSystemMemoryInfo().subscribe(function (data) {
            var freeSpace = Number(data["freeMemory"]);
            _this.systemMemoryInUse = Number(data["inUseMemory"]);
            _this.systemMemoryTotal = Number(data["totalMemory"]);
            _this.systemMemoryUsagePercent = Math.round(_this.systemMemoryInUse * 100 / _this.systemMemoryTotal);
            //TODO: open it if this chart will be used
            //$('#chartSystemMemory').data('easyPieChart').update(this.systemMemoryUsagePercent);
        });
    };
    OverviewComponent.prototype.getFileSystemInfo = function () {
        var _this = this;
        this.restService.getFileSystemInfo().subscribe(function (data) {
            // Read the result field from the JSON response.
            var freeSpace = Number(data["freeSpace"]);
            _this.diskInUseSpace = Number(data["inUseSpace"]);
            _this.diskTotalSpace = Number(data["totalSpace"]);
            _this.diskUsagePercent = Math.round(_this.diskInUseSpace * 100 / _this.diskTotalSpace);
            $("#chartDiskUsage").data('easyPieChart').update(_this.diskUsagePercent);
        });
    };
    OverviewComponent.prototype.getJVMMemoryInfo = function () {
        var _this = this;
        this.restService.getJVMMemoryInfo().subscribe(function (data) {
            _this.memoryInUseSpace = Number(data["inUseMemory"]);
            _this.memoryTotalSpace = Number(data["maxMemory"]);
            _this.memoryUsagePercent = Math.round(Number(_this.memoryInUseSpace * 100 / _this.memoryTotalSpace));
            $("#chartMemoryUsage").data('easyPieChart').update(_this.memoryUsagePercent);
        });
    };
    OverviewComponent.prototype.getApplicationsInfo = function () {
        var _this = this;
        this.restService.getApplicationsInfo().subscribe(function (data) {
            _this.appTableData.dataRows = [];
            for (var i in data) {
                _this.appTableData.dataRows.push(data[i]);
            }
        });
    };
    OverviewComponent.prototype.isMobileMenu = function () {
        if ($(window).width() > 991) {
            return true;
        }
        return false;
    };
    OverviewComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            selector: 'overview-cmp',
            template: __webpack_require__("./src/app/dashboard/overview/overview.component.html")
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__rest_rest_service__["d" /* RestService */], __WEBPACK_IMPORTED_MODULE_2__angular_router__["c" /* Router */]])
    ], OverviewComponent);
    return OverviewComponent;
}());



/***/ })

});
//# sourceMappingURL=dashboard.module.chunk.js.map