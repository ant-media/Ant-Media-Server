webpackJsonp(["pages.module"],{

/***/ "./src/app/pages/equal-validator.directive.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return EqualValidator; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms__ = __webpack_require__("./node_modules/@angular/forms/esm5/forms.js");
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

/***/ "./src/app/pages/lock/lock.component.html":
/***/ (function(module, exports) {

module.exports = "<nav class=\"navbar navbar-transparent navbar-absolute\">\n    <div class=\"container\">\n        <div class=\"navbar-header\">\n            <button type=\"button\" class=\"navbar-toggle\" data-toggle=\"collapse\" data-target=\"#navigation-example-2\" (click)=\"sidebarToggle()\">\n                <span class=\"sr-only\">Toggle navigation</span>\n                <span class=\"icon-bar\"></span>\n                <span class=\"icon-bar\"></span>\n                <span class=\"icon-bar\"></span>\n            </button>\n                <a class=\"navbar-brand\" routerLinkActive=\"active\" [routerLink]=\"['/dashboard/overview']\">Paper Dashboard PRO</a>\n        </div>\n        <div class=\"collapse navbar-collapse\">\n            <ul class=\"nav navbar-nav navbar-right\">\n\n            </ul>\n        </div>\n    </div>\n</nav>\n<div class=\"wrapper wrapper-full-page\">\n    <div class=\"full-page lock-page\" data-color=\"green\" data-image=\"../../assets/img/background/background-5.png\">\n    <!--   you can change the color of the filter page using: data-color=\"blue | azure | green | orange | red | purple\" -->\n        <div class=\"content\">\n            <form method=\"#\" action=\"#\">\n                <div class=\"card card-lock\">\n                    <div class=\"author\">\n                        <img class=\"avatar\" src=\"../../assets/img/faces/face-2.jpg\" alt=\"...\">\n                    </div>\n                    <h4>Chet Faker</h4>\n                    <div class=\"form-group\">\n                        <input type=\"password\" placeholder=\"Enter Password\" class=\"form-control\">\n                    </div>\n                    <button type=\"button\" class=\"btn btn-wd\">Unlock</button>\n                </div>\n            </form>\n        </div>\n        <footer class=\"footer footer-transparent\">\n            <div class=\"container\">\n                <div class=\"copyright\">\n                    &copy; {{test | date: 'yyyy'}}, made with <i class=\"fa fa-heart heart\"></i> by <a href=\"https://www.creative-tim.com\">Creative Tim</a>\n                </div>\n            </div>\n        </footer>\n    </div>\n</div>\n"

/***/ }),

/***/ "./src/app/pages/lock/lock.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return LockComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};

var LockComponent = /** @class */ (function () {
    function LockComponent(element) {
        this.element = element;
        this.test = new Date();
        this.nativeElement = element.nativeElement;
        this.sidebarVisible = false;
    }
    LockComponent.prototype.checkFullPageBackgroundImage = function () {
        var $page = $('.full-page');
        var image_src = $page.data('image');
        if (image_src !== undefined) {
            var image_container = '<div class="full-page-background" style="background-image: url(' + image_src + ') "/>';
            $page.append(image_container);
        }
    };
    ;
    LockComponent.prototype.ngOnInit = function () {
        this.checkFullPageBackgroundImage();
        var navbar = this.element.nativeElement;
        this.toggleButton = navbar.getElementsByClassName('navbar-toggle')[0];
        setTimeout(function () {
            // after 1000 ms we add the class animated to the login/register card
            $('.card').removeClass('card-hidden');
        }, 700);
    };
    LockComponent.prototype.sidebarToggle = function () {
        var toggleButton = this.toggleButton;
        var body = document.getElementsByTagName('body')[0];
        var sidebar = document.getElementsByClassName('navbar-collapse')[0];
        if (this.sidebarVisible == false) {
            setTimeout(function () {
                toggleButton.classList.add('toggled');
            }, 500);
            body.classList.add('nav-open');
            this.sidebarVisible = true;
        }
        else {
            this.toggleButton.classList.remove('toggled');
            this.sidebarVisible = false;
            body.classList.remove('nav-open');
        }
    };
    LockComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'lock-cmp',
            template: __webpack_require__("./src/app/pages/lock/lock.component.html")
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["v" /* ElementRef */]])
    ], LockComponent);
    return LockComponent;
}());



/***/ }),

/***/ "./src/app/pages/login/login.component.html":
/***/ (function(module, exports) {

module.exports = "<nav class=\"navbar navbar-transparent navbar-absolute\">\n    <div class=\"container\">\n        <div class=\"navbar-header\">\n            <button type=\"button\" class=\"navbar-toggle\" data-toggle=\"collapse\" data-target=\"#navigation-example-2\" (click)=\"sidebarToggle()\">\n                <span class=\"sr-only\">Toggle navigation</span>\n                <span class=\"icon-bar\"></span>\n                <span class=\"icon-bar\"></span>\n                <span class=\"icon-bar\"></span>\n            </button>\n            <a class=\"navbar-brand\" i18n=\"@@loginPageHeader\" routerLinkActive = \"active\" [routerLink]=\"['/dashboard/overview']\">Manager of Ant Media Server</a>\n        </div>\n        <!--\n        <div class=\"collapse navbar-collapse\">\n            <ul class=\"nav navbar-nav navbar-left\">\n                <li>\n                    <a routerLinkActive = \"active\" [routerLink]=\"['/pages/register']\">\n                        Register\n                    </a>\n                </li>\n              \n            </ul>\n        </div>\n    -->\n    \n    </div>\n</nav>\n\n<div class=\"wrapper wrapper-full-page\">\n    <div class=\"full-page login-page\" data-color=\"\" data-image=\"../../assets/img/background/background-2.jpg\">\n    <!--   you can change the color of the filter page using: data-color=\"blue | azure | green | orange | red | purple\" -->\n        <div class=\"content\">\n            <div class=\"container\">\n                <div class=\"row\">\n                    <div class=\"col-md-4 col-sm-6 col-md-offset-4 col-sm-offset-3\" *ngIf=\"!firstLogin\">\n                        <form (ngSubmit)=\"loginUser()\">\n                            <div class=\"card\" data-background=\"color\" data-color=\"blue\">\n                                <div class=\"card-header\">\n                                    <h3 class=\"card-title\" i18n=\"@@loginTitle\">Login</h3>\n                                </div>\n                                <div class=\"card-content\">\n                                    <div class=\"form-group text-success text-center\" [hidden]=\"!showYouCanLogin\"\n                                    i18n=\"@@login_success_message\" >\n                                            You can now login with your username and password\n                                    </div>\n\n                                    <div class=\"form-group\">\n                                        <label i18n=\"@@loginFormEmail\">Username</label>\n                                        <input (keydown)=\"credentialsChanged()\" type=\"email\" name=\"email\" i18n-placeholder=\"@@email_place_holder\" placeholder=\"Username\" class=\"form-control input-no-border\" [(ngModel)]=\"email\">\n\n                                    </div>\n                                    <div class=\"form-group\">\n                                        <label i18n=\"@@loginFormPassword\">Password</label>\n                                        <input (keydown)=\"credentialsChanged()\" type=\"password\" name=\"password\" i18n-placeholder=\"@@password_place_holder\" placeholder=\"Password\" class=\"form-control input-no-border\" [(ngModel)]=\"password\">\n                                    </div>\n                                    <div class=\"form-group text-danger text-center\" [hidden]=\"!showIncorrectCredentials\"\n                                    i18n=\"@@loginFormIncorrectCredentials\" >\n                                            Username or password is incorrect\n                                        </div>\n                                </div>\n                                <div class=\"card-footer text-center\">\n                                    <button type=\"submit\"  i18n=\"@@loginFormSubmit\" class=\"btn btn-fill btn-wd \">Let's go</button>\n                                    <!--\n                                    <div class=\"forgot\">\n                                        <a href=\"#pablo\" i18n=\"@@loginFormForgotPassword\">Forgot your password?</a>\n                                    </div>\n                                -->\n                                </div>\n                            </div>\n                        </form>\n                    </div>\n                    \n                    <div class=\"col-md-4 col-sm-6 col-md-offset-4 col-sm-offset-3\" *ngIf=\"firstLogin\">\n                            <form #registerForm=\"ngForm\" novalidate (ngSubmit)=\"createFirstAccount(registerForm.valid)\">\n                                    <div class=\"card\" data-background=\"color\" data-color=\"blue\">\n                                        <div class=\"card-header\">\n                                            <h3 class=\"card-title\" i18n=\"@@loginRegisterFirtUser\">Create First Account</h3>\n                                        </div>\n                                        <div class=\"card-content\">\n\n                                            <div class=\"form-group text-danger text-center\" [hidden]=\"!showFailedToCreateUserAccount\"\n                                            i18n=\"@@failed_to_create_user_account\" >\n                                            Failed to create user account. There can be already an user\n                                            </div>\n\n                                            <div class=\"form-group\">\n                                                <label i18n=\"@@registerUserFullName\">Full Name<span class=\"star\">*</span></label>\n                                                <input type=\"text\" name=\"fullname\" class=\"form-control input-no-border\"\n                                                   [(ngModel)]=\"firstUser.fullName\" #fullName=\"ngModel\" required minlength=\"6\">\n                                                <small i18n=\"@@fullNameIsRequired\" [hidden]=\"fullName.valid || (fullName.pristine && !registerForm.submitted)\" class=\"text-danger\">\n                                                Full name is required and it should be at least 6 characters\n                                                </small>\n                                             </div>\n                                           \n                                            <div class=\"form-group\">\n                                                <label i18n=\"@@loginFormEmail\">Username<span class=\"star\">*</span></label>\n                                                <input  type=\"email\" name=\"firstEmail\" class=\"form-control input-no-border\" \n                                                    [(ngModel)]=\"firstUser.email\" required #firstAccountEmail=\"ngModel\" minlength=\"5\">\n                                                    <small i18n=\"@@fistAccountEmailRequired\" [hidden]=\"firstAccountEmail.valid || (firstAccountEmail.pristine && !registerForm.submitted)\" class=\"text-danger\">\n                                                    Username should be at least 5 characters\n                                                    </small>\n                                            </div>\n                                            <div class=\"form-group\">\n                                                <label i18n=\"@@loginFormPassword\">Password<span class=\"star\">*</span></label>\n                                                <input type=\"password\" name=\"first_password\" \n                                                    class=\"form-control input-no-border\" [(ngModel)]=\"firstUser.password\"\n                                                    required #firstPassword=\"ngModel\" minlength=\"7\" reverse=\"true\" validateEqual=\"first_password_again\">\n                                                    <small i18n=\"@@passwordRequired\" [hidden]=\"firstPassword.valid || (firstPassword.pristine && !registerForm.submitted)\" class=\"text-danger\">\n                                                    Password should be at least 7 characters\n                                                    </small>\n                                            </div>\n                                            <div class=\"form-group\">\n                                                    <label i18n=\"@@loginFormPasswordAgain\">Confirm Password<span class=\"star\">*</span></label>\n                                                    <input type=\"password\" name=\"first_password_again\" class=\"form-control input-no-border\" \n                                                        [(ngModel)]=\"temp_model_password\" #firstPasswordAgain=\"ngModel\" required reverse=\"false\" validateEqual=\"first_password\">\n                                                        <small i18n=\"@@password_mismatch\" [hidden]=\"firstPasswordAgain.valid || (firstPasswordAgain.pristine && !registerForm.submitted)\" class=\"text-danger\">\n                                                        Password mismatch\n                                                        </small>\n                                                </div>\n                                        </div>\n                                        <div class=\"card-footer text-center\">\n                                            <button type=\"submit\" [disabled]='firstUserIsCreating' \n                                                  class=\"btn btn-fill btn-wd \">\n                                                 <i class=\"fa fa-spinner fa-pulse fa-1x fa-fw\" *ngIf=\"firstUserIsCreating\" aria-hidden=\"true\"></i>\n                                                 <ng-container i18n=\"@@createAccount\">Create Account</ng-container></button>\n                                           \n                                        </div>\n                                    </div>\n                                </form>\n                    </div>\n\n                </div>\n            </div>\n        </div>\n\n        <footer class=\"footer footer-transparent\">\n            <div class=\"container\">\n                <div class=\"copyright\">\n                    &copy; {{test | date: 'yyyy'}}  <i class=\"fa fa-heart heart\"></i> by <a href=\"http://antmedia.io\">AntMedia</a>\n                </div>\n            </div>\n        </footer>\n    </div>\n</div>\n"

/***/ }),

/***/ "./src/app/pages/login/login.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return LoginComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__("./node_modules/@angular/router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__rest_auth_service__ = __webpack_require__("./src/app/rest/auth.service.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__rest_rest_service__ = __webpack_require__("./src/app/rest/rest.service.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__angular_common_http__ = __webpack_require__("./node_modules/@angular/common/esm5/http.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};





var LoginComponent = /** @class */ (function () {
    function LoginComponent(authService, element, auth, http, router) {
        this.authService = authService;
        this.element = element;
        this.auth = auth;
        this.http = http;
        this.router = router;
        this.test = new Date();
        this.email = "";
        this.password = "";
        this.showIncorrectCredentials = false;
        this.firstLogin = false;
        this.nativeElement = element.nativeElement;
        this.sidebarVisible = false;
        this.showYouCanLogin = false;
        this.showFailedToCreateUserAccount = false;
    }
    LoginComponent.prototype.checkFullPageBackgroundImage = function () {
        var $page = $('.full-page');
        var image_src = $page.data('image');
        if (image_src !== undefined) {
            var image_container = '<div class="full-page-background" style="background-image: url(' + image_src + ') "/>';
            $page.append(image_container);
        }
    };
    ;
    LoginComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.auth.isFirstLogin().subscribe(function (data) {
            _this.firstLogin = data["success"];
            if (_this.firstLogin) {
                _this.firstUser = new __WEBPACK_IMPORTED_MODULE_3__rest_rest_service__["f" /* User */]("", "");
            }
        });
        this.checkFullPageBackgroundImage();
        this.logout();
        var navbar = this.element.nativeElement;
        this.toggleButton = navbar.getElementsByClassName('navbar-toggle')[0];
        setTimeout(function () {
            // after 1000 ms we add the class animated to the login/register card
            $('.card').removeClass('card-hidden');
        }, 700);
    };
    LoginComponent.prototype.logout = function () {
        // localStorage.setItem("authenticated", null);
        localStorage.clear();
        //this.router.navigateByUrl("/pages/login");
    };
    LoginComponent.prototype.sidebarToggle = function () {
        var toggleButton = this.toggleButton;
        var body = document.getElementsByTagName('body')[0];
        var sidebar = document.getElementsByClassName('navbar-collapse')[0];
        if (this.sidebarVisible == false) {
            setTimeout(function () {
                toggleButton.classList.add('toggled');
            }, 500);
            body.classList.add('nav-open');
            this.sidebarVisible = true;
        }
        else {
            this.toggleButton.classList.remove('toggled');
            this.sidebarVisible = false;
            body.classList.remove('nav-open');
        }
    };
    LoginComponent.prototype.loginUser = function () {
        var _this = this;
        this.auth.login(this.email, this.password).subscribe(function (data) {
            if (data["success"] == true) {
                _this.authService.isAuthenticated = data["success"];
                localStorage.setItem("authenticated", "true");
                _this.router.navigateByUrl("/dashboard");
            }
            else {
                _this.showIncorrectCredentials = true;
            }
        });
    };
    LoginComponent.prototype.createFirstAccount = function (isValid) {
        var _this = this;
        console.log("is first account");
        if (!isValid) {
            return;
        }
        this.firstUserIsCreating = true;
        this.auth.createFirstAccount(this.firstUser).subscribe(function (data) {
            _this.firstUserIsCreating = false;
            if (data["success"] == true) {
                _this.firstLogin = false;
                _this.showYouCanLogin = true;
            }
            else {
                _this.showFailedToCreateUserAccount = true;
            }
        });
    };
    LoginComponent.prototype.credentialsChanged = function () {
        this.showIncorrectCredentials = false;
    };
    LoginComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'login-cmp',
            template: __webpack_require__("./src/app/pages/login/login.component.html")
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_2__rest_auth_service__["a" /* AuthService */], __WEBPACK_IMPORTED_MODULE_0__angular_core__["v" /* ElementRef */], __WEBPACK_IMPORTED_MODULE_2__rest_auth_service__["a" /* AuthService */], __WEBPACK_IMPORTED_MODULE_4__angular_common_http__["b" /* HttpClient */], __WEBPACK_IMPORTED_MODULE_1__angular_router__["c" /* Router */]])
    ], LoginComponent);
    return LoginComponent;
}());



/***/ }),

/***/ "./src/app/pages/pages.module.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "PagesModule", function() { return PagesModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__("./node_modules/@angular/router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__("./node_modules/@angular/common/esm5/common.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_forms__ = __webpack_require__("./node_modules/@angular/forms/esm5/forms.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__pages_routing__ = __webpack_require__("./src/app/pages/pages.routing.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__register_register_component__ = __webpack_require__("./src/app/pages/register/register.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__lock_lock_component__ = __webpack_require__("./src/app/pages/lock/lock.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__login_login_component__ = __webpack_require__("./src/app/pages/login/login.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__equal_validator_directive__ = __webpack_require__("./src/app/pages/equal-validator.directive.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__angular_common_http__ = __webpack_require__("./node_modules/@angular/common/esm5/http.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10__rest_rest_service__ = __webpack_require__("./src/app/rest/rest.service.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};











var PagesModule = /** @class */ (function () {
    function PagesModule() {
    }
    PagesModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["L" /* NgModule */])({
            imports: [
                __WEBPACK_IMPORTED_MODULE_2__angular_common__["b" /* CommonModule */],
                __WEBPACK_IMPORTED_MODULE_1__angular_router__["d" /* RouterModule */].forChild(__WEBPACK_IMPORTED_MODULE_4__pages_routing__["a" /* PagesRoutes */]),
                __WEBPACK_IMPORTED_MODULE_3__angular_forms__["c" /* FormsModule */],
                __WEBPACK_IMPORTED_MODULE_3__angular_forms__["h" /* ReactiveFormsModule */]
            ],
            declarations: [
                __WEBPACK_IMPORTED_MODULE_7__login_login_component__["a" /* LoginComponent */],
                __WEBPACK_IMPORTED_MODULE_5__register_register_component__["a" /* RegisterComponent */],
                __WEBPACK_IMPORTED_MODULE_6__lock_lock_component__["a" /* LockComponent */],
                __WEBPACK_IMPORTED_MODULE_8__equal_validator_directive__["a" /* EqualValidator */]
            ],
            providers: [{
                    provide: __WEBPACK_IMPORTED_MODULE_9__angular_common_http__["a" /* HTTP_INTERCEPTORS */],
                    useClass: __WEBPACK_IMPORTED_MODULE_10__rest_rest_service__["a" /* AuthInterceptor */],
                    multi: true
                }]
        })
    ], PagesModule);
    return PagesModule;
}());



/***/ }),

/***/ "./src/app/pages/pages.routing.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return PagesRoutes; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__register_register_component__ = __webpack_require__("./src/app/pages/register/register.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__lock_lock_component__ = __webpack_require__("./src/app/pages/lock/lock.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__login_login_component__ = __webpack_require__("./src/app/pages/login/login.component.ts");



var PagesRoutes = [{
        path: '',
        children: [{
                path: 'login',
                component: __WEBPACK_IMPORTED_MODULE_2__login_login_component__["a" /* LoginComponent */]
            }, {
                path: 'lock',
                component: __WEBPACK_IMPORTED_MODULE_1__lock_lock_component__["a" /* LockComponent */]
            }, {
                path: 'register',
                component: __WEBPACK_IMPORTED_MODULE_0__register_register_component__["a" /* RegisterComponent */]
            }
        ]
    }];


/***/ }),

/***/ "./src/app/pages/register/register.component.html":
/***/ (function(module, exports) {

module.exports = "<nav class=\"navbar navbar-transparent navbar-absolute\">\n    <div class=\"container\">\n        <div class=\"navbar-header\">\n            <button type=\"button\" class=\"navbar-toggle navbar-toggle-black\" data-toggle=\"collapse\" data-target=\"#navigation-example-2\" (click)=\"sidebarToggle()\">\n                <span class=\"sr-only\">Toggle navigation</span>\n                <span class=\"icon-bar \"></span>\n                <span class=\"icon-bar \"></span>\n                <span class=\"icon-bar\"></span>\n            </button>\n        </div>\n        <div class=\"collapse navbar-collapse\">\n            <ul class=\"nav navbar-nav navbar-right\">\n                <li>\n                   <a routerLinkActive=\"active\" [routerLink]=\"['/pages/login']\" class=\"btn\">\n                        Looking to login?\n                    </a>\n                </li>\n            </ul>\n        </div>\n    </div>\n</nav>\n\n<div class=\"wrapper wrapper-full-page\">\n    <div class=\"register-page\">\n    <!--   you can change the color of the filter page using: data-color=\"blue | azure | green | orange | red | purple\" -->\n        <div class=\"content\">\n            <div class=\"container\">\n                <div class=\"row\">\n                    <div class=\"col-md-8 col-md-offset-2\">\n                        <div class=\"header-text\">\n                            <h2>Paper Dashboard PRO</h2>\n                            <h4>Register for free and experience the dashboard today.</h4>\n                            <hr>\n                        </div>\n                    </div>\n                    <div class=\"col-md-4 col-md-offset-2\">\n                        <div class=\"media\">\n                            <div class=\"media-left\">\n                                <div class=\"icon icon-danger\">\n                                    <i class=\"ti ti-user\"></i>\n                                </div>\n                            </div>\n                            <div class=\"media-body\">\n                                <h5>Free Account</h5>\n                                Here you can write a feature description for your dashboard, let the users know what is the value that you give them.\n                            </div>\n                        </div>\n                        <div class=\"media\">\n                            <div class=\"media-left\">\n                                <div class=\"icon icon-warning\">\n                                    <i class=\"ti-bar-chart-alt\"></i>\n                                </div>\n                            </div>\n                            <div class=\"media-body\">\n                                <h5>Awesome Performances</h5>\n                                Here you can write a feature description for your dashboard, let the users know what is the value that you give them.\n                            </div>\n                        </div>\n                        <div class=\"media\">\n                            <div class=\"media-left\">\n                                <div class=\"icon icon-info\">\n                                    <i class=\"ti-headphone\"></i>\n                                </div>\n                            </div>\n                            <div class=\"media-body\">\n                                <h5>Global Support</h5>\n                                Here you can write a feature description for your dashboard, let the users know what is the value that you give them.\n                            </div>\n                        </div>\n                    </div>\n                    <div class=\"col-md-4\">\n                        <form method=\"#\" action=\"#\">\n                            <div class=\"card card-plain\">\n                                <div class=\"content\">\n                                    <div class=\"form-group\">\n                                        <input type=\"email\" placeholder=\"Your First Name\" class=\"form-control\">\n                                    </div>\n                                    <div class=\"form-group\">\n                                        <input type=\"email\" placeholder=\"Your Last Name\" class=\"form-control\">\n                                    </div>\n                                    <div class=\"form-group\">\n                                        <input type=\"email\" placeholder=\"Company\" class=\"form-control\">\n                                    </div>\n                                    <div class=\"form-group\">\n                                        <input type=\"email\" placeholder=\"Enter email\" class=\"form-control\">\n                                    </div>\n                                    <div class=\"form-group\">\n                                        <input type=\"password\" placeholder=\"Password\" class=\"form-control\">\n                                    </div>\n                                    <div class=\"form-group\">\n                                        <input type=\"password\" placeholder=\"Password Confirmation\" class=\"form-control\">\n                                    </div>\n                                </div>\n                                <div class=\"footer text-center\">\n                                    <button type=\"submit\" class=\"btn btn-fill btn-danger btn-wd\">Create Free Account</button>\n                                </div>\n                            </div>\n                        </form>\n                    </div>\n                </div>\n            </div>\n        </div>\n\n        <footer class=\"footer footer-transparent\">\n            <div class=\"container\">\n                <div class=\"copyright text-center\">\n                    &copy; {{test | date: 'yyyy'}}, made with <i class=\"fa fa-heart heart\"></i> by <a href=\"https://www.creative-tim.com\">Creative Tim</a>\n                </div>\n            </div>\n        </footer>\n    </div>\n</div>\n"

/***/ }),

/***/ "./src/app/pages/register/register.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return RegisterComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};

var RegisterComponent = /** @class */ (function () {
    function RegisterComponent(element) {
        this.element = element;
        this.test = new Date();
        this.nativeElement = element.nativeElement;
        this.sidebarVisible = false;
    }
    RegisterComponent.prototype.checkFullPageBackgroundImage = function () {
        var $page = $('.full-page');
        var image_src = $page.data('image');
        if (image_src !== undefined) {
            var image_container = '<div class="full-page-background" style="background-image: url(' + image_src + ') "/>';
            $page.append(image_container);
        }
    };
    ;
    RegisterComponent.prototype.ngOnInit = function () {
        this.checkFullPageBackgroundImage();
        var navbar = this.element.nativeElement;
        this.toggleButton = navbar.getElementsByClassName('navbar-toggle')[0];
        setTimeout(function () {
            // after 1000 ms we add the class animated to the login/register card
            $('.card').removeClass('card-hidden');
        }, 700);
    };
    RegisterComponent.prototype.sidebarToggle = function () {
        var toggleButton = this.toggleButton;
        var body = document.getElementsByTagName('body')[0];
        var sidebar = document.getElementsByClassName('navbar-collapse')[0];
        if (this.sidebarVisible == false) {
            setTimeout(function () {
                toggleButton.classList.add('toggled');
            }, 500);
            body.classList.add('nav-open');
            this.sidebarVisible = true;
        }
        else {
            this.toggleButton.classList.remove('toggled');
            this.sidebarVisible = false;
            body.classList.remove('nav-open');
        }
    };
    RegisterComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'register-cmp',
            template: __webpack_require__("./src/app/pages/register/register.component.html")
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["v" /* ElementRef */]])
    ], RegisterComponent);
    return RegisterComponent;
}());



/***/ })

});
//# sourceMappingURL=pages.module.chunk.js.map