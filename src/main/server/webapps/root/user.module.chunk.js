webpackJsonp(["user.module"],{

/***/ "../../../../../src/app/userpage/equal-validator.directive.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return EqualValidator; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms__ = __webpack_require__("../../../forms/esm5/forms.js");
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


var EqualValidator = /** @class */ (function () {
    function EqualValidator(validateEqual, reverse) {
        this.validateEqual = validateEqual;
        this.reverse = reverse;
    }
    EqualValidator_1 = EqualValidator;
    Object.defineProperty(EqualValidator.prototype, "isReverse", {
        get: function () {
            if (!this.reverse)
                return false;
            return this.reverse === 'true' ? true : false;
        },
        enumerable: true,
        configurable: true
    });
    EqualValidator.prototype.validate = function (c) {
        // self value
        var v = c.value;
        // control vlaue
        var e = c.root.get(this.validateEqual);
        // value not equal
        if (e && v !== e.value && !this.isReverse) {
            return {
                validateEqual: false
            };
        }
        // value equal and reverse
        if (e && v === e.value && this.isReverse) {
            delete e.errors['validateEqual'];
            if (!Object.keys(e.errors).length)
                e.setErrors(null);
        }
        // value not equal and reverse
        if (e && v !== e.value && this.isReverse) {
            e.setErrors({
                validateEqual: false
            });
        }
        return null;
    };
    EqualValidator = EqualValidator_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["u" /* Directive */])({
            selector: '[validateEqual][formControlName],[validateEqual][formControl],[validateEqual][ngModel]',
            providers: [
                { provide: __WEBPACK_IMPORTED_MODULE_1__angular_forms__["d" /* NG_VALIDATORS */], useExisting: Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_19" /* forwardRef */])(function () { return EqualValidator_1; }), multi: true }
            ]
        }),
        __param(0, Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["h" /* Attribute */])('validateEqual')),
        __param(1, Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["h" /* Attribute */])('reverse')),
        __metadata("design:paramtypes", [String, String])
    ], EqualValidator);
    return EqualValidator;
    var EqualValidator_1;
}());



/***/ }),

/***/ "../../../../../src/app/userpage/user.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"main-content\">\n    <div class=\"container-fluid\">\n        <div class=\"row \">\n\n            <div class=\"col-lg-offset-3 col-lg-6\">\n                <div class=\"card\">\n                    <div class=\"card-header\">\n                        <h4 class=\"card-title\" i18n=\"@@change_password_title\">Change Password</h4>\n                    </div>\n                    <div class=\"card-content\">\n                        <form method=\"post\" #f=\"ngForm\" novalidate (ngSubmit)=\"updatePassword(f.valid, f)\">\n                            <div class=\"row\">\n\n                                <div class=\"col-md-12\">\n                                    <div class=\"form-group\">\n                                        <label><ng-container i18n=\"@@current_password\">Current Password</ng-container>\n                                            <span class=\"star\">*</span>\n                                        </label>\n\n                                        <input required name=\"currentPassword\" type=\"password\" class=\"form-control border-input\" [(ngModel)]=\"currentPassword\" #currentPass=\"ngModel\"\n                                        />\n\n                                        <small [hidden]=\"currentPass.valid || (currentPass.pristine && !f.submitted)\" class=\"text-danger\">\n                                            Current password is required.\n                                        </small>\n\n                                    </div>\n                                </div>\n\n                                <div class=\"col-md-12\">\n                                    <div class=\"form-group\">\n                                        <label><ng-container i18n=\"@@new_password\">New Password</ng-container>\n                                            <span class=\"star\">*</span>\n                                        </label>\n                                        <input required name=\"newPassword\" type=\"password\" class=\"form-control border-input\"\n                                             [(ngModel)]=\"newPassword\"  reverse=\"true\" #newPass=\"ngModel\"\n                                            validateEqual=\"newPasswordAgain\" minlength=\"7\">\n                                        <small i18n=\"@@passwordRequired\" [hidden]=\"newPass.valid || (newPass.pristine && !f.submitted)\" class=\"text-danger\">\n                                                Password should be at least 7 characters\n                                        </small>\n                                    </div>\n                                </div>\n                                <div class=\"col-md-12\">\n                                    <div class=\"form-group\">\n                                        <label for=\"exampleInputEmail1\" i18n=\"@@confirmNewPassword\">Confirm New Password\n                                            <span class=\"star\">*</span>\n                                        </label>\n                                        <input required name=\"newPasswordAgain\" type=\"password\" class=\"form-control border-input\"\n                                             [(ngModel)]=\"newPasswordAgain\" reverse=\"false\" #newPassAgain=\"ngModel\"\n                                            validateEqual=\"newPassword\">\n                                        <small i18n=\"@@password_mismatch\" [hidden]=\"newPassAgain.valid || (newPassAgain.pristine && !f.submitted)\" class=\"text-danger\">\n                                            Password mismatch\n                                        </small>\n                                    </div>\n                                </div>\n\n                                <div class=\"col-md-12 form-group text-center text-danger\" i18n=\"@@makeSureCurrentPasswordIsCorrect\" [hidden]=\"!showPasswordNotChangedError\">\n                                    Make sure you that current password is entered correctly.\n                                </div>\n                                <div class=\"col-md-12 form-group text-center text-success\" [hidden]=\"!showYourPasswordChanged\" i18n=\"@@yourPasswordChanged\" [hidden]=\"!showPasswordNotChangedError\">\n                                    Your password has beed changed\n                                 </div>\n                            </div>\n                            <div class=\"text-center\">\n                                <button type=\"submit\" class=\"btn btn-info btn-fill btn-wd\" i18n=\"@@update_button_label\">Update</button>\n                            </div>\n                            <div class=\"clearfix\"></div>\n                            \n                        </form>\n                    </div>\n                </div>\n            </div>\n        </div>\n    </div>\n</div>"

/***/ }),

/***/ "../../../../../src/app/userpage/user.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return UserComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__rest_auth_service__ = __webpack_require__("../../../../../src/app/rest/auth.service.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var UserComponent = /** @class */ (function () {
    function UserComponent(auth) {
        this.auth = auth;
        this.showPasswordNotChangedError = false;
        this.showYourPasswordChanged = false;
    }
    UserComponent.prototype.updatePassword = function (isValid, form) {
        var _this = this;
        if (!isValid) {
            return;
        }
        this.auth.changeUserPassword("nope", this.currentPassword, this.newPasswordAgain)
            .subscribe(function (data) {
            console.log(data);
            if (data["success"]) {
                form.resetForm();
                _this.showYourPasswordChanged = true;
                _this.showPasswordNotChangedError = false;
            }
            else {
                _this.showPasswordNotChangedError = true;
            }
        });
    };
    UserComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'user-cmp',
            template: __webpack_require__("../../../../../src/app/userpage/user.component.html")
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__rest_auth_service__["a" /* AuthService */]])
    ], UserComponent);
    return UserComponent;
}());



/***/ }),

/***/ "../../../../../src/app/userpage/user.module.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "UserModule", function() { return UserModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__("../../../router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__("../../../common/esm5/common.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_forms__ = __webpack_require__("../../../forms/esm5/forms.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__equal_validator_directive__ = __webpack_require__("../../../../../src/app/userpage/equal-validator.directive.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__user_component__ = __webpack_require__("../../../../../src/app/userpage/user.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__user_routing__ = __webpack_require__("../../../../../src/app/userpage/user.routing.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};







var UserModule = /** @class */ (function () {
    function UserModule() {
    }
    UserModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["L" /* NgModule */])({
            imports: [
                __WEBPACK_IMPORTED_MODULE_2__angular_common__["b" /* CommonModule */],
                __WEBPACK_IMPORTED_MODULE_1__angular_router__["d" /* RouterModule */].forChild(__WEBPACK_IMPORTED_MODULE_6__user_routing__["a" /* UserRoutes */]),
                __WEBPACK_IMPORTED_MODULE_3__angular_forms__["c" /* FormsModule */]
            ],
            declarations: [__WEBPACK_IMPORTED_MODULE_5__user_component__["a" /* UserComponent */], __WEBPACK_IMPORTED_MODULE_4__equal_validator_directive__["a" /* EqualValidator */]]
        })
    ], UserModule);
    return UserModule;
}());



/***/ }),

/***/ "../../../../../src/app/userpage/user.routing.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return UserRoutes; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__user_component__ = __webpack_require__("../../../../../src/app/userpage/user.component.ts");

var UserRoutes = [{
        path: '',
        children: [{
                path: 'pages/changepass',
                component: __WEBPACK_IMPORTED_MODULE_0__user_component__["a" /* UserComponent */]
            }]
    }];


/***/ })

});
//# sourceMappingURL=user.module.chunk.js.map