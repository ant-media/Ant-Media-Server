webpackJsonp(["tables.module"],{

/***/ "../../../../../src/app/tables/datatable.net/datatable.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"main-content\">\n    <div class=\"container-fluid\">\n        <div class=\"row\">\n            <div class=\"col-md-12\">\n                <h4 class=\"title\">DataTables.net</h4>\n                <p class=\"category\">A powerful jQuery plugin handcrafted by our friends from <a href=\"https://datatables.net/\" target=\"_blank\">dataTables.net</a>. It is a highly flexible tool, based upon the foundations of progressive enhancement and will add advanced interaction controls to any HTML table. Please check out the <a href=\"https://datatables.net/manual/index\" target=\"_blank\">full documentation.</a></p>\n\n                <br>\n                <div class=\"card\">\n                    <div class=\"card-content\">\n                        <h4 class=\"card-title\">DataTables.net</h4>\n                        <div class=\"toolbar\">\n                            <!--        Here you can write extra buttons/actions for the toolbar              -->\n                        </div>\n                        <div class=\"fresh-table\">\n                            <table id=\"datatables\" class=\"table table-striped table-no-bordered table-hover\" cellspacing=\"0\" width=\"100%\" style=\"width:100%\">\n                                <thead>\n                                    <tr>\n                                      <th>{{ dataTable.headerRow[0] }}</th>\n                                      <th>{{ dataTable.headerRow[1] }}</th>\n                                      <th>{{ dataTable.headerRow[2] }}</th>\n                                      <th>{{ dataTable.headerRow[3] }}</th>\n                                      <th>{{ dataTable.headerRow[4] }}</th>\n                                      <th class=\"disabled-sorting\">{{ dataTable.headerRow[5] }}</th>\n                                    </tr>\n                                </thead>\n                                <tfoot>\n                                    <tr>\n                                      <th>{{ dataTable.footerRow[0] }}</th>\n                                      <th>{{ dataTable.footerRow[1] }}</th>\n                                      <th>{{ dataTable.footerRow[2] }}</th>\n                                      <th>{{ dataTable.footerRow[3] }}</th>\n                                      <th>{{ dataTable.footerRow[4] }}</th>\n                                      <th>{{ dataTable.footerRow[5] }}</th>\n                                    </tr>\n                                </tfoot>\n                                <tbody>\n                                    <tr *ngFor=\"let row of dataTable.dataRows\">\n                                        <td>{{row[0]}}</td>\n                                        <td>{{row[1]}}</td>\n                                        <td>{{row[2]}}</td>\n                                        <td>{{row[3]}}</td>\n                                        <td>{{row[4]}}</td>\n                                        <td>\n                                            <button class=\"btn btn-simple btn-info btn-icon like\"><i class=\"ti-heart\"></i></button>\n                                            <button class=\"btn btn-simple btn-warning btn-icon edit\"><i class=\"ti-pencil-alt\"></i></button>\n                                            <button class=\"btn btn-simple btn-danger btn-icon remove\"><i class=\"ti-close\"></i></button>\n                                        </td>\n\n                                    </tr>\n                                </tbody>\n                            </table>\n                        </div>\n                    </div>\n                    <!-- end content-->\n                </div>\n                <!--  end card  -->\n            </div>\n            <!-- end col-md-12 -->\n        </div>\n        <!-- end row -->\n    </div>\n</div>\n"

/***/ }),

/***/ "../../../../../src/app/tables/datatable.net/datatable.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return DataTableComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var DataTableComponent = /** @class */ (function () {
    function DataTableComponent() {
    }
    DataTableComponent.prototype.ngOnInit = function () {
        this.dataTable = {
            headerRow: ['Name', 'Position', 'Office', 'Age', 'Date', 'Actions'],
            footerRow: ['Name', 'Position', 'Office', 'Age', 'Start Date', 'Actions'],
            dataRows: [
                ['Airi Satou', 'Andrew Mike', 'Develop', '2013', '99,225', ''],
                ['Angelica Ramos', 'John Doe', 'Design', '2012', '89,241', 'btn-round'],
                ['Ashton Cox', 'Alex Mike', 'Design', '2010', '92,144', 'btn-simple'],
                ['Bradley Greer', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Brenden Wagner', 'Paul Dickens', 'Communication', '2015', '69,201', ''],
                ['Brielle Williamson', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Caesar Vance', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Cedric Kelly', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Charde Marshall', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Colleen Hurst', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Dai Rios', 'Andrew Mike', 'Develop', '2013', '99,225', ''],
                ['Doris Wilder', 'John Doe', 'Design', '2012', '89,241', 'btn-round'],
                ['Fiona Green', 'Alex Mike', 'Design', '2010', '92,144', 'btn-simple'],
                ['Garrett Winters', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Gavin Cortez', 'Paul Dickens', 'Communication', '2015', '69,201', ''],
                ['Gavin Joyce', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Gloria Little', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Haley Kennedy', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Herrod Chandler', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Hope Fuentes', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Howard Hatfield', 'Andrew Mike', 'Develop', '2013', '99,225', ''],
                ['Jena Gaines', 'John Doe', 'Design', '2012', '89,241', 'btn-round'],
                ['Jenette Caldwell', 'Alex Mike', 'Design', '2010', '92,144', 'btn-simple'],
                ['Jennifer Chang', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Martena Mccray', 'Paul Dickens', 'Communication', '2015', '69,201', ''],
                ['Michael Silva', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Michelle House', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Paul Byrd', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Prescott Bartlett', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Quinn Flynn', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Rhona Davidson', 'Andrew Mike', 'Develop', '2013', '99,225', ''],
                ['Shou Itou', 'John Doe', 'Design', '2012', '89,241', 'btn-round'],
                ['Sonya Frost', 'Alex Mike', 'Design', '2010', '92,144', 'btn-simple'],
                ['Suki Burks', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Tatyana Fitzpatrick', 'Paul Dickens', 'Communication', '2015', '69,201', ''],
                ['Tiger Nixon', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Timothy Mooney', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Unity Butler', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Vivian Harrell', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round'],
                ['Yuri Berry', 'Mike Monday', 'Marketing', '2013', '49,990', 'btn-round']
            ]
        };
    };
    DataTableComponent.prototype.ngAfterViewInit = function () {
        $('#datatables').DataTable({
            "pagingType": "full_numbers",
            "lengthMenu": [[10, 25, 50, -1], [10, 25, 50, "All"]],
            responsive: true,
            language: {
                search: "_INPUT_",
                searchPlaceholder: "Search records",
            }
        });
        var table = $('#datatables').DataTable();
        // Edit record
        table.on('click', '.edit', function () {
            var $tr = $(this).closest('tr');
            var data = table.row($tr).data();
            alert('You press on Row: ' + data[0] + ' ' + data[1] + ' ' + data[2] + '\'s row.');
        });
        // Delete a record
        table.on('click', '.remove', function (e) {
            var $tr = $(this).closest('tr');
            table.row($tr).remove().draw();
            e.preventDefault();
        });
        //Like record
        table.on('click', '.like', function () {
            alert('You clicked on Like button');
        });
    };
    DataTableComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'data-table-cmp',
            template: __webpack_require__("../../../../../src/app/tables/datatable.net/datatable.component.html")
        })
    ], DataTableComponent);
    return DataTableComponent;
}());



/***/ }),

/***/ "../../../../../src/app/tables/extendedtable/extendedtable.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"main-content\">\n    <div class=\"container-fluid\">\n        <div class=\"row\">\n            <div class=\"col-md-12\">\n                <div class=\"card\">\n                    <div class=\"card-header\">\n                        <h4 class=\"card-title\">Table with Links</h4>\n                        <p class=\"category\">Here is a subtitle for this table</p>\n                    </div>\n                    <div class=\"card-content table-responsive table-full-width\">\n                        <table class=\"table\">\n                                <thead>\n                                    <tr>\n                                      <th class=\"text-center\">{{ tableData1.headerRow[0] }}</th>\n                                      <th>{{ tableData1.headerRow[1] }}</th>\n                                      <th>{{ tableData1.headerRow[2] }}</th>\n                                      <th class=\"text-right\">{{ tableData1.headerRow[4] }}</th>\n                                      <th class=\"text-right\">{{ tableData1.headerRow[5] }}</th>\n                                    </tr>\n                                </thead>\n                              <tbody>\n                                  <tr *ngFor=\"let row of tableData1.dataRows\">\n                                      <td class=\"text-center\">{{row[0]}}</td>\n                                      <td>{{row[1]}}</td>\n                                      <td>{{row[2]}}</td>\n                                      <td class=\"text-right\">&euro; {{row[4]}}</td>\n                                      <td class=\"td-actions text-right\">\n                                          <a rel=\"tooltip\" title=\"View Profile\" class=\"btn btn-info btn-simple btn-xs\">\n                                              <i class=\"ti-user\"></i>\n                                          </a>\n                                          <a rel=\"tooltip\" title=\"Edit Profile\" class=\"btn btn-success btn-simple btn-xs\">\n                                              <i class=\"ti-pencil-alt\"></i>\n                                          </a>\n                                          <a rel=\"tooltip\" title=\"Remove\" class=\"btn btn-danger btn-simple btn-xs\">\n                                              <i class=\"ti-close\"></i>\n                                          </a>\n                                      </td>\n                                  </tr>\n                              </tbody>\n                        </table>\n                    </div>\n                </div>\n            </div>\n            <div class=\"col-md-12\">\n                <div class=\"card\">\n                    <div class=\"card-header\">\n                        <h4 class=\"card-title\">Table with Switches</h4>\n                        <p class=\"category\">With some subtitle</p>\n                    </div>\n                    <div class=\"card-content table-full-width\">\n                        <table class=\"table table-striped\">\n                                <thead>\n                                    <tr>\n                                      <th class=\"text-center\">{{ tableData2.headerRow[0] }}</th>\n                                      <th>{{ tableData2.headerRow[1] }}</th>\n                                      <th>{{ tableData2.headerRow[2] }}</th>\n                                      <th class=\"text-right\">{{ tableData2.headerRow[3] }}</th>\n                                      <th class=\"text-right\">{{ tableData2.headerRow[4] }}</th>\n                                    </tr>\n                                </thead>\n                                <tbody>\n                                    <tr *ngFor=\"let row of tableData2.dataRows\">\n                                        <td class=\"text-center\">{{row.id}}</td>\n                                        <td>{{row.name}}</td>\n                                        <td>{{row.job_position}}</td>\n                                        <td class=\"text-right\">&euro; {{row.salary}}</td>\n                                        <td class=\"text-right\">\n                                            <div [ngSwitch]=\"row.active\">\n                                                <div *ngSwitchCase=\"true\">\n                                                    <input type=\"checkbox\" class=\"switch-plain\" checked=\"\">\n                                                </div>\n                                                <div *ngSwitchDefault>\n                                                    <input type=\"checkbox\" class=\"switch-plain\">\n                                                </div>\n                                            </div>\n                                        </td>\n                                    </tr>\n                                </tbody>\n                        </table>\n                    </div>\n                </div>\n            </div>\n            <div class=\"col-md-12\">\n                <div class=\"card\">\n                    <div class=\"card-header\">\n                        <h4 class=\"card-title\">Table Big Boy</h4>\n                        <p class=\"category\">A table for content management</p>\n                        <br />\n                    </div>\n                    <div class=\"table-responsive\">\n                        <table class=\"table table-shopping\">\n                                <thead>\n                                    <tr>\n                                        <th class=\"text-center\"></th>\n                                        <th></th>\n                                      <th class=\"text-right\">{{ tableData3.headerRow[2] }}</th>\n                                      <th class=\"text-right\">{{ tableData3.headerRow[3] }}</th>\n                                      <th class=\"text-right\">{{ tableData3.headerRow[4] }}</th>\n                                    </tr>\n                                </thead>\n                              <tbody>\n                                  <tr *ngFor=\"let row of tableData3.dataRows\">\n                                      <td>\n                                          <div class=\"img-container\">\n                                              <img src=\"../../assets/img/{{row[0]}}\" alt=\"...\">\n                                          </div>\n                                      </td>\n                                      <td class=\"td-product\">\n                                          <strong>{{row[1]}}</strong>\n                                          <p>{{row[2]}}</p>\n                                      </td>\n                                      <td class=\"td-price\">\n                                          <small>&euro;</small>{{row[3]}}\n                                      </td>\n                                      <td class=\"td-number td-quantity\">\n                                          {{row[4]}}\n                                          <div class=\"btn-group\">\n                                              <button class=\"btn btn-sm\"><i class=\"ti-minus\"></i></button>\n                                              <button class=\"btn btn-sm\"><i class=\"ti-plus\"></i></button>\n                                          </div>\n                                      </td>\n                                      <td class=\"td-number\">\n                                          <small>&euro;</small>{{row[5]}}\n                                      </td>\n                                  </tr>\n\n                                  <tr>\n                                      <td colspan=\"2\"></td>\n                                      <td></td>\n                                      <td class=\"td-total\">\n                                          Total\n                                      </td>\n                                      <td class=\"td-total\">\n                                          {{getTotal()| currency:'EUR':true:'1.2-2'}}\n                                      </td>\n                                  </tr>\n                              </tbody>\n                        </table>\n                    </div>\n                </div>\n            </div>\n        </div>\n    </div>\n</div>\n"

/***/ }),

/***/ "../../../../../src/app/tables/extendedtable/extendedtable.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ExtendedTableComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var ExtendedTableComponent = /** @class */ (function () {
    function ExtendedTableComponent() {
    }
    ExtendedTableComponent.prototype.ngOnInit = function () {
        this.tableData1 = {
            headerRow: ['#', 'Name', 'Job Position', 'Since', 'Salary', 'Actions'],
            dataRows: [
                ['1', 'Andrew Mike', 'Develop', '2013', '99,225', ''],
                ['2', 'John Doe', 'Design', '2012', '89,241', ''],
                ['3', 'Alex Mike', 'Design', '2010', '92,144', ''],
                ['4', 'Mike Monday', 'Marketing', '2013', '49,990', ''],
                ['5', 'Paul Dickens', 'Communication', '2015', '69,201', '']
            ]
        };
        this.tableData2 = {
            headerRow: ['#', 'Name', 'Job Position', 'Salary', 'Active'],
            dataRows: [
                { id: 1, name: 'Andrew Mike', job_position: 'Develop', salary: '99,225', active: false },
                { id: 2, name: 'John Doe', job_position: 'Design', salary: '89,241', active: false },
                { id: 3, name: 'Alex Mike', job_position: 'Design', salary: '92,144', active: true },
                { id: 4, name: 'Mike Monday', job_position: 'Marketing', salary: '49,990', active: true }
            ]
        };
        this.tableData3 = {
            headerRow: ['', '', 'Price', 'Quantity', 'Total'],
            dataRows: [
                ['tables/agenda.png', 'Get Shit Done Notebook', 'Most beautiful agenda for the office.', '49', '1', '549'],
                ['tables/stylus.jpg', 'Stylus', 'Design is not just what it looks like and feels like. Design is how it works.', '499', '2', '998'],
                ['tables/evernote.png', 'Evernote iPad Stander', 'A groundbreaking Retina display. All-flash architecture. Fourth-generation Intel processors.', '799', '1', '799']
            ]
        };
    };
    ExtendedTableComponent.prototype.ngAfterViewInit = function () {
        // Init Tooltips
        $('[rel="tooltip"]').tooltip();
        $('.switch-plain').bootstrapSwitch({
            onColor: '',
            onText: '',
            offText: ''
        });
    };
    ExtendedTableComponent.prototype.getTotal = function () {
        var total = 0;
        for (var i = 0; i < this.tableData3.dataRows.length; i++) {
            var integer = parseInt(this.tableData3.dataRows[i][5]);
            total += integer;
        }
        return total;
    };
    ;
    ExtendedTableComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'extended-table-cmp',
            template: __webpack_require__("../../../../../src/app/tables/extendedtable/extendedtable.component.html")
        })
    ], ExtendedTableComponent);
    return ExtendedTableComponent;
}());



/***/ }),

/***/ "../../../../../src/app/tables/regulartable/regulartable.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"main-content\">\n    <div class=\"container-fluid\">\n        <div class=\"row\">\n            <div class=\"col-md-12\">\n                <div class=\"card\">\n                    <div class=\"card-header\">\n                        <h4 class=\"card-title\">Striped Table</h4>\n                        <p class=\"category\">Here is a subtitle for this table</p>\n                    </div>\n                    <div class=\"card-content table-responsive table-full-width\">\n                          <table class=\"table table-striped\">\n                                <thead>\n                                    <tr>\n                                        <th *ngFor=\"let cell of tableData1.headerRow\">{{ cell }}</th>\n                                    </tr>\n                                </thead>\n                                <tbody>\n                                    <tr *ngFor=\"let row of tableData1.dataRows\">\n                                        <td *ngFor=\"let cell of row\">{{cell}}</td>\n                                    </tr>\n                                </tbody>\n                          </table>\n\n                    </div>\n                </div>\n                <div class=\"col-md-12\">\n                    <div class=\"card card-plain\">\n                        <div class=\"card-header\">\n                            <h4 class=\"card-title\">Table on Plain Background</h4>\n                            <p class=\"category\">Here is a subtitle for this table</p>\n                        </div>\n                        <div class=\"card-content table-responsive table-full-width\">\n                          <table class=\"table table-hover\">\n                              <thead>\n                                  <tr>\n                                    <th *ngFor=\"let cell of tableData2.headerRow\">{{ cell }}</th>\n                                  </tr>\n                              </thead>\n                            <tbody>\n                                    <tr *ngFor=\"let row of tableData2.dataRows\">\n                                        <td *ngFor=\"let cell of row\">{{cell}}</td>\n                                    </tr>\n                            </tbody>\n                          </table>\n\n                        </div>\n                    </div>\n                </div>\n                <div class=\"col-md-12\">\n                    <div class=\"card\">\n                        <div class=\"card-header\">\n                            <h4 class=\"card-title\">Regular Table with Colors</h4>\n                            <p class=\"category\">Here is a subtitle for this table</p>\n                        </div>\n                        <div class=\"card-content table-responsive table-full-width\">\n                            <table class=\"table table-hover\">\n                                <thead>\n                                    <tr>\n                                      <th *ngFor=\"let cell of tableData3.headerRow\">{{ cell }}</th>\n                                    </tr>\n                                </thead>\n                              <tbody>\n                                      <tr class=\"success\">\n                                          <td *ngFor=\"let cell of tableData3.dataRows[0]\">{{cell}}</td>\n                                      </tr>\n                                      <tr>\n                                          <td *ngFor=\"let cell of tableData3.dataRows[1]\">{{cell}}</td>\n                                      </tr>\n                                      <tr class=\"info\">\n                                          <td *ngFor=\"let cell of tableData3.dataRows[2]\">{{cell}}</td>\n                                      </tr>\n                                      <tr>\n                                          <td *ngFor=\"let cell of tableData3.dataRows[3]\">{{cell}}</td>\n                                      </tr>\n                                      <tr class=\"danger\">\n                                          <td *ngFor=\"let cell of tableData3.dataRows[4]\">{{cell}}</td>\n                                      </tr>\n                                      <tr>\n                                          <td *ngFor=\"let cell of tableData3.dataRows[5]\">{{cell}}</td>\n                                      </tr>\n                                      <tr class=\"warning\">\n                                          <td *ngFor=\"let cell of tableData3.dataRows[6]\">{{cell}}</td>\n                                      </tr>\n                              </tbody>\n                            </table>\n                        </div>\n                    </div>\n                </div>\n            </div>\n        </div>\n    </div>\n</div>\n"

/***/ }),

/***/ "../../../../../src/app/tables/regulartable/regulartable.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return RegularTableComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var RegularTableComponent = /** @class */ (function () {
    function RegularTableComponent() {
    }
    RegularTableComponent.prototype.ngOnInit = function () {
        this.tableData1 = {
            headerRow: ['ID', 'Name', 'Country', 'City', 'Salary'],
            dataRows: [
                ['1', 'Dakota Rice', 'Niger', 'Oud-Turnhout', '$36,738'],
                ['2', 'Minerva Hooper', 'Curaçao', 'Sinaai-Waas', '$23,789'],
                ['3', 'Sage Rodriguez', 'Netherlands', 'Baileux', '$56,142'],
                ['4', 'Philip Chaney', 'Korea, South', 'Overland Park', '$38,735'],
                ['5', 'Doris Greene', 'Malawi', 'Feldkirchen in Kärnten', '$63,542'],
                ['6', 'Mason Porter', 'Chile', 'Gloucester', '$78,615']
            ]
        };
        this.tableData2 = {
            headerRow: ['ID', 'Name', 'Salary', 'Country', 'City'],
            dataRows: [
                ['1', 'Dakota Rice', '$36,738', 'Niger', 'Oud-Turnhout'],
                ['2', 'Minerva Hooper', '$23,789', 'Curaçao', 'Sinaai-Waas'],
                ['3', 'Sage Rodriguez', '$56,142', 'Netherlands', 'Baileux'],
                ['4', 'Philip Chaney', '$38,735', 'Korea, South', 'Overland Park'],
                ['5', 'Doris Greene', '$63,542', 'Malawi', 'Feldkirchen in Kärnten',],
                ['6', 'Mason Porter', '$78,615', 'Chile', 'Gloucester']
            ]
        };
        this.tableData3 = {
            headerRow: ['ID', 'Name', 'Salary', 'Country', 'City'],
            dataRows: [
                ['1', 'Dakota Rice (Success)', '$36,738', 'Niger', 'Oud-Turnhout'],
                ['2', 'Minerva Hooper', '$23,789', 'Curaçao', 'Sinaai-Waas'],
                ['3', 'Sage Rodriguez (Info)', '$56,142', 'Netherlands', 'Baileux'],
                ['4', 'Philip Chaney', '$38,735', 'Korea, South', 'Overland Park'],
                ['5', 'Doris Greene (Danger)', '$63,542', 'Malawi', 'Feldkirchen in Kärnten',],
                ['6', 'Mason Porter', '$78,615', 'Chile', 'Gloucester'],
                ['7', 'Mike Chaney (Warning)', '$38,735', 'Romania', 'Bucharest']
            ]
        };
    };
    RegularTableComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'regular-table-cmp',
            template: __webpack_require__("../../../../../src/app/tables/regulartable/regulartable.component.html")
        })
    ], RegularTableComponent);
    return RegularTableComponent;
}());



/***/ }),

/***/ "../../../../../src/app/tables/tables.module.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "TablesModule", function() { return TablesModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("../../../core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__("../../../router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__("../../../common/esm5/common.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_forms__ = __webpack_require__("../../../forms/esm5/forms.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__tables_routing__ = __webpack_require__("../../../../../src/app/tables/tables.routing.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__extendedtable_extendedtable_component__ = __webpack_require__("../../../../../src/app/tables/extendedtable/extendedtable.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__regulartable_regulartable_component__ = __webpack_require__("../../../../../src/app/tables/regulartable/regulartable.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__datatable_net_datatable_component__ = __webpack_require__("../../../../../src/app/tables/datatable.net/datatable.component.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};








var TablesModule = /** @class */ (function () {
    function TablesModule() {
    }
    TablesModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["L" /* NgModule */])({
            imports: [
                __WEBPACK_IMPORTED_MODULE_2__angular_common__["b" /* CommonModule */],
                __WEBPACK_IMPORTED_MODULE_1__angular_router__["d" /* RouterModule */].forChild(__WEBPACK_IMPORTED_MODULE_4__tables_routing__["a" /* TablesRoutes */]),
                __WEBPACK_IMPORTED_MODULE_3__angular_forms__["c" /* FormsModule */]
            ],
            declarations: [
                __WEBPACK_IMPORTED_MODULE_5__extendedtable_extendedtable_component__["a" /* ExtendedTableComponent */],
                __WEBPACK_IMPORTED_MODULE_7__datatable_net_datatable_component__["a" /* DataTableComponent */],
                __WEBPACK_IMPORTED_MODULE_6__regulartable_regulartable_component__["a" /* RegularTableComponent */]
            ]
        })
    ], TablesModule);
    return TablesModule;
}());



/***/ }),

/***/ "../../../../../src/app/tables/tables.routing.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TablesRoutes; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__extendedtable_extendedtable_component__ = __webpack_require__("../../../../../src/app/tables/extendedtable/extendedtable.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__regulartable_regulartable_component__ = __webpack_require__("../../../../../src/app/tables/regulartable/regulartable.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__datatable_net_datatable_component__ = __webpack_require__("../../../../../src/app/tables/datatable.net/datatable.component.ts");



var TablesRoutes = [{
        path: '',
        children: [{
                path: 'regular',
                component: __WEBPACK_IMPORTED_MODULE_1__regulartable_regulartable_component__["a" /* RegularTableComponent */]
            }]
    }, {
        path: '',
        children: [{
                path: 'extended',
                component: __WEBPACK_IMPORTED_MODULE_0__extendedtable_extendedtable_component__["a" /* ExtendedTableComponent */]
            }]
    }, {
        path: '',
        children: [{
                path: 'datatables.net',
                component: __WEBPACK_IMPORTED_MODULE_2__datatable_net_datatable_component__["a" /* DataTableComponent */]
            }]
    }
];


/***/ })

});
//# sourceMappingURL=tables.module.chunk.js.map