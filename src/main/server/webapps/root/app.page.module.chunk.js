webpackJsonp(["app.page.module"],{

/***/ "./src/app/app.page/app.page.module.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "AppPageRoutes", function() { return AppPageRoutes; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "AppPageModule", function() { return AppPageModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__("./node_modules/@angular/router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__("./node_modules/@angular/common/esm5/common.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_forms__ = __webpack_require__("./node_modules/@angular/forms/esm5/forms.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__angular_common_http__ = __webpack_require__("./node_modules/@angular/common/esm5/http.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__app_page_component__ = __webpack_require__("./src/app/app.page/app.page.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_ngx_clipboard__ = __webpack_require__("./node_modules/ngx-clipboard/dist/ngx-clipboard.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__angular_material__ = __webpack_require__("./node_modules/@angular/material/esm5/material.es5.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__dialog_detected_objects_list__ = __webpack_require__("./src/app/app.page/dialog/detected.objects.list.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};









var AppPageRoutes = [
    {
        path: '',
        component: __WEBPACK_IMPORTED_MODULE_5__app_page_component__["a" /* AppPageComponent */],
        pathMatch: 'full',
    },
    {
        path: ':appname',
        component: __WEBPACK_IMPORTED_MODULE_5__app_page_component__["a" /* AppPageComponent */],
    }
];
var AppPageModule = /** @class */ (function () {
    function AppPageModule() {
    }
    AppPageModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["L" /* NgModule */])({
            declarations: [__WEBPACK_IMPORTED_MODULE_5__app_page_component__["a" /* AppPageComponent */], __WEBPACK_IMPORTED_MODULE_5__app_page_component__["c" /* CamSettinsDialogComponent */], __WEBPACK_IMPORTED_MODULE_5__app_page_component__["e" /* UploadVodDialogComponent */], __WEBPACK_IMPORTED_MODULE_5__app_page_component__["b" /* BroadcastEditComponent */],
                __WEBPACK_IMPORTED_MODULE_8__dialog_detected_objects_list__["a" /* DetectedObjectListDialog */], __WEBPACK_IMPORTED_MODULE_5__app_page_component__["d" /* StreamSourceEditComponent */]],
            entryComponents: [
                __WEBPACK_IMPORTED_MODULE_5__app_page_component__["c" /* CamSettinsDialogComponent */], __WEBPACK_IMPORTED_MODULE_5__app_page_component__["a" /* AppPageComponent */], __WEBPACK_IMPORTED_MODULE_5__app_page_component__["e" /* UploadVodDialogComponent */], __WEBPACK_IMPORTED_MODULE_5__app_page_component__["b" /* BroadcastEditComponent */],
                __WEBPACK_IMPORTED_MODULE_8__dialog_detected_objects_list__["a" /* DetectedObjectListDialog */], __WEBPACK_IMPORTED_MODULE_5__app_page_component__["d" /* StreamSourceEditComponent */]
            ],
            bootstrap: [__WEBPACK_IMPORTED_MODULE_5__app_page_component__["a" /* AppPageComponent */]],
            imports: [
                __WEBPACK_IMPORTED_MODULE_2__angular_common__["b" /* CommonModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["n" /* MatFormFieldModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["q" /* MatInputModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["f" /* MatCheckboxModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["j" /* MatDialogModule */],
                __WEBPACK_IMPORTED_MODULE_1__angular_router__["d" /* RouterModule */].forChild(AppPageRoutes),
                __WEBPACK_IMPORTED_MODULE_3__angular_forms__["c" /* FormsModule */],
                __WEBPACK_IMPORTED_MODULE_4__angular_common_http__["c" /* HttpClientModule */],
                __WEBPACK_IMPORTED_MODULE_6_ngx_clipboard__["a" /* ClipboardModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["j" /* MatDialogModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["f" /* MatCheckboxModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["q" /* MatInputModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["b" /* MatAutocompleteModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["c" /* MatButtonModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["d" /* MatButtonToggleModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["e" /* MatCardModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["g" /* MatChipsModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["h" /* MatDatepickerModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["l" /* MatDividerModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["m" /* MatExpansionModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["o" /* MatGridListModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["p" /* MatIconModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["r" /* MatListModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["s" /* MatMenuModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["t" /* MatNativeDateModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["v" /* MatPaginatorModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["w" /* MatProgressBarModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["x" /* MatProgressSpinnerModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["y" /* MatRadioModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["z" /* MatRippleModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["A" /* MatSelectModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["B" /* MatSidenavModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["D" /* MatSliderModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["C" /* MatSlideToggleModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["E" /* MatSnackBarModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["G" /* MatSortModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["H" /* MatStepperModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["J" /* MatTableModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["K" /* MatTabsModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["L" /* MatToolbarModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["M" /* MatTooltipModule */]
            ],
            exports: [__WEBPACK_IMPORTED_MODULE_7__angular_material__["f" /* MatCheckboxModule */], __WEBPACK_IMPORTED_MODULE_7__angular_material__["j" /* MatDialogModule */], __WEBPACK_IMPORTED_MODULE_7__angular_material__["n" /* MatFormFieldModule */], __WEBPACK_IMPORTED_MODULE_7__angular_material__["q" /* MatInputModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["j" /* MatDialogModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["f" /* MatCheckboxModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["n" /* MatFormFieldModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["q" /* MatInputModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["b" /* MatAutocompleteModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["c" /* MatButtonModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["d" /* MatButtonToggleModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["e" /* MatCardModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["g" /* MatChipsModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["h" /* MatDatepickerModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["l" /* MatDividerModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["m" /* MatExpansionModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["o" /* MatGridListModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["p" /* MatIconModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["r" /* MatListModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["s" /* MatMenuModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["t" /* MatNativeDateModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["v" /* MatPaginatorModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["w" /* MatProgressBarModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["x" /* MatProgressSpinnerModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["y" /* MatRadioModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["z" /* MatRippleModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["A" /* MatSelectModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["B" /* MatSidenavModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["D" /* MatSliderModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["C" /* MatSlideToggleModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["E" /* MatSnackBarModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["G" /* MatSortModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["H" /* MatStepperModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["J" /* MatTableModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["K" /* MatTabsModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["L" /* MatToolbarModule */],
                __WEBPACK_IMPORTED_MODULE_7__angular_material__["M" /* MatTooltipModule */]],
        })
    ], AppPageModule);
    return AppPageModule;
}());



/***/ })

});
//# sourceMappingURL=app.page.module.chunk.js.map