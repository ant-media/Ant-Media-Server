webpackJsonp(["cluster.module"],{

/***/ "./src/app/cluster/cluster.component.css":
/***/ (function(module, exports) {

module.exports = ""

/***/ }),

/***/ "./src/app/cluster/cluster.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"main-content\">\n    <div class=\"container-fluid\">\n        <div class=\"row\">\n            <div class=\"col-lg-12 col-sm-12\">\n                <div class=\"card\">\n                    <div class=\"card-content\">\n                       \n                                <div class=\"container\">\n\n                                </div>\n\n                                <div class=\" text-right\">\n\n                                    <div>\n                                        <button class=\"btn btn-fill btn-success\" (click)=\"openNodeCreateDialog()\"\n                                                data-toggle=\"dropdown\" role=\"button\">Add\n                                        </button>\n                                    </div>\n\n                                </div>\n\n                                <div id=\"nodeTable\" style=\"text-align: left\"\n                                     *ngIf=\" nodeTableData.dataRows.length>0\">\n\n                                    <br>\n                                    <div class=\"mat-container mat-elevation-z1\">\n\n                                        <mat-table [dataSource]=\"dataSourceNode\">\n\n                                            <!-- >ng-container matColumnDef=\"nodeId\">\n                                                <mat-header-cell *matHeaderCellDef> Node ID </mat-header-cell>\n\n                                                <mat-cell *matCellDef=\"let row\">\n\n                                                    <ng-container [ngSwitch]=\"row.type\">\n                                                        <i> {{row.id}}</i>\n                                                    </ng-container>\n\n                                                </mat-cell>\n\n                                            </ng-container  -->\n\n                                            <ng-container matColumnDef=\"nodeIp\">\n                                                <mat-header-cell *matHeaderCellDef> Node IP </mat-header-cell>\n\n                                                <mat-cell *matCellDef=\"let row\">\n                                                    {{row.ip}}\n\n                                                </mat-cell>\n                                            </ng-container>\n\n\t\t\t\t\t\t\t\t\t\t\t\n                                            <!--  >ng-container matColumnDef=\"status\">\n                                                <mat-header-cell *matHeaderCellDef> Status </mat-header-cell>\n\n                                                <mat-cell *matCellDef=\"let row\">\n                                                    {{row.status}}\n\n                                                </mat-cell>\n                                            </ng-container  -->\n                                            \n                                            <ng-container matColumnDef=\"lastUpdateTime\">\n                                                <mat-header-cell *matHeaderCellDef> Last Update </mat-header-cell>\n\n                                                <mat-cell *matCellDef=\"let row\">\n                                                \t{{row.lastUpdateTime | date:'yyyy-MM-dd HH:mm:ss'}}\n                                                </mat-cell>\n                                            </ng-container>\n                                            \n                                            <ng-container matColumnDef=\"cpu\">\n                                                <mat-header-cell *matHeaderCellDef> CPU Usage (%) </mat-header-cell>\n\n                                                <mat-cell *matCellDef=\"let row\">\n                                                \t{{row.cpu}}\n                                                </mat-cell>\n                                            </ng-container>\n                                            \n                                            <ng-container matColumnDef=\"memory\">\n                                                <mat-header-cell *matHeaderCellDef> Memory Usage (MB) </mat-header-cell>\n\n                                                <mat-cell *matCellDef=\"let row\">\n                                                \t{{row.memory}}\n                                                </mat-cell>\n                                            </ng-container>\n                                            \n                                            <ng-container matColumnDef=\"inTheCluster\">\n                                                <mat-header-cell *matHeaderCellDef> Alive </mat-header-cell>\n\n                                                <mat-cell *matCellDef=\"let row\">\n                                                    {{row.inTheCluster}}\n\n                                                </mat-cell>\n                                            </ng-container>\n\n                                            <ng-container matColumnDef=\"actions\">\n                                                <mat-header-cell *matHeaderCellDef> Actions</mat-header-cell>\n\n                                                <mat-cell *matCellDef=\"let row\">\n\n\t\t\t\t\t\t\t\t\t\t\t\t\t\n                                                    <!-- button (click)=\"openNodeEditDialog(row.id)\"\n                                                            class=\"btn btn-success btn-simple btn-magnify\"\n                                                            type=\"button\">\n                                                        <span class=\"btn-label\">\n                                                            <i class=\"ti-pencil\"></i>\n                                                        </span>\n                                                        <ng-container i18n=\"@@editNodeButton\">Edit</ng-container>\n                                                    </button -->\n                                                    <button (click)=\"deleteNode(row.id)\" class=\"btn btn-simple btn-magnify btn-danger\" type=\"button\">\n                                                        <span class=\"btn-label\">\n                                                            <i class=\"ti-close\"></i>\n                                                        </span>\n                                                        <ng-container i18n=\"@@deleteNodeButton\">Delete</ng-container>\n                                                    </button>\n\n                                                </mat-cell>\n                                            </ng-container>\n\n                                            <mat-header-row *matHeaderRowDef=\"nodeColumns\"></mat-header-row>\n                                            <mat-row *matRowDef=\"let row; columns: nodeColumns;\">\n                                            </mat-row>\n                                        </mat-table>\n\n\n                                        <mat-paginator [length]=\"nodeLength\"\n                                                       [pageSize]=\"pageSize\"\n                                                       [pageSizeOptions]=\"pageSizeOptions\"\n                                                       (page)=\"onPaginateChange($event)\"\n                                        >\n                                        </mat-paginator>\n\n\n                                    </div>\n\n                                </div>\n\n\n                                <p *ngIf=\"nodeTableData.dataRows.length == 0\" i18n=\"no cluster node | text messages appears when no node@@noNodeExistsMessage\">\n                                    There is no cluster node.\n                                </p>\n                        <div class=\"footer\">\n                            <!--\n                        <hr />\n\n                        <div class=\"stats\">\n                               <button class=\"btn btn-primary btn-simple btn-sm\" (click)=\"updateCPULoad()\">\n                                   <i class=\"ti-reload\"></i>Update now</button>\n                        </div>\n                    -->\n                        </div>\n                    </div>\n                </div>\n            </div>\n        </div>\n    </div>\n</div>"

/***/ }),

/***/ "./src/app/cluster/cluster.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ClusterComponent; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return EditNodeComponent; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return CreateNodeComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common_http__ = __webpack_require__("./node_modules/@angular/common/esm5/http.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_router__ = __webpack_require__("./node_modules/@angular/router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__rest_cluster_service__ = __webpack_require__("./src/app/rest/cluster.service.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__locale_locale__ = __webpack_require__("./src/app/locale/locale.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__angular_material__ = __webpack_require__("./node_modules/@angular/material/esm5/material.es5.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_rxjs_add_operator_toPromise__ = __webpack_require__("./node_modules/rxjs/_esm5/add/operator/toPromise.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_rxjs_add_operator_toPromise___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_6_rxjs_add_operator_toPromise__);
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







var ClusterComponent = /** @class */ (function () {
    function ClusterComponent(http, route, clusterRestService, renderer, router, dialog, cdr, matpage) {
        this.http = http;
        this.route = route;
        this.clusterRestService = clusterRestService;
        this.renderer = renderer;
        this.router = router;
        this.dialog = dialog;
        this.cdr = cdr;
        this.matpage = matpage;
        this.nodeColumns = ['nodeIp', 'lastUpdateTime', 'cpu', 'memory', 'inTheCluster', 'actions'];
        this.pageSize = 5;
        this.pageSizeOptions = [5, 10, 25];
        this.pageIndex = 0;
        this.dataSourceNode = new __WEBPACK_IMPORTED_MODULE_5__angular_material__["I" /* MatTableDataSource */]();
    }
    ClusterComponent.prototype.setPageSizeOptions = function (setPageSizeOptionsInput) {
        this.pageSizeOptions = setPageSizeOptionsInput.split(',').map(function (str) { return +str; });
    };
    ClusterComponent.prototype.ngOnInit = function () {
        var self = this;
        this.nodeTableData = {
            dataRows: []
        };
    };
    ClusterComponent.prototype.onPaginateChange = function (event) {
        console.log("page index:" + event.pageIndex);
        console.log("length:" + event.length);
        console.log("page size:" + event.pageSize);
        this.pageIndex = event.pageIndex;
        this.updateTable();
    };
    ClusterComponent.prototype.ngAfterViewInit = function () {
        var _this = this;
        setTimeout(function () {
            _this.getClusterNodes();
        }, 500);
        this.timerId = window.setInterval(function () {
            _this.getClusterNodes();
        }, 10000);
    };
    ClusterComponent.prototype.ngOnDestroy = function () {
        if (this.timerId) {
            clearInterval(this.timerId);
        }
    };
    ClusterComponent.prototype.updateTable = function () {
        var start = this.pageIndex * this.pageSize;
        var end = start + this.pageSize;
        this.dataSourceNode = new __WEBPACK_IMPORTED_MODULE_5__angular_material__["I" /* MatTableDataSource */](this.nodeTableData.dataRows.slice(start, end));
    };
    ClusterComponent.prototype.getClusterNodes = function () {
        var _this = this;
        this.clusterRestService.getClusterNodes().subscribe(function (data) {
            _this.nodeTableData.dataRows = [];
            for (var i in data) {
                _this.nodeTableData.dataRows.push(data[i]);
            }
            _this.nodeLength = _this.nodeTableData.dataRows.length;
            _this.updateTable();
        });
    };
    ClusterComponent.prototype.openNodeEditDialog = function (nodeId) {
        var _this = this;
        var node = this.nodeTableData.dataRows.find(function (n) { return n.id == nodeId; });
        var dialogRef = this.dialog.open(EditNodeComponent, {
            data: {
                node: node,
            },
            width: '300px'
        });
        dialogRef.afterClosed().subscribe(function (result) {
            console.log('The dialog was closed');
            _this.getClusterNodes();
        });
    };
    ClusterComponent.prototype.openNodeCreateDialog = function () {
        var _this = this;
        var dialogRef = this.dialog.open(CreateNodeComponent, {
            data: {},
            width: '300px'
        });
        dialogRef.afterClosed().subscribe(function (result) {
            console.log('The dialog was closed');
            _this.getClusterNodes();
        });
    };
    ClusterComponent.prototype.deleteNode = function (nodeId) {
        var _this = this;
        swal({
            title: __WEBPACK_IMPORTED_MODULE_4__locale_locale__["a" /* Locale */].getLocaleInterface().are_you_sure,
            text: __WEBPACK_IMPORTED_MODULE_4__locale_locale__["a" /* Locale */].getLocaleInterface().wont_be_able_to_revert,
            type: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#3085d6',
            cancelButtonColor: '#d33',
            confirmButtonText: 'Yes, delete it!'
        }).then(function () {
            var node = _this.nodeTableData.dataRows.find(function (n) { return n.id == nodeId; });
            _this.clusterRestService.deleteClusterNodes(node).subscribe(function (data) {
                if (data["success"] == true) {
                }
                else {
                    _this.showNodeNotDeleted();
                }
                ;
                _this.getClusterNodes();
            });
        }).catch(function () {
        });
    };
    ClusterComponent.prototype.showNodeNotDeleted = function () {
        $.notify({
            icon: "ti-save",
            message: "Node can not be deleted."
        }, {
            type: "warning",
            delay: 900,
            placement: {
                from: 'top',
                align: 'right'
            }
        });
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["G" /* Input */])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_5__angular_material__["N" /* PageEvent */])
    ], ClusterComponent.prototype, "pageEvent", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["S" /* Output */])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["y" /* EventEmitter */])
    ], ClusterComponent.prototype, "pageChange", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_12" /* ViewChild */])(__WEBPACK_IMPORTED_MODULE_5__angular_material__["F" /* MatSort */]),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_5__angular_material__["F" /* MatSort */])
    ], ClusterComponent.prototype, "sort", void 0);
    ClusterComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            selector: 'app-cluster',
            template: __webpack_require__("./src/app/cluster/cluster.component.html"),
            styles: [__webpack_require__("./src/app/cluster/cluster.component.css")]
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__angular_common_http__["b" /* HttpClient */],
            __WEBPACK_IMPORTED_MODULE_2__angular_router__["a" /* ActivatedRoute */],
            __WEBPACK_IMPORTED_MODULE_3__rest_cluster_service__["a" /* ClusterRestService */],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["Y" /* Renderer */],
            __WEBPACK_IMPORTED_MODULE_2__angular_router__["c" /* Router */],
            __WEBPACK_IMPORTED_MODULE_5__angular_material__["i" /* MatDialog */],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["l" /* ChangeDetectorRef */],
            __WEBPACK_IMPORTED_MODULE_5__angular_material__["u" /* MatPaginatorIntl */]])
    ], ClusterComponent);
    return ClusterComponent;
}());

var EditNodeComponent = /** @class */ (function () {
    function EditNodeComponent(dialogRef, clusterRestService, data) {
        this.dialogRef = dialogRef;
        this.clusterRestService = clusterRestService;
        this.data = data;
        this.progressing = false;
        this.node = data.node;
    }
    EditNodeComponent.prototype.cancel = function () {
        this.dialogRef.close();
    };
    EditNodeComponent.prototype.updateNode = function () {
        var _this = this;
        this.progressing = true;
        this.clusterRestService.updateClusterNodes(this.node).subscribe(function (data) {
            if (data["success"] == true) {
                _this.progressing = false;
                _this.dialogRef.close();
                swal({
                    type: "success",
                    title: " Node is successfully updated!",
                    buttonsStyling: false,
                    confirmButtonClass: "btn btn-success"
                });
            }
            else {
                _this.progressing = false;
                _this.dialogRef.close();
                swal({
                    type: "error",
                    title: "An Error Occured!",
                    buttonsStyling: false,
                    confirmButtonClass: "btn btn-error"
                });
            }
        });
    };
    EditNodeComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            selector: 'edit-node-dialog',
            template: __webpack_require__("./src/app/cluster/edit-node-dialog.html"),
        }),
        __param(2, Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["C" /* Inject */])(__WEBPACK_IMPORTED_MODULE_5__angular_material__["a" /* MAT_DIALOG_DATA */])),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_5__angular_material__["k" /* MatDialogRef */],
            __WEBPACK_IMPORTED_MODULE_3__rest_cluster_service__["a" /* ClusterRestService */], Object])
    ], EditNodeComponent);
    return EditNodeComponent;
}());

var CreateNodeComponent = /** @class */ (function () {
    function CreateNodeComponent(dialogRef, clusterRestService, data) {
        this.dialogRef = dialogRef;
        this.clusterRestService = clusterRestService;
        this.data = data;
        this.progressing = false;
        this.node = { id: '', ip: '', status: '', lastUpdateTime: "", inTheCluster: "", memory: "", cpu: "" };
    }
    CreateNodeComponent.prototype.cancel = function () {
        this.dialogRef.close();
    };
    CreateNodeComponent.prototype.createNode = function () {
        var _this = this;
        this.progressing = true;
        this.clusterRestService.addClusterNodes(this.node).subscribe(function (data) {
            if (data["success"] == true) {
                _this.progressing = false;
                _this.dialogRef.close();
                swal({
                    type: "success",
                    title: " Node is successfully added!",
                    buttonsStyling: false,
                    confirmButtonClass: "btn btn-success"
                });
            }
            else {
                _this.progressing = false;
                _this.dialogRef.close();
                swal({
                    type: "error",
                    title: "An Error Occured!",
                    buttonsStyling: false,
                    confirmButtonClass: "btn btn-error"
                });
            }
        });
    };
    CreateNodeComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            selector: 'create-node-dialog',
            template: __webpack_require__("./src/app/cluster/create-node-dialog.html"),
        }),
        __param(2, Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["C" /* Inject */])(__WEBPACK_IMPORTED_MODULE_5__angular_material__["a" /* MAT_DIALOG_DATA */])),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_5__angular_material__["k" /* MatDialogRef */],
            __WEBPACK_IMPORTED_MODULE_3__rest_cluster_service__["a" /* ClusterRestService */], Object])
    ], CreateNodeComponent);
    return CreateNodeComponent;
}());



/***/ }),

/***/ "./src/app/cluster/cluster.module.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "ClusterRoutes", function() { return ClusterRoutes; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "ClusterModule", function() { return ClusterModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__("./node_modules/@angular/router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__("./node_modules/@angular/common/esm5/common.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_forms__ = __webpack_require__("./node_modules/@angular/forms/esm5/forms.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__cluster_component__ = __webpack_require__("./src/app/cluster/cluster.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__angular_common_http__ = __webpack_require__("./node_modules/@angular/common/esm5/http.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__angular_material__ = __webpack_require__("./node_modules/@angular/material/esm5/material.es5.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};








var ClusterRoutes = [
    {
        path: '',
        component: __WEBPACK_IMPORTED_MODULE_4__cluster_component__["a" /* ClusterComponent */],
        pathMatch: 'full',
    },
];
var ClusterModule = /** @class */ (function () {
    function ClusterModule() {
    }
    ClusterModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["L" /* NgModule */])({
            declarations: [
                __WEBPACK_IMPORTED_MODULE_4__cluster_component__["a" /* ClusterComponent */],
                __WEBPACK_IMPORTED_MODULE_4__cluster_component__["b" /* CreateNodeComponent */],
                __WEBPACK_IMPORTED_MODULE_4__cluster_component__["c" /* EditNodeComponent */]
            ],
            entryComponents: [
                __WEBPACK_IMPORTED_MODULE_4__cluster_component__["b" /* CreateNodeComponent */],
                __WEBPACK_IMPORTED_MODULE_4__cluster_component__["c" /* EditNodeComponent */]
            ],
            imports: [
                __WEBPACK_IMPORTED_MODULE_1__angular_router__["d" /* RouterModule */].forChild(ClusterRoutes),
                __WEBPACK_IMPORTED_MODULE_2__angular_common__["b" /* CommonModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["n" /* MatFormFieldModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["q" /* MatInputModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["f" /* MatCheckboxModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["j" /* MatDialogModule */],
                __WEBPACK_IMPORTED_MODULE_3__angular_forms__["c" /* FormsModule */],
                __WEBPACK_IMPORTED_MODULE_5__angular_common_http__["c" /* HttpClientModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["j" /* MatDialogModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["f" /* MatCheckboxModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["q" /* MatInputModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["b" /* MatAutocompleteModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["c" /* MatButtonModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["d" /* MatButtonToggleModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["e" /* MatCardModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["g" /* MatChipsModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["h" /* MatDatepickerModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["l" /* MatDividerModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["m" /* MatExpansionModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["o" /* MatGridListModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["p" /* MatIconModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["r" /* MatListModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["s" /* MatMenuModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["t" /* MatNativeDateModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["v" /* MatPaginatorModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["w" /* MatProgressBarModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["x" /* MatProgressSpinnerModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["y" /* MatRadioModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["z" /* MatRippleModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["A" /* MatSelectModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["B" /* MatSidenavModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["D" /* MatSliderModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["C" /* MatSlideToggleModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["E" /* MatSnackBarModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["G" /* MatSortModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["H" /* MatStepperModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["J" /* MatTableModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["K" /* MatTabsModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["L" /* MatToolbarModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["M" /* MatTooltipModule */]
            ],
            exports: [
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["f" /* MatCheckboxModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["j" /* MatDialogModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["n" /* MatFormFieldModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["q" /* MatInputModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["j" /* MatDialogModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["f" /* MatCheckboxModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["n" /* MatFormFieldModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["q" /* MatInputModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["b" /* MatAutocompleteModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["c" /* MatButtonModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["d" /* MatButtonToggleModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["e" /* MatCardModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["g" /* MatChipsModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["h" /* MatDatepickerModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["l" /* MatDividerModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["m" /* MatExpansionModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["o" /* MatGridListModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["p" /* MatIconModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["r" /* MatListModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["s" /* MatMenuModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["t" /* MatNativeDateModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["v" /* MatPaginatorModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["w" /* MatProgressBarModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["x" /* MatProgressSpinnerModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["y" /* MatRadioModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["z" /* MatRippleModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["A" /* MatSelectModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["B" /* MatSidenavModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["D" /* MatSliderModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["C" /* MatSlideToggleModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["E" /* MatSnackBarModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["G" /* MatSortModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["H" /* MatStepperModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["J" /* MatTableModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["K" /* MatTabsModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["L" /* MatToolbarModule */],
                __WEBPACK_IMPORTED_MODULE_6__angular_material__["M" /* MatTooltipModule */]
            ],
        })
    ], ClusterModule);
    return ClusterModule;
}());



/***/ }),

/***/ "./src/app/cluster/create-node-dialog.html":
/***/ (function(module, exports) {

module.exports = "\n\n<div >\n    <td colspan=\"4\" style=\"border-top:0px\">\n        <form method=\"post\" #f=\"ngForm\">\n\n            <div class=\"card-content\" style=\"padding-top:0px\">\n                <h4 class=\"card-title text-left\" i18n=\"@@editNodeCardTitle\">\n                    Create Node\n                </h4>\n                <div class=\"form-group text-left\">\n                    <label i18n=\"@@newNodeId\">Id</label>\n                    <input type=\"text\" required minlength=\"4\" name=\"nodeId\" i18n-placeholder=\"@@node_id_place_holder\" placeholder=\"Node ID\"\n                           class=\"form-control\" [(ngModel)]=\"node.id\" #nodeId=\"ngModel\">\n                </div>\n                <div class=\"form-group text-left\">\n                    <label i18n=\"@@newNodeIp\">Ip</label>\n                    <input type=\"text\" required minlength=\"4\" name=\"nodeIp\" i18n-placeholder=\"@@node_ip_place_holder\" placeholder=\"Node IP\"\n                           class=\"form-control\" [(ngModel)]=\"node.ip\" #nodeIp=\"ngModel\">\n                </div>\n                \n\n                <div class=\"form-group text-center\">\n\n                    <button type=\"submit\" [disabled]='progressing' class=\"btn btn-fill btn-success\" i18n=\"@@newNodeSaveButton\" (click)=\"createNode()\">\n                        <i class=\"fa fa-spinner fa-pulse fa-1x fa-fw\" *ngIf=\"progressing\" aria-hidden=\"true\"></i>Save</button>\n\n                    <button type=\"button\" class=\"btn btn-simple\" i18n=\"@@newNodeCancelButton\" (click)=\"cancel()\">Cancel</button>\n                </div>\n\n            </div>\n\n        </form>\n    </td>\n</div>\n\n"

/***/ }),

/***/ "./src/app/cluster/edit-node-dialog.html":
/***/ (function(module, exports) {

module.exports = "\n\n<div >\n    <td colspan=\"4\" style=\"border-top:0px\">\n        <form method=\"post\" #f=\"ngForm\">\n\n            <div class=\"card-content\" style=\"padding-top:0px\">\n                <h4 class=\"card-title text-left\" i18n=\"@@editNodeCardTitle\">\n                    Edit Node\n                </h4>\n                <div class=\"form-group text-left\">\n                    <label i18n=\"@@newNodeId\">Id</label>\n                    <input type=\"text\" disabled name=\"nodeId\" i18n-placeholder=\"@@node_id_place_holder\" placeholder=\"Node ID\"\n                           class=\"form-control\" [(ngModel)]=\"node.id\" #nodeId=\"ngModel\">\n                </div>\n                <div class=\"form-group text-left\">\n                    <label i18n=\"@@newNodeIp\">Ip</label>\n                    <input type=\"text\" required minlength=\"4\" name=\"nodeIp\" i18n-placeholder=\"@@node_ip_place_holder\" placeholder=\"Node IP\"\n                           class=\"form-control\" [(ngModel)]=\"node.ip\" #nodeIp=\"ngModel\">\n                </div>\n                \n\n                <div class=\"form-group text-center\">\n\n                    <button type=\"submit\" [disabled]='progressing' class=\"btn btn-fill btn-success\" i18n=\"@@newNodeSaveButton\" (click)=\"updateNode()\">\n                        <i class=\"fa fa-spinner fa-pulse fa-1x fa-fw\" *ngIf=\"progressing\" aria-hidden=\"true\"></i>Save</button>\n\n                    <button type=\"button\" class=\"btn btn-simple\" i18n=\"@@newNodeCancelButton\" (click)=\"cancel()\">Cancel</button>\n                </div>\n\n            </div>\n\n        </form>\n    </td>\n</div>\n\n"

/***/ })

});
//# sourceMappingURL=cluster.module.chunk.js.map