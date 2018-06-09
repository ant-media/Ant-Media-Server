webpackJsonp(["forms.module"],{

/***/ "./node_modules/nouislider/distribute/nouislider.js":
/***/ (function(module, exports, __webpack_require__) {

var __WEBPACK_AMD_DEFINE_FACTORY__, __WEBPACK_AMD_DEFINE_ARRAY__, __WEBPACK_AMD_DEFINE_RESULT__;/*! nouislider - 9.2.0 - 2017-01-11 10:35:34 */

(function (factory) {

    if ( true ) {

        // AMD. Register as an anonymous module.
        !(__WEBPACK_AMD_DEFINE_ARRAY__ = [], __WEBPACK_AMD_DEFINE_FACTORY__ = (factory),
				__WEBPACK_AMD_DEFINE_RESULT__ = (typeof __WEBPACK_AMD_DEFINE_FACTORY__ === 'function' ?
				(__WEBPACK_AMD_DEFINE_FACTORY__.apply(exports, __WEBPACK_AMD_DEFINE_ARRAY__)) : __WEBPACK_AMD_DEFINE_FACTORY__),
				__WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));

    } else if ( typeof exports === 'object' ) {

        // Node/CommonJS
        module.exports = factory();

    } else {

        // Browser globals
        window.noUiSlider = factory();
    }

}(function( ){

	'use strict';

	var VERSION = '9.2.0';


	// Creates a node, adds it to target, returns the new node.
	function addNodeTo ( target, className ) {
		var div = document.createElement('div');
		addClass(div, className);
		target.appendChild(div);
		return div;
	}

	// Removes duplicates from an array.
	function unique ( array ) {
		return array.filter(function(a){
			return !this[a] ? this[a] = true : false;
		}, {});
	}

	// Round a value to the closest 'to'.
	function closest ( value, to ) {
		return Math.round(value / to) * to;
	}

	// Current position of an element relative to the document.
	function offset ( elem, orientation ) {

	var rect = elem.getBoundingClientRect(),
		doc = elem.ownerDocument,
		docElem = doc.documentElement,
		pageOffset = getPageOffset();

		// getBoundingClientRect contains left scroll in Chrome on Android.
		// I haven't found a feature detection that proves this. Worst case
		// scenario on mis-match: the 'tap' feature on horizontal sliders breaks.
		if ( /webkit.*Chrome.*Mobile/i.test(navigator.userAgent) ) {
			pageOffset.x = 0;
		}

		return orientation ? (rect.top + pageOffset.y - docElem.clientTop) : (rect.left + pageOffset.x - docElem.clientLeft);
	}

	// Checks whether a value is numerical.
	function isNumeric ( a ) {
		return typeof a === 'number' && !isNaN( a ) && isFinite( a );
	}

	// Sets a class and removes it after [duration] ms.
	function addClassFor ( element, className, duration ) {
		if (duration > 0) {
		addClass(element, className);
			setTimeout(function(){
				removeClass(element, className);
			}, duration);
		}
	}

	// Limits a value to 0 - 100
	function limit ( a ) {
		return Math.max(Math.min(a, 100), 0);
	}

	// Wraps a variable as an array, if it isn't one yet.
	// Note that an input array is returned by reference!
	function asArray ( a ) {
		return Array.isArray(a) ? a : [a];
	}

	// Counts decimals
	function countDecimals ( numStr ) {
		numStr = String(numStr);
		var pieces = numStr.split(".");
		return pieces.length > 1 ? pieces[1].length : 0;
	}

	// http://youmightnotneedjquery.com/#add_class
	function addClass ( el, className ) {
		if ( el.classList ) {
			el.classList.add(className);
		} else {
			el.className += ' ' + className;
		}
	}

	// http://youmightnotneedjquery.com/#remove_class
	function removeClass ( el, className ) {
		if ( el.classList ) {
			el.classList.remove(className);
		} else {
			el.className = el.className.replace(new RegExp('(^|\\b)' + className.split(' ').join('|') + '(\\b|$)', 'gi'), ' ');
		}
	}

	// https://plainjs.com/javascript/attributes/adding-removing-and-testing-for-classes-9/
	function hasClass ( el, className ) {
		return el.classList ? el.classList.contains(className) : new RegExp('\\b' + className + '\\b').test(el.className);
	}

	// https://developer.mozilla.org/en-US/docs/Web/API/Window/scrollY#Notes
	function getPageOffset ( ) {

		var supportPageOffset = window.pageXOffset !== undefined,
			isCSS1Compat = ((document.compatMode || "") === "CSS1Compat"),
			x = supportPageOffset ? window.pageXOffset : isCSS1Compat ? document.documentElement.scrollLeft : document.body.scrollLeft,
			y = supportPageOffset ? window.pageYOffset : isCSS1Compat ? document.documentElement.scrollTop : document.body.scrollTop;

		return {
			x: x,
			y: y
		};
	}

	// we provide a function to compute constants instead
	// of accessing window.* as soon as the module needs it
	// so that we do not compute anything if not needed
	function getActions ( ) {

		// Determine the events to bind. IE11 implements pointerEvents without
		// a prefix, which breaks compatibility with the IE10 implementation.
		return window.navigator.pointerEnabled ? {
			start: 'pointerdown',
			move: 'pointermove',
			end: 'pointerup'
		} : window.navigator.msPointerEnabled ? {
			start: 'MSPointerDown',
			move: 'MSPointerMove',
			end: 'MSPointerUp'
		} : {
			start: 'mousedown touchstart',
			move: 'mousemove touchmove',
			end: 'mouseup touchend'
		};
	}


// Value calculation

	// Determine the size of a sub-range in relation to a full range.
	function subRangeRatio ( pa, pb ) {
		return (100 / (pb - pa));
	}

	// (percentage) How many percent is this value of this range?
	function fromPercentage ( range, value ) {
		return (value * 100) / ( range[1] - range[0] );
	}

	// (percentage) Where is this value on this range?
	function toPercentage ( range, value ) {
		return fromPercentage( range, range[0] < 0 ?
			value + Math.abs(range[0]) :
				value - range[0] );
	}

	// (value) How much is this percentage on this range?
	function isPercentage ( range, value ) {
		return ((value * ( range[1] - range[0] )) / 100) + range[0];
	}


// Range conversion

	function getJ ( value, arr ) {

		var j = 1;

		while ( value >= arr[j] ){
			j += 1;
		}

		return j;
	}

	// (percentage) Input a value, find where, on a scale of 0-100, it applies.
	function toStepping ( xVal, xPct, value ) {

		if ( value >= xVal.slice(-1)[0] ){
			return 100;
		}

		var j = getJ( value, xVal ), va, vb, pa, pb;

		va = xVal[j-1];
		vb = xVal[j];
		pa = xPct[j-1];
		pb = xPct[j];

		return pa + (toPercentage([va, vb], value) / subRangeRatio (pa, pb));
	}

	// (value) Input a percentage, find where it is on the specified range.
	function fromStepping ( xVal, xPct, value ) {

		// There is no range group that fits 100
		if ( value >= 100 ){
			return xVal.slice(-1)[0];
		}

		var j = getJ( value, xPct ), va, vb, pa, pb;

		va = xVal[j-1];
		vb = xVal[j];
		pa = xPct[j-1];
		pb = xPct[j];

		return isPercentage([va, vb], (value - pa) * subRangeRatio (pa, pb));
	}

	// (percentage) Get the step that applies at a certain value.
	function getStep ( xPct, xSteps, snap, value ) {

		if ( value === 100 ) {
			return value;
		}

		var j = getJ( value, xPct ), a, b;

		// If 'snap' is set, steps are used as fixed points on the slider.
		if ( snap ) {

			a = xPct[j-1];
			b = xPct[j];

			// Find the closest position, a or b.
			if ((value - a) > ((b-a)/2)){
				return b;
			}

			return a;
		}

		if ( !xSteps[j-1] ){
			return value;
		}

		return xPct[j-1] + closest(
			value - xPct[j-1],
			xSteps[j-1]
		);
	}


// Entry parsing

	function handleEntryPoint ( index, value, that ) {

		var percentage;

		// Wrap numerical input in an array.
		if ( typeof value === "number" ) {
			value = [value];
		}

		// Reject any invalid input, by testing whether value is an array.
		if ( Object.prototype.toString.call( value ) !== '[object Array]' ){
			throw new Error("noUiSlider (" + VERSION + "): 'range' contains invalid value.");
		}

		// Covert min/max syntax to 0 and 100.
		if ( index === 'min' ) {
			percentage = 0;
		} else if ( index === 'max' ) {
			percentage = 100;
		} else {
			percentage = parseFloat( index );
		}

		// Check for correct input.
		if ( !isNumeric( percentage ) || !isNumeric( value[0] ) ) {
			throw new Error("noUiSlider (" + VERSION + "): 'range' value isn't numeric.");
		}

		// Store values.
		that.xPct.push( percentage );
		that.xVal.push( value[0] );

		// NaN will evaluate to false too, but to keep
		// logging clear, set step explicitly. Make sure
		// not to override the 'step' setting with false.
		if ( !percentage ) {
			if ( !isNaN( value[1] ) ) {
				that.xSteps[0] = value[1];
			}
		} else {
			that.xSteps.push( isNaN(value[1]) ? false : value[1] );
		}

		that.xHighestCompleteStep.push(0);
	}

	function handleStepPoint ( i, n, that ) {

		// Ignore 'false' stepping.
		if ( !n ) {
			return true;
		}

		// Factor to range ratio
		that.xSteps[i] = fromPercentage([
			 that.xVal[i]
			,that.xVal[i+1]
		], n) / subRangeRatio (
			that.xPct[i],
			that.xPct[i+1] );

		var totalSteps = (that.xVal[i+1] - that.xVal[i]) / that.xNumSteps[i];
		var highestStep = Math.ceil(Number(totalSteps.toFixed(3)) - 1);
		var step = that.xVal[i] + (that.xNumSteps[i] * highestStep);

		that.xHighestCompleteStep[i] = step;
	}


// Interface

	// The interface to Spectrum handles all direction-based
	// conversions, so the above values are unaware.

	function Spectrum ( entry, snap, direction, singleStep ) {

		this.xPct = [];
		this.xVal = [];
		this.xSteps = [ singleStep || false ];
		this.xNumSteps = [ false ];
		this.xHighestCompleteStep = [];

		this.snap = snap;
		this.direction = direction;

		var index, ordered = [ /* [0, 'min'], [1, '50%'], [2, 'max'] */ ];

		// Map the object keys to an array.
		for ( index in entry ) {
			if ( entry.hasOwnProperty(index) ) {
				ordered.push([entry[index], index]);
			}
		}

		// Sort all entries by value (numeric sort).
		if ( ordered.length && typeof ordered[0][0] === "object" ) {
			ordered.sort(function(a, b) { return a[0][0] - b[0][0]; });
		} else {
			ordered.sort(function(a, b) { return a[0] - b[0]; });
		}


		// Convert all entries to subranges.
		for ( index = 0; index < ordered.length; index++ ) {
			handleEntryPoint(ordered[index][1], ordered[index][0], this);
		}

		// Store the actual step values.
		// xSteps is sorted in the same order as xPct and xVal.
		this.xNumSteps = this.xSteps.slice(0);

		// Convert all numeric steps to the percentage of the subrange they represent.
		for ( index = 0; index < this.xNumSteps.length; index++ ) {
			handleStepPoint(index, this.xNumSteps[index], this);
		}
	}

	Spectrum.prototype.getMargin = function ( value ) {

		var step = this.xNumSteps[0];

		if ( step && ((value / step) % 1) !== 0 ) {
			throw new Error("noUiSlider (" + VERSION + "): 'limit', 'margin' and 'padding' must be divisible by step.");
		}

		return this.xPct.length === 2 ? fromPercentage(this.xVal, value) : false;
	};

	Spectrum.prototype.toStepping = function ( value ) {

		value = toStepping( this.xVal, this.xPct, value );

		return value;
	};

	Spectrum.prototype.fromStepping = function ( value ) {

		return fromStepping( this.xVal, this.xPct, value );
	};

	Spectrum.prototype.getStep = function ( value ) {

		value = getStep(this.xPct, this.xSteps, this.snap, value );

		return value;
	};

	Spectrum.prototype.getNearbySteps = function ( value ) {

		var j = getJ(value, this.xPct);

		return {
			stepBefore: { startValue: this.xVal[j-2], step: this.xNumSteps[j-2], highestStep: this.xHighestCompleteStep[j-2] },
			thisStep: { startValue: this.xVal[j-1], step: this.xNumSteps[j-1], highestStep: this.xHighestCompleteStep[j-1] },
			stepAfter: { startValue: this.xVal[j-0], step: this.xNumSteps[j-0], highestStep: this.xHighestCompleteStep[j-0] }
		};
	};

	Spectrum.prototype.countStepDecimals = function () {
		var stepDecimals = this.xNumSteps.map(countDecimals);
		return Math.max.apply(null, stepDecimals);
 	};

	// Outside testing
	Spectrum.prototype.convert = function ( value ) {
		return this.getStep(this.toStepping(value));
	};

/*	Every input option is tested and parsed. This'll prevent
	endless validation in internal methods. These tests are
	structured with an item for every option available. An
	option can be marked as required by setting the 'r' flag.
	The testing function is provided with three arguments:
		- The provided value for the option;
		- A reference to the options object;
		- The name for the option;

	The testing function returns false when an error is detected,
	or true when everything is OK. It can also modify the option
	object, to make sure all values can be correctly looped elsewhere. */

	var defaultFormatter = { 'to': function( value ){
		return value !== undefined && value.toFixed(2);
	}, 'from': Number };

	function testStep ( parsed, entry ) {

		if ( !isNumeric( entry ) ) {
			throw new Error("noUiSlider (" + VERSION + "): 'step' is not numeric.");
		}

		// The step option can still be used to set stepping
		// for linear sliders. Overwritten if set in 'range'.
		parsed.singleStep = entry;
	}

	function testRange ( parsed, entry ) {

		// Filter incorrect input.
		if ( typeof entry !== 'object' || Array.isArray(entry) ) {
			throw new Error("noUiSlider (" + VERSION + "): 'range' is not an object.");
		}

		// Catch missing start or end.
		if ( entry.min === undefined || entry.max === undefined ) {
			throw new Error("noUiSlider (" + VERSION + "): Missing 'min' or 'max' in 'range'.");
		}

		// Catch equal start or end.
		if ( entry.min === entry.max ) {
			throw new Error("noUiSlider (" + VERSION + "): 'range' 'min' and 'max' cannot be equal.");
		}

		parsed.spectrum = new Spectrum(entry, parsed.snap, parsed.dir, parsed.singleStep);
	}

	function testStart ( parsed, entry ) {

		entry = asArray(entry);

		// Validate input. Values aren't tested, as the public .val method
		// will always provide a valid location.
		if ( !Array.isArray( entry ) || !entry.length ) {
			throw new Error("noUiSlider (" + VERSION + "): 'start' option is incorrect.");
		}

		// Store the number of handles.
		parsed.handles = entry.length;

		// When the slider is initialized, the .val method will
		// be called with the start options.
		parsed.start = entry;
	}

	function testSnap ( parsed, entry ) {

		// Enforce 100% stepping within subranges.
		parsed.snap = entry;

		if ( typeof entry !== 'boolean' ){
			throw new Error("noUiSlider (" + VERSION + "): 'snap' option must be a boolean.");
		}
	}

	function testAnimate ( parsed, entry ) {

		// Enforce 100% stepping within subranges.
		parsed.animate = entry;

		if ( typeof entry !== 'boolean' ){
			throw new Error("noUiSlider (" + VERSION + "): 'animate' option must be a boolean.");
		}
	}

	function testAnimationDuration ( parsed, entry ) {

		parsed.animationDuration = entry;

		if ( typeof entry !== 'number' ){
			throw new Error("noUiSlider (" + VERSION + "): 'animationDuration' option must be a number.");
		}
	}

	function testConnect ( parsed, entry ) {

		var connect = [false];
		var i;

		// Map legacy options
		if ( entry === 'lower' ) {
			entry = [true, false];
		}

		else if ( entry === 'upper' ) {
			entry = [false, true];
		}

		// Handle boolean options
		if ( entry === true || entry === false ) {

			for ( i = 1; i < parsed.handles; i++ ) {
				connect.push(entry);
			}

			connect.push(false);
		}

		// Reject invalid input
		else if ( !Array.isArray( entry ) || !entry.length || entry.length !== parsed.handles + 1 ) {
			throw new Error("noUiSlider (" + VERSION + "): 'connect' option doesn't match handle count.");
		}

		else {
			connect = entry;
		}

		parsed.connect = connect;
	}

	function testOrientation ( parsed, entry ) {

		// Set orientation to an a numerical value for easy
		// array selection.
		switch ( entry ){
		  case 'horizontal':
			parsed.ort = 0;
			break;
		  case 'vertical':
			parsed.ort = 1;
			break;
		  default:
			throw new Error("noUiSlider (" + VERSION + "): 'orientation' option is invalid.");
		}
	}

	function testMargin ( parsed, entry ) {

		if ( !isNumeric(entry) ){
			throw new Error("noUiSlider (" + VERSION + "): 'margin' option must be numeric.");
		}

		// Issue #582
		if ( entry === 0 ) {
			return;
		}

		parsed.margin = parsed.spectrum.getMargin(entry);

		if ( !parsed.margin ) {
			throw new Error("noUiSlider (" + VERSION + "): 'margin' option is only supported on linear sliders.");
		}
	}

	function testLimit ( parsed, entry ) {

		if ( !isNumeric(entry) ){
			throw new Error("noUiSlider (" + VERSION + "): 'limit' option must be numeric.");
		}

		parsed.limit = parsed.spectrum.getMargin(entry);

		if ( !parsed.limit || parsed.handles < 2 ) {
			throw new Error("noUiSlider (" + VERSION + "): 'limit' option is only supported on linear sliders with 2 or more handles.");
		}
	}

	function testPadding ( parsed, entry ) {

		if ( !isNumeric(entry) ){
			throw new Error("noUiSlider (" + VERSION + "): 'padding' option must be numeric.");
		}

		if ( entry === 0 ) {
			return;
		}

		parsed.padding = parsed.spectrum.getMargin(entry);

		if ( !parsed.padding ) {
			throw new Error("noUiSlider (" + VERSION + "): 'padding' option is only supported on linear sliders.");
		}

		if ( parsed.padding < 0 ) {
			throw new Error("noUiSlider (" + VERSION + "): 'padding' option must be a positive number.");
		}

		if ( parsed.padding >= 50 ) {
			throw new Error("noUiSlider (" + VERSION + "): 'padding' option must be less than half the range.");
		}
	}

	function testDirection ( parsed, entry ) {

		// Set direction as a numerical value for easy parsing.
		// Invert connection for RTL sliders, so that the proper
		// handles get the connect/background classes.
		switch ( entry ) {
		  case 'ltr':
			parsed.dir = 0;
			break;
		  case 'rtl':
			parsed.dir = 1;
			break;
		  default:
			throw new Error("noUiSlider (" + VERSION + "): 'direction' option was not recognized.");
		}
	}

	function testBehaviour ( parsed, entry ) {

		// Make sure the input is a string.
		if ( typeof entry !== 'string' ) {
			throw new Error("noUiSlider (" + VERSION + "): 'behaviour' must be a string containing options.");
		}

		// Check if the string contains any keywords.
		// None are required.
		var tap = entry.indexOf('tap') >= 0;
		var drag = entry.indexOf('drag') >= 0;
		var fixed = entry.indexOf('fixed') >= 0;
		var snap = entry.indexOf('snap') >= 0;
		var hover = entry.indexOf('hover') >= 0;

		if ( fixed ) {

			if ( parsed.handles !== 2 ) {
				throw new Error("noUiSlider (" + VERSION + "): 'fixed' behaviour must be used with 2 handles");
			}

			// Use margin to enforce fixed state
			testMargin(parsed, parsed.start[1] - parsed.start[0]);
		}

		parsed.events = {
			tap: tap || snap,
			drag: drag,
			fixed: fixed,
			snap: snap,
			hover: hover
		};
	}

	function testTooltips ( parsed, entry ) {

		if ( entry === false ) {
			return;
		}

		else if ( entry === true ) {

			parsed.tooltips = [];

			for ( var i = 0; i < parsed.handles; i++ ) {
				parsed.tooltips.push(true);
			}
		}

		else {

			parsed.tooltips = asArray(entry);

			if ( parsed.tooltips.length !== parsed.handles ) {
				throw new Error("noUiSlider (" + VERSION + "): must pass a formatter for all handles.");
			}

			parsed.tooltips.forEach(function(formatter){
				if ( typeof formatter !== 'boolean' && (typeof formatter !== 'object' || typeof formatter.to !== 'function') ) {
					throw new Error("noUiSlider (" + VERSION + "): 'tooltips' must be passed a formatter or 'false'.");
				}
			});
		}
	}

	function testFormat ( parsed, entry ) {

		parsed.format = entry;

		// Any object with a to and from method is supported.
		if ( typeof entry.to === 'function' && typeof entry.from === 'function' ) {
			return true;
		}

		throw new Error("noUiSlider (" + VERSION + "): 'format' requires 'to' and 'from' methods.");
	}

	function testCssPrefix ( parsed, entry ) {

		if ( entry !== undefined && typeof entry !== 'string' && entry !== false ) {
			throw new Error("noUiSlider (" + VERSION + "): 'cssPrefix' must be a string or `false`.");
		}

		parsed.cssPrefix = entry;
	}

	function testCssClasses ( parsed, entry ) {

		if ( entry !== undefined && typeof entry !== 'object' ) {
			throw new Error("noUiSlider (" + VERSION + "): 'cssClasses' must be an object.");
		}

		if ( typeof parsed.cssPrefix === 'string' ) {
			parsed.cssClasses = {};

			for ( var key in entry ) {
				if ( !entry.hasOwnProperty(key) ) { continue; }

				parsed.cssClasses[key] = parsed.cssPrefix + entry[key];
			}
		} else {
			parsed.cssClasses = entry;
		}
	}

	function testUseRaf ( parsed, entry ) {
		if ( entry === true || entry === false ) {
			parsed.useRequestAnimationFrame = entry;
		} else {
			throw new Error("noUiSlider (" + VERSION + "): 'useRequestAnimationFrame' option should be true (default) or false.");
		}
	}

	// Test all developer settings and parse to assumption-safe values.
	function testOptions ( options ) {

		// To prove a fix for #537, freeze options here.
		// If the object is modified, an error will be thrown.
		// Object.freeze(options);

		var parsed = {
			margin: 0,
			limit: 0,
			padding: 0,
			animate: true,
			animationDuration: 300,
			format: defaultFormatter
		};

		// Tests are executed in the order they are presented here.
		var tests = {
			'step': { r: false, t: testStep },
			'start': { r: true, t: testStart },
			'connect': { r: true, t: testConnect },
			'direction': { r: true, t: testDirection },
			'snap': { r: false, t: testSnap },
			'animate': { r: false, t: testAnimate },
			'animationDuration': { r: false, t: testAnimationDuration },
			'range': { r: true, t: testRange },
			'orientation': { r: false, t: testOrientation },
			'margin': { r: false, t: testMargin },
			'limit': { r: false, t: testLimit },
			'padding': { r: false, t: testPadding },
			'behaviour': { r: true, t: testBehaviour },
			'format': { r: false, t: testFormat },
			'tooltips': { r: false, t: testTooltips },
			'cssPrefix': { r: false, t: testCssPrefix },
			'cssClasses': { r: false, t: testCssClasses },
			'useRequestAnimationFrame': { r: false, t: testUseRaf }
		};

		var defaults = {
			'connect': false,
			'direction': 'ltr',
			'behaviour': 'tap',
			'orientation': 'horizontal',
			'cssPrefix' : 'noUi-',
			'cssClasses': {
				target: 'target',
				base: 'base',
				origin: 'origin',
				handle: 'handle',
				handleLower: 'handle-lower',
				handleUpper: 'handle-upper',
				horizontal: 'horizontal',
				vertical: 'vertical',
				background: 'background',
				connect: 'connect',
				ltr: 'ltr',
				rtl: 'rtl',
				draggable: 'draggable',
				drag: 'state-drag',
				tap: 'state-tap',
				active: 'active',
				tooltip: 'tooltip',
				pips: 'pips',
				pipsHorizontal: 'pips-horizontal',
				pipsVertical: 'pips-vertical',
				marker: 'marker',
				markerHorizontal: 'marker-horizontal',
				markerVertical: 'marker-vertical',
				markerNormal: 'marker-normal',
				markerLarge: 'marker-large',
				markerSub: 'marker-sub',
				value: 'value',
				valueHorizontal: 'value-horizontal',
				valueVertical: 'value-vertical',
				valueNormal: 'value-normal',
				valueLarge: 'value-large',
				valueSub: 'value-sub'
			},
			'useRequestAnimationFrame': true
		};

		// Run all options through a testing mechanism to ensure correct
		// input. It should be noted that options might get modified to
		// be handled properly. E.g. wrapping integers in arrays.
		Object.keys(tests).forEach(function( name ){

			// If the option isn't set, but it is required, throw an error.
			if ( options[name] === undefined && defaults[name] === undefined ) {

				if ( tests[name].r ) {
					throw new Error("noUiSlider (" + VERSION + "): '" + name + "' is required.");
				}

				return true;
			}

			tests[name].t( parsed, options[name] === undefined ? defaults[name] : options[name] );
		});

		// Forward pips options
		parsed.pips = options.pips;

		var styles = [['left', 'top'], ['right', 'bottom']];

		// Pre-define the styles.
		parsed.style = styles[parsed.dir][parsed.ort];
		parsed.styleOposite = styles[parsed.dir?0:1][parsed.ort];

		return parsed;
	}


function closure ( target, options, originalOptions ){

	var actions = getActions( );

	// All variables local to 'closure' are prefixed with 'scope_'
	var scope_Target = target;
	var scope_Locations = [];
	var scope_Base;
	var scope_Handles;
	var scope_HandleNumbers = [];
	var scope_ActiveHandle = false;
	var scope_Connects;
	var scope_Spectrum = options.spectrum;
	var scope_Values = [];
	var scope_Events = {};
	var scope_Self;


	// Append a origin to the base
	function addOrigin ( base, handleNumber ) {

		var origin = addNodeTo(base, options.cssClasses.origin);
		var handle = addNodeTo(origin, options.cssClasses.handle);

		handle.setAttribute('data-handle', handleNumber);

		if ( handleNumber === 0 ) {
			addClass(handle, options.cssClasses.handleLower);
		}

		else if ( handleNumber === options.handles - 1 ) {
			addClass(handle, options.cssClasses.handleUpper);
		}

		return origin;
	}

	// Insert nodes for connect elements
	function addConnect ( base, add ) {

		if ( !add ) {
			return false;
		}

		return addNodeTo(base, options.cssClasses.connect);
	}

	// Add handles to the slider base.
	function addElements ( connectOptions, base ) {

		scope_Handles = [];
		scope_Connects = [];

		scope_Connects.push(addConnect(base, connectOptions[0]));

		// [::::O====O====O====]
		// connectOptions = [0, 1, 1, 1]

		for ( var i = 0; i < options.handles; i++ ) {
			// Keep a list of all added handles.
			scope_Handles.push(addOrigin(base, i));
			scope_HandleNumbers[i] = i;
			scope_Connects.push(addConnect(base, connectOptions[i + 1]));
		}
	}

	// Initialize a single slider.
	function addSlider ( target ) {

		// Apply classes and data to the target.
		addClass(target, options.cssClasses.target);

		if ( options.dir === 0 ) {
			addClass(target, options.cssClasses.ltr);
		} else {
			addClass(target, options.cssClasses.rtl);
		}

		if ( options.ort === 0 ) {
			addClass(target, options.cssClasses.horizontal);
		} else {
			addClass(target, options.cssClasses.vertical);
		}

		scope_Base = addNodeTo(target, options.cssClasses.base);
	}


	function addTooltip ( handle, handleNumber ) {

		if ( !options.tooltips[handleNumber] ) {
			return false;
		}

		return addNodeTo(handle.firstChild, options.cssClasses.tooltip);
	}

	// The tooltips option is a shorthand for using the 'update' event.
	function tooltips ( ) {

		// Tooltips are added with options.tooltips in original order.
		var tips = scope_Handles.map(addTooltip);

		bindEvent('update', function(values, handleNumber, unencoded) {

			if ( !tips[handleNumber] ) {
				return;
			}

			var formattedValue = values[handleNumber];

			if ( options.tooltips[handleNumber] !== true ) {
				formattedValue = options.tooltips[handleNumber].to(unencoded[handleNumber]);
			}

			tips[handleNumber].innerHTML = formattedValue;
		});
	}


	function getGroup ( mode, values, stepped ) {

		// Use the range.
		if ( mode === 'range' || mode === 'steps' ) {
			return scope_Spectrum.xVal;
		}

		if ( mode === 'count' ) {

			if ( !values ) {
				throw new Error("noUiSlider (" + VERSION + "): 'values' required for mode 'count'.");
			}

			// Divide 0 - 100 in 'count' parts.
			var spread = ( 100 / (values - 1) );
			var v;
			var i = 0;

			values = [];

			// List these parts and have them handled as 'positions'.
			while ( (v = i++ * spread) <= 100 ) {
				values.push(v);
			}

			mode = 'positions';
		}

		if ( mode === 'positions' ) {

			// Map all percentages to on-range values.
			return values.map(function( value ){
				return scope_Spectrum.fromStepping( stepped ? scope_Spectrum.getStep( value ) : value );
			});
		}

		if ( mode === 'values' ) {

			// If the value must be stepped, it needs to be converted to a percentage first.
			if ( stepped ) {

				return values.map(function( value ){

					// Convert to percentage, apply step, return to value.
					return scope_Spectrum.fromStepping( scope_Spectrum.getStep( scope_Spectrum.toStepping( value ) ) );
				});

			}

			// Otherwise, we can simply use the values.
			return values;
		}
	}

	function generateSpread ( density, mode, group ) {

		function safeIncrement(value, increment) {
			// Avoid floating point variance by dropping the smallest decimal places.
			return (value + increment).toFixed(7) / 1;
		}

		var indexes = {};
		var firstInRange = scope_Spectrum.xVal[0];
		var lastInRange = scope_Spectrum.xVal[scope_Spectrum.xVal.length-1];
		var ignoreFirst = false;
		var ignoreLast = false;
		var prevPct = 0;

		// Create a copy of the group, sort it and filter away all duplicates.
		group = unique(group.slice().sort(function(a, b){ return a - b; }));

		// Make sure the range starts with the first element.
		if ( group[0] !== firstInRange ) {
			group.unshift(firstInRange);
			ignoreFirst = true;
		}

		// Likewise for the last one.
		if ( group[group.length - 1] !== lastInRange ) {
			group.push(lastInRange);
			ignoreLast = true;
		}

		group.forEach(function ( current, index ) {

			// Get the current step and the lower + upper positions.
			var step;
			var i;
			var q;
			var low = current;
			var high = group[index+1];
			var newPct;
			var pctDifference;
			var pctPos;
			var type;
			var steps;
			var realSteps;
			var stepsize;

			// When using 'steps' mode, use the provided steps.
			// Otherwise, we'll step on to the next subrange.
			if ( mode === 'steps' ) {
				step = scope_Spectrum.xNumSteps[ index ];
			}

			// Default to a 'full' step.
			if ( !step ) {
				step = high-low;
			}

			// Low can be 0, so test for false. If high is undefined,
			// we are at the last subrange. Index 0 is already handled.
			if ( low === false || high === undefined ) {
				return;
			}

			// Make sure step isn't 0, which would cause an infinite loop (#654)
			step = Math.max(step, 0.0000001);

			// Find all steps in the subrange.
			for ( i = low; i <= high; i = safeIncrement(i, step) ) {

				// Get the percentage value for the current step,
				// calculate the size for the subrange.
				newPct = scope_Spectrum.toStepping( i );
				pctDifference = newPct - prevPct;

				steps = pctDifference / density;
				realSteps = Math.round(steps);

				// This ratio represents the ammount of percentage-space a point indicates.
				// For a density 1 the points/percentage = 1. For density 2, that percentage needs to be re-devided.
				// Round the percentage offset to an even number, then divide by two
				// to spread the offset on both sides of the range.
				stepsize = pctDifference/realSteps;

				// Divide all points evenly, adding the correct number to this subrange.
				// Run up to <= so that 100% gets a point, event if ignoreLast is set.
				for ( q = 1; q <= realSteps; q += 1 ) {

					// The ratio between the rounded value and the actual size might be ~1% off.
					// Correct the percentage offset by the number of points
					// per subrange. density = 1 will result in 100 points on the
					// full range, 2 for 50, 4 for 25, etc.
					pctPos = prevPct + ( q * stepsize );
					indexes[pctPos.toFixed(5)] = ['x', 0];
				}

				// Determine the point type.
				type = (group.indexOf(i) > -1) ? 1 : ( mode === 'steps' ? 2 : 0 );

				// Enforce the 'ignoreFirst' option by overwriting the type for 0.
				if ( !index && ignoreFirst ) {
					type = 0;
				}

				if ( !(i === high && ignoreLast)) {
					// Mark the 'type' of this point. 0 = plain, 1 = real value, 2 = step value.
					indexes[newPct.toFixed(5)] = [i, type];
				}

				// Update the percentage count.
				prevPct = newPct;
			}
		});

		return indexes;
	}

	function addMarking ( spread, filterFunc, formatter ) {

		var element = document.createElement('div');
		var out = '';
		var valueSizeClasses = [
			options.cssClasses.valueNormal,
			options.cssClasses.valueLarge,
			options.cssClasses.valueSub
		];
		var markerSizeClasses = [
			options.cssClasses.markerNormal,
			options.cssClasses.markerLarge,
			options.cssClasses.markerSub
		];
		var valueOrientationClasses = [
			options.cssClasses.valueHorizontal,
			options.cssClasses.valueVertical
		];
		var markerOrientationClasses = [
			options.cssClasses.markerHorizontal,
			options.cssClasses.markerVertical
		];

		addClass(element, options.cssClasses.pips);
		addClass(element, options.ort === 0 ? options.cssClasses.pipsHorizontal : options.cssClasses.pipsVertical);

		function getClasses( type, source ){
			var a = source === options.cssClasses.value;
			var orientationClasses = a ? valueOrientationClasses : markerOrientationClasses;
			var sizeClasses = a ? valueSizeClasses : markerSizeClasses;

			return source + ' ' + orientationClasses[options.ort] + ' ' + sizeClasses[type];
		}

		function getTags( offset, source, values ) {
			return 'class="' + getClasses(values[1], source) + '" style="' + options.style + ': ' + offset + '%"';
		}

		function addSpread ( offset, values ){

			// Apply the filter function, if it is set.
			values[1] = (values[1] && filterFunc) ? filterFunc(values[0], values[1]) : values[1];

			// Add a marker for every point
			out += '<div ' + getTags(offset, options.cssClasses.marker, values) + '></div>';

			// Values are only appended for points marked '1' or '2'.
			if ( values[1] ) {
				out += '<div ' + getTags(offset, options.cssClasses.value, values) + '>' + formatter.to(values[0]) + '</div>';
			}
		}

		// Append all points.
		Object.keys(spread).forEach(function(a){
			addSpread(a, spread[a]);
		});

		element.innerHTML = out;

		return element;
	}

	function pips ( grid ) {

		var mode = grid.mode;
		var density = grid.density || 1;
		var filter = grid.filter || false;
		var values = grid.values || false;
		var stepped = grid.stepped || false;
		var group = getGroup( mode, values, stepped );
		var spread = generateSpread( density, mode, group );
		var format = grid.format || {
			to: Math.round
		};

		return scope_Target.appendChild(addMarking(
			spread,
			filter,
			format
		));
	}


	// Shorthand for base dimensions.
	function baseSize ( ) {
		var rect = scope_Base.getBoundingClientRect(), alt = 'offset' + ['Width', 'Height'][options.ort];
		return options.ort === 0 ? (rect.width||scope_Base[alt]) : (rect.height||scope_Base[alt]);
	}

	// Handler for attaching events trough a proxy.
	function attachEvent ( events, element, callback, data ) {

		// This function can be used to 'filter' events to the slider.
		// element is a node, not a nodeList

		var method = function ( e ){

			if ( scope_Target.hasAttribute('disabled') ) {
				return false;
			}

			// Stop if an active 'tap' transition is taking place.
			if ( hasClass(scope_Target, options.cssClasses.tap) ) {
				return false;
			}

			e = fixEvent(e, data.pageOffset);

			// Handle reject of multitouch
			if ( !e ) {
				return false;
			}

			// Ignore right or middle clicks on start #454
			if ( events === actions.start && e.buttons !== undefined && e.buttons > 1 ) {
				return false;
			}

			// Ignore right or middle clicks on start #454
			if ( data.hover && e.buttons ) {
				return false;
			}

			e.calcPoint = e.points[ options.ort ];

			// Call the event handler with the event [ and additional data ].
			callback ( e, data );
		};

		var methods = [];

		// Bind a closure on the target for every event type.
		events.split(' ').forEach(function( eventName ){
			element.addEventListener(eventName, method, false);
			methods.push([eventName, method]);
		});

		return methods;
	}

	// Provide a clean event with standardized offset values.
	function fixEvent ( e, pageOffset ) {

		// Prevent scrolling and panning on touch events, while
		// attempting to slide. The tap event also depends on this.
		e.preventDefault();

		// Filter the event to register the type, which can be
		// touch, mouse or pointer. Offset changes need to be
		// made on an event specific basis.
		var touch = e.type.indexOf('touch') === 0;
		var mouse = e.type.indexOf('mouse') === 0;
		var pointer = e.type.indexOf('pointer') === 0;
		var x;
		var y;

		// IE10 implemented pointer events with a prefix;
		if ( e.type.indexOf('MSPointer') === 0 ) {
			pointer = true;
		}

		if ( touch ) {

			// Fix bug when user touches with two or more fingers on mobile devices.
			// It's useful when you have two or more sliders on one page,
			// that can be touched simultaneously.
			// #649, #663, #668
			if ( e.touches.length > 1 ) {
				return false;
			}

			// noUiSlider supports one movement at a time,
			// so we can select the first 'changedTouch'.
			x = e.changedTouches[0].pageX;
			y = e.changedTouches[0].pageY;
		}

		pageOffset = pageOffset || getPageOffset();

		if ( mouse || pointer ) {
			x = e.clientX + pageOffset.x;
			y = e.clientY + pageOffset.y;
		}

		e.pageOffset = pageOffset;
		e.points = [x, y];
		e.cursor = mouse || pointer; // Fix #435

		return e;
	}

	// Translate a coordinate in the document to a percentage on the slider
	function calcPointToPercentage ( calcPoint ) {
		var location = calcPoint - offset(scope_Base, options.ort);
		var proposal = ( location * 100 ) / baseSize();
		return options.dir ? 100 - proposal : proposal;
	}

	// Find handle closest to a certain percentage on the slider
	function getClosestHandle ( proposal ) {

		var closest = 100;
		var handleNumber = false;

		scope_Handles.forEach(function(handle, index){

			// Disabled handles are ignored
			if ( handle.hasAttribute('disabled') ) {
				return;
			}

			var pos = Math.abs(scope_Locations[index] - proposal);

			if ( pos < closest ) {
				handleNumber = index;
				closest = pos;
			}
		});

		return handleNumber;
	}

	// Moves handle(s) by a percentage
	// (bool, % to move, [% where handle started, ...], [index in scope_Handles, ...])
	function moveHandles ( upward, proposal, locations, handleNumbers ) {

		var proposals = locations.slice();

		var b = [!upward, upward];
		var f = [upward, !upward];

		// Copy handleNumbers so we don't change the dataset
		handleNumbers = handleNumbers.slice();

		// Check to see which handle is 'leading'.
		// If that one can't move the second can't either.
		if ( upward ) {
			handleNumbers.reverse();
		}

		// Step 1: get the maximum percentage that any of the handles can move
		if ( handleNumbers.length > 1 ) {

			handleNumbers.forEach(function(handleNumber, o) {

				var to = checkHandlePosition(proposals, handleNumber, proposals[handleNumber] + proposal, b[o], f[o]);

				// Stop if one of the handles can't move.
				if ( to === false ) {
					proposal = 0;
				} else {
					proposal = to - proposals[handleNumber];
					proposals[handleNumber] = to;
				}
			});
		}

		// If using one handle, check backward AND forward
		else {
			b = f = [true];
		}

		var state = false;

		// Step 2: Try to set the handles with the found percentage
		handleNumbers.forEach(function(handleNumber, o) {
			state = setHandle(handleNumber, locations[handleNumber] + proposal, b[o], f[o]) || state;
		});

		// Step 3: If a handle moved, fire events
		if ( state ) {
			handleNumbers.forEach(function(handleNumber){
				fireEvent('update', handleNumber);
				fireEvent('slide', handleNumber);
			});
		}
	}

	// External event handling
	function fireEvent ( eventName, handleNumber, tap ) {

		Object.keys(scope_Events).forEach(function( targetEvent ) {

			var eventType = targetEvent.split('.')[0];

			if ( eventName === eventType ) {
				scope_Events[targetEvent].forEach(function( callback ) {

					callback.call(
						// Use the slider public API as the scope ('this')
						scope_Self,
						// Return values as array, so arg_1[arg_2] is always valid.
						scope_Values.map(options.format.to),
						// Handle index, 0 or 1
						handleNumber,
						// Unformatted slider values
						scope_Values.slice(),
						// Event is fired by tap, true or false
						tap || false,
						// Left offset of the handle, in relation to the slider
						scope_Locations.slice()
					);
				});
			}
		});
	}


	// Fire 'end' when a mouse or pen leaves the document.
	function documentLeave ( event, data ) {
		if ( event.type === "mouseout" && event.target.nodeName === "HTML" && event.relatedTarget === null ){
			eventEnd (event, data);
		}
	}

	// Handle movement on document for handle and range drag.
	function eventMove ( event, data ) {

		// Fix #498
		// Check value of .buttons in 'start' to work around a bug in IE10 mobile (data.buttonsProperty).
		// https://connect.microsoft.com/IE/feedback/details/927005/mobile-ie10-windows-phone-buttons-property-of-pointermove-event-always-zero
		// IE9 has .buttons and .which zero on mousemove.
		// Firefox breaks the spec MDN defines.
		if ( navigator.appVersion.indexOf("MSIE 9") === -1 && event.buttons === 0 && data.buttonsProperty !== 0 ) {
			return eventEnd(event, data);
		}

		// Check if we are moving up or down
		var movement = (options.dir ? -1 : 1) * (event.calcPoint - data.startCalcPoint);

		// Convert the movement into a percentage of the slider width/height
		var proposal = (movement * 100) / data.baseSize;

		moveHandles(movement > 0, proposal, data.locations, data.handleNumbers);
	}

	// Unbind move events on document, call callbacks.
	function eventEnd ( event, data ) {

		// The handle is no longer active, so remove the class.
		if ( scope_ActiveHandle ) {
			removeClass(scope_ActiveHandle, options.cssClasses.active);
			scope_ActiveHandle = false;
		}

		// Remove cursor styles and text-selection events bound to the body.
		if ( event.cursor ) {
			document.body.style.cursor = '';
			document.body.removeEventListener('selectstart', document.body.noUiListener);
		}

		// Unbind the move and end events, which are added on 'start'.
		document.documentElement.noUiListeners.forEach(function( c ) {
			document.documentElement.removeEventListener(c[0], c[1]);
		});

		// Remove dragging class.
		removeClass(scope_Target, options.cssClasses.drag);

		setZindex();

		data.handleNumbers.forEach(function(handleNumber){
			fireEvent('set', handleNumber);
			fireEvent('change', handleNumber);
			fireEvent('end', handleNumber);
		});
	}

	// Bind move events on document.
	function eventStart ( event, data ) {

		if ( data.handleNumbers.length === 1 ) {

			var handle = scope_Handles[data.handleNumbers[0]];

			// Ignore 'disabled' handles
			if ( handle.hasAttribute('disabled') ) {
				return false;
			}

			// Mark the handle as 'active' so it can be styled.
			scope_ActiveHandle = handle.children[0];
			addClass(scope_ActiveHandle, options.cssClasses.active);
		}

		// Fix #551, where a handle gets selected instead of dragged.
		event.preventDefault();

		// A drag should never propagate up to the 'tap' event.
		event.stopPropagation();

		// Attach the move and end events.
		var moveEvent = attachEvent(actions.move, document.documentElement, eventMove, {
			startCalcPoint: event.calcPoint,
			baseSize: baseSize(),
			pageOffset: event.pageOffset,
			handleNumbers: data.handleNumbers,
			buttonsProperty: event.buttons,
			locations: scope_Locations.slice()
		});

		var endEvent = attachEvent(actions.end, document.documentElement, eventEnd, {
			handleNumbers: data.handleNumbers
		});

		var outEvent = attachEvent("mouseout", document.documentElement, documentLeave, {
			handleNumbers: data.handleNumbers
		});

		document.documentElement.noUiListeners = moveEvent.concat(endEvent, outEvent);

		// Text selection isn't an issue on touch devices,
		// so adding cursor styles can be skipped.
		if ( event.cursor ) {

			// Prevent the 'I' cursor and extend the range-drag cursor.
			document.body.style.cursor = getComputedStyle(event.target).cursor;

			// Mark the target with a dragging state.
			if ( scope_Handles.length > 1 ) {
				addClass(scope_Target, options.cssClasses.drag);
			}

			var f = function(){
				return false;
			};

			document.body.noUiListener = f;

			// Prevent text selection when dragging the handles.
			document.body.addEventListener('selectstart', f, false);
		}

		data.handleNumbers.forEach(function(handleNumber){
			fireEvent('start', handleNumber);
		});
	}

	// Move closest handle to tapped location.
	function eventTap ( event ) {

		// The tap event shouldn't propagate up
		event.stopPropagation();

		var proposal = calcPointToPercentage(event.calcPoint);
		var handleNumber = getClosestHandle(proposal);

		// Tackle the case that all handles are 'disabled'.
		if ( handleNumber === false ) {
			return false;
		}

		// Flag the slider as it is now in a transitional state.
		// Transition takes a configurable amount of ms (default 300). Re-enable the slider after that.
		if ( !options.events.snap ) {
			addClassFor(scope_Target, options.cssClasses.tap, options.animationDuration);
		}

		setHandle(handleNumber, proposal, true, true);

		setZindex();

		fireEvent('slide', handleNumber, true);
		fireEvent('set', handleNumber, true);
		fireEvent('change', handleNumber, true);
		fireEvent('update', handleNumber, true);

		if ( options.events.snap ) {
			eventStart(event, { handleNumbers: [handleNumber] });
		}
	}

	// Fires a 'hover' event for a hovered mouse/pen position.
	function eventHover ( event ) {

		var proposal = calcPointToPercentage(event.calcPoint);

		var to = scope_Spectrum.getStep(proposal);
		var value = scope_Spectrum.fromStepping(to);

		Object.keys(scope_Events).forEach(function( targetEvent ) {
			if ( 'hover' === targetEvent.split('.')[0] ) {
				scope_Events[targetEvent].forEach(function( callback ) {
					callback.call( scope_Self, value );
				});
			}
		});
	}

	// Attach events to several slider parts.
	function bindSliderEvents ( behaviour ) {

		// Attach the standard drag event to the handles.
		if ( !behaviour.fixed ) {

			scope_Handles.forEach(function( handle, index ){

				// These events are only bound to the visual handle
				// element, not the 'real' origin element.
				attachEvent ( actions.start, handle.children[0], eventStart, {
					handleNumbers: [index]
				});
			});
		}

		// Attach the tap event to the slider base.
		if ( behaviour.tap ) {
			attachEvent (actions.start, scope_Base, eventTap, {});
		}

		// Fire hover events
		if ( behaviour.hover ) {
			attachEvent (actions.move, scope_Base, eventHover, { hover: true });
		}

		// Make the range draggable.
		if ( behaviour.drag ){

			scope_Connects.forEach(function( connect, index ){

				if ( connect === false || index === 0 || index === scope_Connects.length - 1 ) {
					return;
				}

				var handleBefore = scope_Handles[index - 1];
				var handleAfter = scope_Handles[index];
				var eventHolders = [connect];

				addClass(connect, options.cssClasses.draggable);

				// When the range is fixed, the entire range can
				// be dragged by the handles. The handle in the first
				// origin will propagate the start event upward,
				// but it needs to be bound manually on the other.
				if ( behaviour.fixed ) {
					eventHolders.push(handleBefore.children[0]);
					eventHolders.push(handleAfter.children[0]);
				}

				eventHolders.forEach(function( eventHolder ) {
					attachEvent ( actions.start, eventHolder, eventStart, {
						handles: [handleBefore, handleAfter],
						handleNumbers: [index - 1, index]
					});
				});
			});
		}
	}


	// Split out the handle positioning logic so the Move event can use it, too
	function checkHandlePosition ( reference, handleNumber, to, lookBackward, lookForward ) {

		// For sliders with multiple handles, limit movement to the other handle.
		// Apply the margin option by adding it to the handle positions.
		if ( scope_Handles.length > 1 ) {

			if ( lookBackward && handleNumber > 0 ) {
				to = Math.max(to, reference[handleNumber - 1] + options.margin);
			}

			if ( lookForward && handleNumber < scope_Handles.length - 1 ) {
				to = Math.min(to, reference[handleNumber + 1] - options.margin);
			}
		}

		// The limit option has the opposite effect, limiting handles to a
		// maximum distance from another. Limit must be > 0, as otherwise
		// handles would be unmoveable.
		if ( scope_Handles.length > 1 && options.limit ) {

			if ( lookBackward && handleNumber > 0 ) {
				to = Math.min(to, reference[handleNumber - 1] + options.limit);
			}

			if ( lookForward && handleNumber < scope_Handles.length - 1 ) {
				to = Math.max(to, reference[handleNumber + 1] - options.limit);
			}
		}

		// The padding option keeps the handles a certain distance from the
		// edges of the slider. Padding must be > 0.
		if ( options.padding ) {

			if ( handleNumber === 0 ) {
				to = Math.max(to, options.padding);
			}

			if ( handleNumber === scope_Handles.length - 1 ) {
				to = Math.min(to, 100 - options.padding);
			}
		}

		to = scope_Spectrum.getStep(to);

		// Limit percentage to the 0 - 100 range
		to = limit(to);

		// Return false if handle can't move
		if ( to === reference[handleNumber] ) {
			return false;
		}

		return to;
	}

	function toPct ( pct ) {
		return pct + '%';
	}

	// Updates scope_Locations and scope_Values, updates visual state
	function updateHandlePosition ( handleNumber, to ) {

		// Update locations.
		scope_Locations[handleNumber] = to;

		// Convert the value to the slider stepping/range.
		scope_Values[handleNumber] = scope_Spectrum.fromStepping(to);

		// Called synchronously or on the next animationFrame
		var stateUpdate = function() {
			scope_Handles[handleNumber].style[options.style] = toPct(to);
			updateConnect(handleNumber);
			updateConnect(handleNumber + 1);
		};

		// Set the handle to the new position.
		// Use requestAnimationFrame for efficient painting.
		// No significant effect in Chrome, Edge sees dramatic performace improvements.
		// Option to disable is useful for unit tests, and single-step debugging.
		if ( window.requestAnimationFrame && options.useRequestAnimationFrame ) {
			window.requestAnimationFrame(stateUpdate);
		} else {
			stateUpdate();
		}
	}

	function setZindex ( ) {

		scope_HandleNumbers.forEach(function(handleNumber){
			// Handles before the slider middle are stacked later = higher,
			// Handles after the middle later is lower
			// [[7] [8] .......... | .......... [5] [4]
			var dir = (scope_Locations[handleNumber] > 50 ? -1 : 1);
			var zIndex = 3 + (scope_Handles.length + (dir * handleNumber));
			scope_Handles[handleNumber].childNodes[0].style.zIndex = zIndex;
		});
	}

	// Test suggested values and apply margin, step.
	function setHandle ( handleNumber, to, lookBackward, lookForward ) {

		to = checkHandlePosition(scope_Locations, handleNumber, to, lookBackward, lookForward);

		if ( to === false ) {
			return false;
		}

		updateHandlePosition(handleNumber, to);

		return true;
	}

	// Updates style attribute for connect nodes
	function updateConnect ( index ) {

		// Skip connects set to false
		if ( !scope_Connects[index] ) {
			return;
		}

		var l = 0;
		var h = 100;

		if ( index !== 0 ) {
			l = scope_Locations[index - 1];
		}

		if ( index !== scope_Connects.length - 1 ) {
			h = scope_Locations[index];
		}

		scope_Connects[index].style[options.style] = toPct(l);
		scope_Connects[index].style[options.styleOposite] = toPct(100 - h);
	}

	// ...
	function setValue ( to, handleNumber ) {

		// Setting with null indicates an 'ignore'.
		// Inputting 'false' is invalid.
		if ( to === null || to === false ) {
			return;
		}

		// If a formatted number was passed, attemt to decode it.
		if ( typeof to === 'number' ) {
			to = String(to);
		}

		to = options.format.from(to);

		// Request an update for all links if the value was invalid.
		// Do so too if setting the handle fails.
		if ( to !== false && !isNaN(to) ) {
			setHandle(handleNumber, scope_Spectrum.toStepping(to), false, false);
		}
	}

	// Set the slider value.
	function valueSet ( input, fireSetEvent ) {

		var values = asArray(input);
		var isInit = scope_Locations[0] === undefined;

		// Event fires by default
		fireSetEvent = (fireSetEvent === undefined ? true : !!fireSetEvent);

		values.forEach(setValue);

		// Animation is optional.
		// Make sure the initial values were set before using animated placement.
		if ( options.animate && !isInit ) {
			addClassFor(scope_Target, options.cssClasses.tap, options.animationDuration);
		}

		// Now that all base values are set, apply constraints
		scope_HandleNumbers.forEach(function(handleNumber){
			setHandle(handleNumber, scope_Locations[handleNumber], true, false);
		});

		setZindex();

		scope_HandleNumbers.forEach(function(handleNumber){

			fireEvent('update', handleNumber);

			// Fire the event only for handles that received a new value, as per #579
			if ( values[handleNumber] !== null && fireSetEvent ) {
				fireEvent('set', handleNumber);
			}
		});
	}

	// Reset slider to initial values
	function valueReset ( fireSetEvent ) {
		valueSet(options.start, fireSetEvent);
	}

	// Get the slider value.
	function valueGet ( ) {

		var values = scope_Values.map(options.format.to);

		// If only one handle is used, return a single value.
		if ( values.length === 1 ){
			return values[0];
		}

		return values;
	}

	// Removes classes from the root and empties it.
	function destroy ( ) {

		for ( var key in options.cssClasses ) {
			if ( !options.cssClasses.hasOwnProperty(key) ) { continue; }
			removeClass(scope_Target, options.cssClasses[key]);
		}

		while (scope_Target.firstChild) {
			scope_Target.removeChild(scope_Target.firstChild);
		}

		delete scope_Target.noUiSlider;
	}

	// Get the current step size for the slider.
	function getCurrentStep ( ) {

		// Check all locations, map them to their stepping point.
		// Get the step point, then find it in the input list.
		return scope_Locations.map(function( location, index ){

			var nearbySteps = scope_Spectrum.getNearbySteps( location );
			var value = scope_Values[index];
			var increment = nearbySteps.thisStep.step;
			var decrement = null;

			// If the next value in this step moves into the next step,
			// the increment is the start of the next step - the current value
			if ( increment !== false ) {
				if ( value + increment > nearbySteps.stepAfter.startValue ) {
					increment = nearbySteps.stepAfter.startValue - value;
				}
			}


			// If the value is beyond the starting point
			if ( value > nearbySteps.thisStep.startValue ) {
				decrement = nearbySteps.thisStep.step;
			}

			else if ( nearbySteps.stepBefore.step === false ) {
				decrement = false;
			}

			// If a handle is at the start of a step, it always steps back into the previous step first
			else {
				decrement = value - nearbySteps.stepBefore.highestStep;
			}


			// Now, if at the slider edges, there is not in/decrement
			if ( location === 100 ) {
				increment = null;
			}

			else if ( location === 0 ) {
				decrement = null;
			}

			// As per #391, the comparison for the decrement step can have some rounding issues.
			var stepDecimals = scope_Spectrum.countStepDecimals();

			// Round per #391
			if ( increment !== null && increment !== false ) {
				increment = Number(increment.toFixed(stepDecimals));
			}

			if ( decrement !== null && decrement !== false ) {
				decrement = Number(decrement.toFixed(stepDecimals));
			}

			return [decrement, increment];
		});
	}

	// Attach an event to this slider, possibly including a namespace
	function bindEvent ( namespacedEvent, callback ) {
		scope_Events[namespacedEvent] = scope_Events[namespacedEvent] || [];
		scope_Events[namespacedEvent].push(callback);

		// If the event bound is 'update,' fire it immediately for all handles.
		if ( namespacedEvent.split('.')[0] === 'update' ) {
			scope_Handles.forEach(function(a, index){
				fireEvent('update', index);
			});
		}
	}

	// Undo attachment of event
	function removeEvent ( namespacedEvent ) {

		var event = namespacedEvent && namespacedEvent.split('.')[0];
		var namespace = event && namespacedEvent.substring(event.length);

		Object.keys(scope_Events).forEach(function( bind ){

			var tEvent = bind.split('.')[0],
				tNamespace = bind.substring(tEvent.length);

			if ( (!event || event === tEvent) && (!namespace || namespace === tNamespace) ) {
				delete scope_Events[bind];
			}
		});
	}

	// Updateable: margin, limit, padding, step, range, animate, snap
	function updateOptions ( optionsToUpdate, fireSetEvent ) {

		// Spectrum is created using the range, snap, direction and step options.
		// 'snap' and 'step' can be updated, 'direction' cannot, due to event binding.
		// If 'snap' and 'step' are not passed, they should remain unchanged.
		var v = valueGet();

		var updateAble = ['margin', 'limit', 'padding', 'range', 'animate', 'snap', 'step', 'format'];

		// Only change options that we're actually passed to update.
		updateAble.forEach(function(name){
			if ( optionsToUpdate[name] !== undefined ) {
				originalOptions[name] = optionsToUpdate[name];
			}
		});

		var newOptions = testOptions(originalOptions);

		// Load new options into the slider state
		updateAble.forEach(function(name){
			if ( optionsToUpdate[name] !== undefined ) {
				options[name] = newOptions[name];
			}
		});

		// Save current spectrum direction as testOptions in testRange call
		// doesn't rely on current direction
		newOptions.spectrum.direction = scope_Spectrum.direction;
		scope_Spectrum = newOptions.spectrum;

		// Limit, margin and padding depend on the spectrum but are stored outside of it. (#677)
		options.margin = newOptions.margin;
		options.limit = newOptions.limit;
		options.padding = newOptions.padding;

		// Invalidate the current positioning so valueSet forces an update.
		scope_Locations = [];
		valueSet(optionsToUpdate.start || v, fireSetEvent);
	}

	// Throw an error if the slider was already initialized.
	if ( scope_Target.noUiSlider ) {
		throw new Error("noUiSlider (" + VERSION + "): Slider was already initialized.");
	}

	// Create the base element, initialise HTML and set classes.
	// Add handles and connect elements.
	addSlider(scope_Target);
	addElements(options.connect, scope_Base);

	scope_Self = {
		destroy: destroy,
		steps: getCurrentStep,
		on: bindEvent,
		off: removeEvent,
		get: valueGet,
		set: valueSet,
		reset: valueReset,
		// Exposed for unit testing, don't use this in your application.
		__moveHandles: function(a, b, c) { moveHandles(a, b, scope_Locations, c); },
		options: originalOptions, // Issue #600, #678
		updateOptions: updateOptions,
		target: scope_Target, // Issue #597
		pips: pips // Issue #594
	};

	// Attach user events.
	bindSliderEvents(options.events);

	// Use the public value method to set the start values.
	valueSet(options.start);

	if ( options.pips ) {
		pips(options.pips);
	}

	if ( options.tooltips ) {
		tooltips();
	}

	return scope_Self;

}


	// Run the standard initializer
	function initialize ( target, originalOptions ) {

		if ( !target.nodeName ) {
			throw new Error("noUiSlider (" + VERSION + "): create requires a single element.");
		}

		// Test the options and create the slider environment;
		var options = testOptions( originalOptions, target );
		var api = closure( target, options, originalOptions );

		target.noUiSlider = api;

		return api;
	}

	// Use an object instead of a function for future expansibility;
	return {
		version: VERSION,
		create: initialize
	};

}));

/***/ }),

/***/ "./src/app/forms/equal-validator.directive.ts":
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

/***/ "./src/app/forms/extendedforms/extendedforms.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"main-content\">\n    <div class=\"container-fluid\">\n        <div class=\"card\">\n            <div class=\"card-content\">\n                <div class=\"row\">\n                    <div class=\"col-md-4\">\n                        <h4 class=\"card-title\">Datetime Picker</h4>\n                        <div class=\"form-group\">\n                            <input type=\"text\" class=\"form-control datetimepicker\" placeholder=\"Datetime Picker Here\"/>\n                        </div>\n                    </div>\n                    <div class=\"col-md-4\">\n                        <h4 class=\"card-title\">Date Picker</h4>\n                        <div class=\"form-group\">\n                            <input type=\"text\" class=\"form-control datepicker\" placeholder=\"Date Picker Here\"/>\n                        </div>\n                    </div>\n                    <div class=\"col-md-4\">\n                        <h4 class=\"card-title\">Time Picker</h4>\n                        <div class=\"form-group\">\n                            <input type=\"text\" class=\"form-control timepicker\" placeholder=\"Time Picker Here\"/>\n                        </div>\n                    </div>\n                </div>\n\n                <br/><br/>\n\n                <div class=\"row\">\n                    <div class=\"col-md-6\">\n                        <h4 class=\"card-title\">Switches</h4>\n                        <div class=\"col-md-4\">\n                            <p class=\"category\">Default</p>\n\n                            <input type=\"checkbox\" class=\"switch\" checked>\n                            <input type=\"checkbox\" class=\"switch\">\n                        </div>\n                        <div class=\"col-md-4\">\n                            <p class=\"category\">Plain</p>\n                            <input type=\"checkbox\" class=\"switch-plain\" checked>\n                            <input type=\"checkbox\" class=\"switch-plain\">\n                        </div>\n                        <div class=\"col-md-4\">\n                            <p class=\"category\">With Icons</p>\n                            <input type=\"checkbox\" class=\"switch-icon\" checked>\n                            <input type=\"checkbox\" class=\"switch-icon\">\n                        </div>\n                    </div>\n                    <div class=\"col-md-3\">\n                        <h4 class=\"card-title\">Checkboxes</h4>\n                        <div class=\"checkbox\">\n                            <input id=\"checkbox1\" type=\"checkbox\">\n                            <label for=\"checkbox1\">\n                                Unchecked\n                            </label>\n                        </div>\n                        <div class=\"checkbox\">\n                           <input id=\"checkbox2\" type=\"checkbox\" checked=\"\">\n                           <label for=\"checkbox2\">\n                                Checked\n                            </label>\n                         </div>\n                        <div class=\"checkbox\">\n                            <input id=\"checkbox3\" type=\"checkbox\" disabled=\"\">\n                            <label for=\"checkbox3\">\n                                Disabled unchecked\n                            </label>\n                        </div>\n                        <div class=\"checkbox\">\n                           <input id=\"checkbox4\" type=\"checkbox\" checked=\"\" disabled=\"\">\n                           <label for=\"checkbox4\">\n                               Disabled checked\n                           </label>\n                        </div>\n                    </div>\n                    <div class=\"col-sm-3\">\n                        <h4 class=\"card-title\">Radio</h4>\n                        <div class=\"radio\">\n                            <input type=\"radio\" name=\"radio1\" id=\"radio1\" value=\"option1\">\n                            <label for=\"radio1\">\n                                Radio is off\n                            </label>\n                        </div>\n\n                        <div class=\"radio\">\n                            <input type=\"radio\" name=\"radio1\" id=\"radio2\" value=\"option2\" checked=\"\">\n                            <label for=\"radio2\">\n                                Radio is on\n                            </label>\n                        </div>\n\n                        <div class=\"radio\">\n                            <input type=\"radio\" name=\"radio3\" id=\"radio3\" value=\"option1\" disabled=\"\">\n                            <label for=\"radio3\">\n                                Disabled radio is off\n                            </label>\n                        </div>\n\n                        <div class=\"radio\">\n                            <input type=\"radio\" name=\"radio4\" id=\"radio4\" value=\"option1\" checked=\"\" disabled=\"\">\n                            <label for=\"radio4\">\n                                Disabled radio is on\n                            </label>\n                        </div>\n                    </div>\n                </div>\n\n                <br/><br/>\n\n                <div class=\"row\">\n                    <div class=\"col-md-6\">\n                        <h4 class=\"card-title\">Tags</h4>\n\n                        <input type=\"text\" value=\"Minimal, Light, New, Friends\" class=\"tagsinput\" data-role=\"tagsinput\" data-color=\"success\"/>\n\n                        <!-- You can change data-color=\"success\" with one of our colors primary | warning | info | danger -->\n                        <br />\n                        <h4 class=\"card-title\">Progress Bars</h4>\n                        <div class=\"progress\">\n                            <div class=\"progress-bar\" role=\"progressbar\" aria-valuenow=\"60\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 30%;\">\n                                <span class=\"sr-only\">60% Complete</span>\n                            </div>\n                        </div>\n                        <div class=\"progress\">\n                            <div class=\"progress-bar progress-bar-info\" role=\"progressbar\" aria-valuenow=\"60\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 60%;\">\n                                <span class=\"sr-only\">60% Complete</span>\n                            </div>\n                        </div>\n                        <div class=\"progress\">\n                            <div class=\"progress-bar progress-bar-success\" style=\"width: 35%\">\n                                <span class=\"sr-only\">35% Complete (success)</span>\n                            </div>\n                            <div class=\"progress-bar progress-bar-warning\" style=\"width: 20%\">\n                                <span class=\"sr-only\">20% Complete (warning)</span>\n                            </div>\n                            <div class=\"progress-bar progress-bar-danger\" style=\"width: 10%\">\n                                <span class=\"sr-only\">10% Complete (danger)</span>\n                            </div>\n                        </div>\n\n                        <br />\n                        <h4 class=\"card-title\">Sliders</h4>\n                        <div id=\"sliderRegular\" class=\"slider slider-success\"></div>\n                            <br>\n                        <div id=\"sliderDouble\" class=\"slider slider-info\"></div>\n                    </div>\n                    <div class=\"col-md-6\">\n                        <h4 class=\"card-title\">Customisable Select</h4>\n                        <div class=\"row\">\n                            <div class=\"col-sm-6\">\n                                <select class=\"selectpicker\" data-style=\"btn btn-danger btn-block\" title=\"Single Select\" data-size=\"7\">\n                                    <option value=\"id\">Bahasa Indonesia</option>\n                                    <option value=\"ms\">Bahasa Melayu</option>\n                                    <option value=\"ca\">Català</option>\n                                    <option value=\"da\">Dansk</option>\n                                    <option value=\"de\">Deutsch</option>\n                                    <option value=\"en\">English</option>\n                                    <option value=\"es\">Español</option>\n                                    <option value=\"el\">Eλληνικά</option>\n                                    <option value=\"fr\">Français</option>\n                                    <option value=\"it\">Italiano</option>\n                                    <option value=\"hu\">Magyar</option>\n                                    <option value=\"nl\">Nederlands</option>\n                                    <option value=\"no\">Norsk</option>\n                                    <option value=\"pl\">Polski</option>\n                                    <option value=\"pt\">Português</option>\n                                    <option value=\"fi\">Suomi</option>\n                                    <option value=\"sv\">Svenska</option>\n                                    <option value=\"tr\">Türkçe</option>\n                                    <option value=\"is\">Íslenska</option>\n                                    <option value=\"cs\">Čeština</option>\n                                    <option value=\"ru\">Русский</option>\n                                    <option value=\"th\">ภาษาไทย</option>\n                                    <option value=\"zh\">中文 (简体)</option>\n                                    <option value=\"zh-TW\">中文 (繁體)</option>\n                                    <option value=\"ja\">日本語</option>\n                                    <option value=\"ko\">한국어</option>\n                                </select>\n                            </div>\n                            <div class=\"col-sm-6\">\n                                <select multiple title=\"Multiple Select\" class=\"selectpicker\" data-style=\"btn-info btn-fill btn-block\" data-size=\"7\">\n                                    <option value=\"ARS\">ARS</option>\n                                    <option value=\"AUD\">AUD</option>\n                                    <option value=\"BRL\">BRL</option>\n                                    <option value=\"CAD\">CAD</option>\n                                    <option value=\"CHF\">CHF</option>\n                                    <option value=\"CNY\">CNY</option>\n                                    <option value=\"CZK\">CZK</option>\n                                    <option value=\"DKK\">DKK</option>\n                                    <option value=\"EUR\">EUR</option>\n                                    <option value=\"GBP\">GBP</option>\n                                    <option value=\"HKD\">HKD</option>\n                                    <option value=\"HUF\">HUF</option>\n                                    <option value=\"IDR\">IDR</option>\n                                    <option value=\"ILS\">ILS</option>\n                                    <option value=\"INR\">INR</option>\n                                    <option value=\"JPY\">JPY</option>\n                                    <option value=\"KRW\">KRW</option>\n                                    <option value=\"MYR\">MYR</option>\n                                    <option value=\"MXN\">MXN</option>\n                                    <option value=\"NOK\">NOK</option>\n                                    <option value=\"NZD\">NZD</option>\n                                    <option value=\"PHP\">PHP</option>\n                                    <option value=\"PLN\">PLN</option>\n                                    <option value=\"RUB\">RUB</option>\n                                    <option value=\"SEK\">SEK</option>\n                                    <option value=\"SGD\">SGD</option>\n                                    <option value=\"TWD\">TWD</option>\n                                    <option value=\"USD\">USD</option>\n                                    <option value=\"VND\">VND</option>\n                                    <option value=\"ZAR\">ZAR</option>\n                                </select>\n                            </div>\n                            <div class=\"col-sm-6\">\n                                <br />\n                                <h4 class=\"card-title\">Dropdown</h4>\n                                <div class=\"dropdown\">\n                                      <button href=\"#\" class=\"btn btn-block dropdown-toggle\" data-toggle=\"dropdown\">\n                                          Regular\n                                          <b class=\"caret\"></b>\n                                      </button>\n                                      <ul class=\"dropdown-menu\">\n                                        <li><a href=\"#\">Action</a></li>\n                                        <li><a href=\"#\">Another action</a></li>\n                                        <li><a href=\"#\">Something else here</a></li>\n                                        <li class=\"divider\"></li>\n                                        <li><a href=\"#\">Separated link</a></li>\n                                        <li class=\"divider\"></li>\n                                        <li><a href=\"#\">One more separated link</a></li>\n                                      </ul>\n                                </div>\n                            </div>\n                            <div class=\"col-sm-6\">\n                                <br />\n                                <h4 class=\"card-title\">Dropup</h4>\n                                <div class=\"dropup\">\n                                      <button href=\"#\" class=\"btn btn-block dropdown-toggle\" data-toggle=\"dropdown\">\n                                          Regular\n                                          <b class=\"caret\"></b>\n                                      </button>\n                                      <ul class=\"dropdown-menu\">\n                                        <li><a href=\"#\">Action</a></li>\n                                        <li><a href=\"#\">Another action</a></li>\n                                        <li><a href=\"#\">Something else here</a></li>\n                                        <li class=\"divider\"></li>\n                                        <li><a href=\"#\">Separated link</a></li>\n                                        <li class=\"divider\"></li>\n                                        <li><a href=\"#\">One more separated link</a></li>\n                                      </ul>\n                                </div>\n                            </div>\n                        </div>\n                    </div>\n                </div>\n\n                <br/><br/>\n            </div>\n        </div> <!-- end card -->\n    </div>\n</div>\n"

/***/ }),

/***/ "./src/app/forms/extendedforms/extendedforms.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ExtendedFormsComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var ExtendedFormsComponent = /** @class */ (function () {
    function ExtendedFormsComponent() {
    }
    ExtendedFormsComponent.prototype.ngOnInit = function () {
        //  Activate the tooltips
        $('[rel="tooltip"]').tooltip();
        $('.switch').bootstrapSwitch({
            onColor: 'primary'
        });
        $('.switch-plain').bootstrapSwitch({
            onColor: '',
            onText: '',
            offText: ''
        });
        $('.switch-icon').bootstrapSwitch({
            onColor: '',
            onText: '<i class="fa fa-check"></i>',
            offText: '<i class="fa fa-times"></i>'
        });
        // // Init Tags Input
        if ($(".tagsinput").length != 0) {
            $(".tagsinput").tagsinput();
        }
        //  Init Bootstrap Select Picker
        if ($(".selectpicker").length != 0) {
            $(".selectpicker").selectpicker({
                iconBase: "ti",
                tickIcon: "ti-check"
            });
        }
        $('.datetimepicker').datetimepicker({
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
        $('.datepicker').datetimepicker({
            format: 'MM/DD/YYYY',
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
        $('.timepicker').datetimepicker({
            //          format: 'H:mm',    // use this format if you want the 24hours timepicker
            format: 'h:mm A',
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
        var noUiSlider = __webpack_require__("./node_modules/nouislider/distribute/nouislider.js");
        var sliderRegular = document.getElementById('sliderRegular');
        noUiSlider.create(sliderRegular, {
            start: 40,
            connect: [true, false],
            range: {
                min: 0,
                max: 100
            }
        });
        var sliderDouble = document.getElementById('sliderDouble');
        noUiSlider.create(sliderDouble, {
            start: [20, 60],
            connect: true,
            range: {
                min: 0,
                max: 100
            }
        });
    };
    ExtendedFormsComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'extendedforms-cmp',
            template: __webpack_require__("./src/app/forms/extendedforms/extendedforms.component.html")
        })
    ], ExtendedFormsComponent);
    return ExtendedFormsComponent;
}());



/***/ }),

/***/ "./src/app/forms/forms.module.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "Forms", function() { return Forms; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__("./node_modules/@angular/router/esm5/router.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__("./node_modules/@angular/common/esm5/common.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_forms__ = __webpack_require__("./node_modules/@angular/forms/esm5/forms.js");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__equal_validator_directive__ = __webpack_require__("./src/app/forms/equal-validator.directive.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__forms_routing__ = __webpack_require__("./src/app/forms/forms.routing.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__extendedforms_extendedforms_component__ = __webpack_require__("./src/app/forms/extendedforms/extendedforms.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__regularforms_regularforms_component__ = __webpack_require__("./src/app/forms/regularforms/regularforms.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__validationforms_validationforms_component__ = __webpack_require__("./src/app/forms/validationforms/validationforms.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__wizard_wizard_component__ = __webpack_require__("./src/app/forms/wizard/wizard.component.ts");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};










var Forms = /** @class */ (function () {
    function Forms() {
    }
    Forms = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["L" /* NgModule */])({
            imports: [
                __WEBPACK_IMPORTED_MODULE_2__angular_common__["b" /* CommonModule */],
                __WEBPACK_IMPORTED_MODULE_1__angular_router__["d" /* RouterModule */].forChild(__WEBPACK_IMPORTED_MODULE_5__forms_routing__["a" /* FormsRoutes */]),
                __WEBPACK_IMPORTED_MODULE_3__angular_forms__["c" /* FormsModule */],
                __WEBPACK_IMPORTED_MODULE_3__angular_forms__["h" /* ReactiveFormsModule */]
            ],
            declarations: [
                __WEBPACK_IMPORTED_MODULE_6__extendedforms_extendedforms_component__["a" /* ExtendedFormsComponent */],
                __WEBPACK_IMPORTED_MODULE_7__regularforms_regularforms_component__["a" /* RegularFormsComponent */],
                __WEBPACK_IMPORTED_MODULE_8__validationforms_validationforms_component__["a" /* ValidationFormsComponent */],
                __WEBPACK_IMPORTED_MODULE_9__wizard_wizard_component__["a" /* WizardComponent */],
                __WEBPACK_IMPORTED_MODULE_4__equal_validator_directive__["a" /* EqualValidator */]
            ]
        })
    ], Forms);
    return Forms;
}());



/***/ }),

/***/ "./src/app/forms/forms.routing.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return FormsRoutes; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__extendedforms_extendedforms_component__ = __webpack_require__("./src/app/forms/extendedforms/extendedforms.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__regularforms_regularforms_component__ = __webpack_require__("./src/app/forms/regularforms/regularforms.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__validationforms_validationforms_component__ = __webpack_require__("./src/app/forms/validationforms/validationforms.component.ts");
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__wizard_wizard_component__ = __webpack_require__("./src/app/forms/wizard/wizard.component.ts");




var FormsRoutes = [
    {
        path: '',
        children: [{
                path: 'regular',
                component: __WEBPACK_IMPORTED_MODULE_1__regularforms_regularforms_component__["a" /* RegularFormsComponent */]
            }]
    }, {
        path: '',
        children: [{
                path: 'extended',
                component: __WEBPACK_IMPORTED_MODULE_0__extendedforms_extendedforms_component__["a" /* ExtendedFormsComponent */]
            }]
    }, {
        path: '',
        children: [{
                path: 'validation',
                component: __WEBPACK_IMPORTED_MODULE_2__validationforms_validationforms_component__["a" /* ValidationFormsComponent */]
            }]
    }, {
        path: '',
        children: [{
                path: 'wizard',
                component: __WEBPACK_IMPORTED_MODULE_3__wizard_wizard_component__["a" /* WizardComponent */]
            }]
    }
];


/***/ }),

/***/ "./src/app/forms/regularforms/regularforms.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"main-content\">\n    <div class=\"container-fluid\">\n        <div class=\"row\">\n            <div class=\"col-md-6\">\n                <div class=\"card\">\n                    <form method=\"#\" action=\"#\">\n                        <div class=\"card-header\">\n                            <h4 class=\"card-title\">\n                                Stacked Form\n                            </h4>\n                        </div>\n                        <div class=\"card-content\">\n                            <div class=\"form-group\">\n                                <label>Email address</label>\n                                <input type=\"email\" placeholder=\"Enter email\" class=\"form-control\">\n                            </div>\n                            <div class=\"form-group\">\n                                <label>Password</label>\n                                <input type=\"password\" placeholder=\"Password\" class=\"form-control\">\n                            </div>\n                            <div class=\"form-group\">\n                                <label class=\"checkbox\">\n                                    <input type=\"checkbox\" data-toggle=\"checkbox\" value=\"\" checked>\n                                    Subscribe to newsletter\n                                </label>\n                            </div>\n                            <button type=\"submit\" class=\"btn btn-fill btn-info\">Submit</button>\n                        </div>\n                    </form>\n                </div> <!-- end card -->\n            </div> <!--  end col-md-6  -->\n            <div class=\"col-md-6\">\n                <div class=\"card\">\n                    <form class=\"form-horizontal\">\n                        <div class=\"card-header\">\n                            <h4 class=\"card-title\">\n                                Horizontal Form\n                            </h4>\n                        </div>\n                        <div class=\"card-content\">\n                            <div class=\"form-group\">\n                                <label class=\"col-md-3 control-label\">Email</label>\n                                <div class=\"col-md-9\">\n                                    <input type=\"email\" placeholder=\"Email\" class=\"form-control\">\n                                </div>\n                            </div>\n                            <div class=\"form-group\">\n                                <label class=\"col-md-3 control-label\">Password</label>\n                                <div class=\"col-md-9\">\n                                    <input type=\"password\" placeholder=\"Password\" class=\"form-control\">\n                                </div>\n                            </div>\n                            <div class=\"form-group\">\n                                <label class=\"col-md-3\"></label>\n                                <div class=\"col-md-9\">\n                                    <label class=\"checkbox\">\n                                        <input type=\"checkbox\" data-toggle=\"checkbox\" value=\"\">\n                                        Remember Me\n                                    </label>\n                                </div>\n                            </div>\n                        </div>\n                        <div class=\"card-footer\">\n                            <div class=\"form-group\">\n                                <label class=\"col-md-3\"></label>\n                                <div class=\"col-md-9\">\n                                    <button type=\"submit\" class=\"btn btn-fill btn-info\">\n                                        Sign in\n                                    </button>\n                                </div>\n                            </div>\n                        </div>\n                    </form>\n                </div> <!-- end card -->\n            </div> <!--  end col-md-6  -->\n            <div class=\"col-md-12\">\n                <div class=\"card\">\n                    <div class=\"card-header\">\n                        <h4 class=\"card-title\">Form Elements</h4>\n                    </div>\n                    <div class=\"card-content\">\n                        <form method=\"get\" action=\"/\" class=\"form-horizontal\">\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">With help</label>\n                                    <div class=\"col-sm-10\">\n                                        <input type=\"text\" class=\"form-control\">\n                                        <span class=\"help-block\">A block of help text that breaks onto a new line.</span>\n                                    </div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">Password</label>\n                                    <div class=\"col-sm-10\">\n                                        <input type=\"password\" name=\"password\" class=\"form-control\">\n                                    </div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">Placeholder</label>\n                                    <div class=\"col-sm-10\">\n                                        <input type=\"text\" placeholder=\"placeholder\" class=\"form-control\">\n                                    </div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">Disabled</label>\n                                    <div class=\"col-sm-10\">\n                                        <input type=\"text\" placeholder=\"Disabled input here...\" disabled=\"\" class=\"form-control\">\n                                    </div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">Static control</label>\n                                    <div class=\"col-sm-10\">\n                                        <p class=\"form-control-static\">hello@creative-tim.com</p>\n                                    </div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">Checkboxes and radios</label>\n                                    <div class=\"col-sm-10\">\n                                        <div class=\"checkbox\">\n                                            <input id=\"checkbox1\" type=\"checkbox\">\n                                            <label for=\"checkbox1\">\n                                                First checkbox\n                                            </label>\n                                        </div>\n\n                                        <div class=\"checkbox\">\n                                            <input id=\"checkbox2\" type=\"checkbox\">\n                                            <label for=\"checkbox2\">\n                                                Second checkbox\n                                            </label>\n                                        </div>\n\n                                        <div class=\"radio\">\n                                            <input type=\"radio\" name=\"radio1\" id=\"radio1\" value=\"option1\" checked=\"\">\n                                            <label for=\"radio1\">\n                                                 First Radio\n                                            </label>\n                                        </div>\n\n                                        <div class=\"radio\">\n                                            <input type=\"radio\" name=\"radio1\" id=\"radio2\" value=\"option2\">\n                                            <label for=\"radio2\">\n                                                Second Radio\n                                            </label>\n                                        </div>\n                                    </div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group\">\n                                   <label class=\"col-sm-2 control-label\">Inline checkboxes</label>\n                                   <div class=\"col-sm-10\">\n                                       <div class=\"checkbox checkbox-inline\">\n                                           <input id=\"checkbox50\" type=\"checkbox\">\n                                           <label for=\"checkbox50\">\n                                               a\n                                           </label>\n                                       </div>\n                                       <div class=\"checkbox checkbox-inline\">\n                                           <input id=\"checkbox51\" type=\"checkbox\">\n                                           <label for=\"checkbox51\">\n                                               b\n                                           </label>\n                                       </div>\n                                       <div class=\"checkbox checkbox-inline\">\n                                           <input id=\"checkbox52\" type=\"checkbox\">\n                                           <label for=\"checkbox52\">\n                                               c\n                                           </label>\n                                       </div>\n                                   </div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <legend>Input Variants</legend>\n\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">Custom Checkboxes &amp; radios</label>\n                                    <div class=\"col-sm-4 col-sm-offset-1\">\n                                        <div class=\"checkbox\">\n                                            <input id=\"checkbox5\" type=\"checkbox\">\n                                            <label for=\"checkbox5\">\n                                            Unchecked\n                                            </label>\n                                        </div>\n\n                                        <div class=\"checkbox\">\n                                            <input id=\"checkbox6\" type=\"checkbox\" checked=\"\">\n                                            <label for=\"checkbox6\">\n                                            Checked\n                                            </label>\n                                        </div>\n\n                                        <div class=\"checkbox\">\n                                            <input id=\"checkbox7\" type=\"checkbox\" disabled=\"\">\n                                            <label for=\"checkbox7\">\n                                             Disabled unchecked\n                                            </label>\n                                        </div>\n\n                                        <div class=\"checkbox\">\n                                            <input id=\"checkbox8\" type=\"checkbox\" checked=\"\" disabled=\"\">\n                                            <label for=\"checkbox8\">\n                                                 Disabled checked\n                                            </label>\n                                        </div>\n                                    </div>\n\n                                    <div class=\"col-sm-5\">\n                                        <div class=\"radio\">\n                                            <input type=\"radio\" name=\"radio10\" id=\"radio20\" value=\"option20\">\n                                            <label for=\"radio20\">\n                                                 Radio is off\n                                            </label>\n                                        </div>\n\n                                        <div class=\"radio\">\n                                            <input type=\"radio\" name=\"radio10\" id=\"radio25\" value=\"option25\" checked=\"\">\n                                            <label for=\"radio25\">\n                                                 Radio is on\n                                            </label>\n                                        </div>\n\n                                        <div class=\"radio\">\n                                            <input type=\"radio\" name=\"radio30\" id=\"radio11\" value=\"option11\" disabled=\"\">\n                                            <label for=\"radio11\">\n                                                 Disabled radio is off\n                                            </label>\n                                        </div>\n\n                                        <div class=\"radio\">\n                                            <input type=\"radio\" name=\"radio30\" id=\"radio12\" value=\"option12\" checked=\"\" disabled=\"\">\n                                            <label for=\"radio12\">\n                                                 Disabled radio is on\n                                            </label>\n                                        </div>\n                                    </div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group has-success\">\n                                    <label class=\"col-sm-2 control-label\">Input with success</label>\n                                    <div class=\"col-sm-10\">\n                                        <input type=\"text\" value=\"Success\" class=\"form-control\"/>\n                                    </div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group has-error\">\n                                    <label class=\"col-sm-2 control-label\">Input with error</label>\n                                    <div class=\"col-sm-10\">\n                                        <input type=\"text\" value=\"Error\" class=\"form-control\"/>\n                                    </div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group column-sizing\">\n                                    <label class=\"col-sm-2 control-label\">Column sizing</label>\n                                    <div class=\"col-sm-10\">\n                                        <div class=\"row\">\n                                            <div class=\"col-md-3\">\n                                                <input type=\"text\" placeholder=\".col-md-3\" class=\"form-control\">\n                                            </div>\n                                            <div class=\"col-md-4\">\n                                                <input type=\"text\" placeholder=\".col-md-4\" class=\"form-control\">\n                                            </div>\n                                            <div class=\"col-md-5\">\n                                                <input type=\"text\" placeholder=\".col-md-5\" class=\"form-control\">\n                                            </div>\n                                        </div>\n                                    </div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">Input groups</label>\n                                    <div class=\"col-sm-3\">\n                                        <div class=\"input-group\">\n                                            <span class=\"input-group-addon\">@</span>\n                                            <input type=\"text\" placeholder=\"Username\" class=\"form-control\">\n                                        </div>\n                                    </div>\n                                    <div class=\"col-sm-3\">\n                                        <div class=\"input-group\">\n                                            <input type=\"text\" class=\"form-control\">\n                                            <span class=\"input-group-addon\">.00</span>\n                                        </div>\n                                    </div>\n                                    <div class=\"col-sm-4\">\n                                        <div class=\"input-group\">\n                                            <span class=\"input-group-addon\">$</span>\n                                            <input type=\"text\" class=\"form-control\">\n                                            <span class=\"input-group-addon\">.00</span>\n                                        </div>\n                                    </div>\n                                </div>\n                            </fieldset>\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">Textarea</label>\n                                    <div class=\"col-sm-10\">\n                                        <textarea class=\"form-control\" placeholder=\"Here can be your nice text\" rows=\"3\"></textarea>\n                                    </div>\n                                </div>\n                            </fieldset>\n                        </form>\n                    </div>\n                </div>  <!-- end card -->\n            </div> <!-- end col-md-12 -->\n        </div> <!-- end row -->\n    </div>\n</div>\n"

/***/ }),

/***/ "./src/app/forms/regularforms/regularforms.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return RegularFormsComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var RegularFormsComponent = /** @class */ (function () {
    function RegularFormsComponent() {
    }
    RegularFormsComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'regularforms-cmp',
            template: __webpack_require__("./src/app/forms/regularforms/regularforms.component.html")
        })
    ], RegularFormsComponent);
    return RegularFormsComponent;
}());



/***/ }),

/***/ "./src/app/forms/validationforms/validationforms.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"main-content\">\n    <div class=\"container-fluid\">\n        <div class=\"row\">\n            <div class=\"col-md-6\">\n                <div class=\"card\">\n                    <form #f=\"ngForm\" novalidate (ngSubmit)=\"save(f.value, f.valid)\">\n\n                        <div class=\"card-header\">\n                            <h4 class=\"card-title\">\n                                Register Form\n                            </h4>\n                        </div>\n                        <div class=\"card-content\">\n                            <div class=\"form-group\">\n                                <label class=\"control-label\" for=\"\">Email Address:\n                                    <span class=\"star\">*</span>\n                                </label>\n                                <input type=\"email\" class=\"form-control\" name=\"email\" [ngModel]=\"register.email\"\n                                  required  #email=\"ngModel\" pattern=\"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+.[a-zA-Z0-9-.]+$\">\n                                <small [hidden]=\"email.valid || (email.pristine && !f.submitted)\" class=\"text-danger\">\n                                  Email is required and format should be <i>john@doe.com</i>.\n                                </small>\n                            </div>\n                            <div class=\"form-group\">\n                                <label  for=\"\" class=\"control-label\">Password\n                                    <span class=\"star\">*</span>\n                                </label>\n                                <input type=\"password\" class=\"form-control\" name=\"password\" [ngModel]=\"register.password\"\n                                  required validateEqual=\"confirmPassword\" reverse=\"true\" #password=\"ngModel\">\n                                <small [hidden]=\"password.valid || (password.pristine && !f.submitted)\" class=\"text-danger\">\n                                  Password is required\n                                </small>\n                            </div>\n                            <div class=\"form-group\">\n                                <label for=\"\" class=\"control-label\">Confirm Password\n                                    <span class=\"star\">*</span>\n                                </label>\n\n                                <input type=\"password\" class=\"form-control\" name=\"confirmPassword\" [ngModel]=\"register.confirmPassword\"\n                                  required validateEqual=\"password\" reverse=\"false\" #confirmPass=\"ngModel\">\n                                <small [hidden]=\"confirmPass.valid || (confirmPass.pristine && !f.submitted)\" class=\"text-danger\">\n                                  Password mismatch\n                                </small>\n                            </div>\n                            <div class=\"category\">    <span class=\"star\">*</span> Required fields</div>\n                        </div>\n                        <div class=\"card-footer\">\n                            <div class=\"checkbox pull-left\">\n                                <input id=\"checkbox1\" name=\"optionsCheckboxes\" type=\"checkbox\" [ngModel]>\n                                <label for=\"checkbox1\">\n                                    Subscribe to newsletter\n                                </label>\n                            </div>\n\n                            <div class=\"form-group text-right\">\n                                <button type=\"submit\" class=\"btn btn-info btn-fill btn-wd\" >Register</button>\n                            </div>\n                        </div>\n                    </form>\n                </div>\n            </div>\n            <div class=\"col-md-6\">\n                <div class=\"card\">\n                    <form #f1=\"ngForm\" novalidate (ngSubmit)=\"save1(f1.value, f1.valid)\">\n                        <div class=\"card-header text-center\">\n                            <h4 class=\"card-title\">\n                                Login Form\n                            </h4>\n                        </div>\n                        <div class=\"card-content\">\n                            <div class=\"form-group\">\n                                <label class=\"control-label\" for=\"\">Email Address:\n                                    <span class=\"star\">*</span>\n                                </label>\n                                <input type=\"email\" class=\"form-control\" name=\"email\" [ngModel]=\"login.email\"\n                                  required  #email=\"ngModel\" pattern=\"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+.[a-zA-Z0-9-.]+$\">\n                                <small [hidden]=\"email.valid || (email.pristine && !f1.submitted)\" class=\"text-danger\">\n                                  Email is required and format should be <i>john@doe.com</i>.\n                                </small>\n                            </div>\n                            <div class=\"form-group\">\n                                <label  for=\"\" class=\"control-label\">Password\n                                    <span class=\"star\">*</span>\n                                </label>\n                                <input type=\"password\" class=\"form-control\" name=\"password\" [ngModel]=\"login.password\"\n                                  required validateEqual=\"confirmPassword\" reverse=\"true\" #password=\"ngModel\">\n                                <small [hidden]=\"password.valid || (password.pristine && !f1.submitted)\" class=\"text-danger\">\n                                  Password is required\n                                </small>\n                            </div>\n                            <div class=\"category\">    <span class=\"star\">*</span> Required fields</div>\n                        </div>\n                        <div class=\"card-footer text-center\">\n                            <button type=\"submit\" class=\"btn btn-info btn-fill btn-wd\">Login</button>\n                        </div>\n                    </form>\n                </div>\n            </div>\n            <div class=\"col-md-12\">\n                <div class=\"card\">\n                    <form #f2=\"ngForm\" class=\"form-horizontal\" novalidate (ngSubmit)=\"save2(f2.value, f2.valid)\">\n                        <div class=\"card-content\">\n                            <h4 class=\"card-title\">Type Validation</h4>\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">\n                                        Required Text\n                                    </label>\n                                    <div class=\"col-sm-6\">\n                                        <input class=\"form-control\" type=\"text\" name=\"text\"  [ngModel]=\"typeValidation.text\"\n                                          required  #text=\"ngModel\">\n                                          <small [hidden]=\"text.valid || (text.pristine && !f2.submitted)\" class=\"text-danger\">\n                                          Text is required.\n                                          </small>\n                                    </div>\n                                    <div class=\"col-sm-4\"><code>required</code></div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">\n                                        Email\n                                    </label>\n                                    <div class=\"col-sm-6\">\n                                        <input type=\"email\" class=\"form-control\" name=\"email\" [ngModel]=\"typeValidation.email\"\n                                          required  #email=\"ngModel\" pattern=\"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+.[a-zA-Z0-9-.]+$\">\n                                        <small [hidden]=\"email.valid || (email.pristine && !f2.submitted)\" class=\"text-danger\">\n                                          Email is required and format should be <i>john@doe.com</i>.\n                                        </small>\n                                    </div>\n                                    <div class=\"col-sm-4\"><code>email=\"true\"</code></div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">\n                                        Number\n                                    </label>\n                                    <div class=\"col-sm-6\">\n                                        <input class=\"form-control\" type=\"number\" name=\"number\" number=\"true\" [ngModel]=\"typeValidation.number\"\n                                          required  #number=\"ngModel\">\n                                    </div>\n                                    <div class=\"col-sm-4\"><code>number=\"true\"</code></div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">\n                                        Url\n                                    </label>\n                                    <div class=\"col-sm-6\">\n                                        <input class=\"form-control\" type=\"text\" id=\"url\" required pattern=\"https?://.+\" [(ngModel)]=\"typeValidation.url\" name=\"url\" #url=\"ngModel\">\n                                          <small [hidden]=\"url.valid || (url.pristine && !f2.submitted)\" class=\"text-danger\">\n                                            Must be a valid URL!\n                                          </small>\n                                    </div>\n                                    <div class=\"col-sm-4\"><code>url=\"true\"</code></div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group column-sizing\">\n                                    <label class=\"col-sm-2 control-label\">\n                                        Equal to\n                                    </label>\n                                    <div class=\"col-sm-3\">\n                                        <input type=\"text\" class=\"form-control\" name=\"idSource\" [ngModel]=\"typeValidation.idSource\" placeholder=\"#idSource\"\n                                          required validateEqual=\"idDestination\" reverse=\"true\" #idSource=\"ngModel\">\n                                        <small [hidden]=\"idSource.valid || (idSource.pristine && !f2.submitted)\" class=\"text-danger\">\n                                          IdSource is required\n                                        </small>\n                                    </div>\n                                    <div class=\"col-sm-3\">\n                                        <input type=\"text\" class=\"form-control\" name=\"idDestination\" [ngModel]=\"typeValidation.idDestination\" placeholder=\"#idDestination\"\n                                          required validateEqual=\"idSource\" reverse=\"false\" #idDestination=\"ngModel\">\n                                        <small [hidden]=\"idDestination.valid || (idDestination.pristine && !f2.submitted)\" class=\"text-danger\">\n                                          IdSource mismatch!\n                                        </small>\n                                    </div>\n                                    <div class=\"col-sm-4\"><code>equalTo=\"#idSource\"</code>\n                                    </div>\n                                </div>\n                            </fieldset>\n\n                            <!-- <h4 class=\"card-title\">Range validation</h4>\n\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">\n                                        Min Length\n                                    </label>\n                                    <div class=\"col-sm-6\">\n                                        <input class=\"form-control\"\n                                               type=\"text\"\n                                               name=\"min_length\"\n                                               minLength=\"5\"\n                                        />\n                                    </div>\n                                    <div class=\"col-sm-4\"><code>minLength=\"5\"</code></div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">\n                                        Max Length\n                                    </label>\n                                    <div class=\"col-sm-6\">\n                                        <input class=\"form-control\"\n                                               type=\"text\"\n                                               name=\"max_length\"\n                                               maxLength=\"5\"\n                                        />\n                                    </div>\n                                    <div class=\"col-sm-4\"><code>maxLength=\"5\"</code></div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">Range</label>\n                                    <div class=\"col-sm-6\">\n                                        <input class=\"form-control\"\n                                               type=\"text\"\n                                               name=\"range\"\n                                               range=\"[6,10]\"\n                                        />\n                                    </div>\n                                    <div class=\"col-sm-4\"><code>range=\"[6,10]\"</code></div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">\n                                        Min Value\n                                    </label>\n                                    <div class=\"col-sm-6\">\n                                        <input class=\"form-control\"\n                                               type=\"text\"\n                                               name=\"min\"\n                                               min=\"6\"\n                                        />\n                                    </div>\n                                    <div class=\"col-sm-4\"><code>min=\"6\"</code></div>\n                                </div>\n                            </fieldset>\n\n                            <fieldset>\n                                <div class=\"form-group\">\n                                    <label class=\"col-sm-2 control-label\">\n                                        Max Value\n                                    </label>\n                                    <div class=\"col-sm-6\">\n                                        <input class=\"form-control\"\n                                               type=\"text\"\n                                               name=\"max\"\n                                               max=\"6\"\n                                        />\n                                    </div>\n                                    <div class=\"col-sm-4\"><code>max=\"6\"</code></div>\n                                </div>\n                            </fieldset> -->\n                        </div>\n                        <div class=\"card-footer text-center\">\n                            <button type=\"submit\" class=\"btn btn-info btn-fill\">Validate Inputs</button>\n                        </div>\n                    </form>\n                </div>\n            </div>\n        </div>\n    </div>\n</div>\n"

/***/ }),

/***/ "./src/app/forms/validationforms/validationforms.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ValidationFormsComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var ValidationFormsComponent = /** @class */ (function () {
    function ValidationFormsComponent() {
    }
    ValidationFormsComponent.prototype.ngOnInit = function () {
        this.register = {
            email: '',
            password: '',
            confirmPassword: ''
        };
        this.login = {
            email: '',
            password: ''
        };
        this.typeValidation = {
            text: '',
            email: '',
            idSource: '',
            idDestination: '',
            url: ''
        };
    };
    ValidationFormsComponent.prototype.save = function (model, isValid) {
        // call API to save customer
        if (isValid) {
            console.log(model, isValid);
        }
    };
    ValidationFormsComponent.prototype.save1 = function (model, isValid) {
        // call API to save customer
        if (isValid) {
            console.log(model, isValid);
        }
    };
    ValidationFormsComponent.prototype.save2 = function (model, isValid) {
        // call API to save customer
        if (isValid) {
            console.log(model, isValid);
        }
    };
    ValidationFormsComponent.prototype.onSubmit = function (value) {
        console.log(value);
    };
    ValidationFormsComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'validationforms-cmp',
            template: __webpack_require__("./src/app/forms/validationforms/validationforms.component.html")
        })
    ], ValidationFormsComponent);
    return ValidationFormsComponent;
}());



/***/ }),

/***/ "./src/app/forms/wizard/wizard.component.html":
/***/ (function(module, exports) {

module.exports = "<div class=\"main-content\">\n    <div class=\"container-fluid\">\n        <div class=\"row\">\n            <div class=\"col-md-8 col-md-offset-2\">\n                <div class=\"card card-wizard\" id=\"wizardCard\">\n                    <form id=\"wizardForm\" method=\"\" action=\"\">\n                        <div class=\"card-header text-center\">\n                            <h4 class=\"card-title\">Awesome Wizard</h4>\n                            <p class=\"category\">Split a complicated flow in multiple steps</p>\n                        </div>\n                        <div class=\"card-content\">\n                            <ul class=\"nav\">\n                                <li><a href=\"#tab1\" data-toggle=\"tab\">First Tab</a></li>\n                                <li><a href=\"#tab2\" data-toggle=\"tab\">Second Tab</a></li>\n                                <li><a href=\"#tab3\" data-toggle=\"tab\">Third Tab</a></li>\n                            </ul>\n                            <div class=\"tab-content\">\n                                <div class=\"tab-pane\" id=\"tab1\">\n                                    <h5 class=\"text-center\">Please tell us more about yourself.</h5>\n                                    <div class=\"row\">\n                                        <div class=\"col-md-5 col-md-offset-1\">\n                                            <div class=\"form-group\">\n                                                <label class=\"control-label\">\n                                                    First Name\n                                                </label>\n                                                <input class=\"form-control\"\n                                                       type=\"text\"\n                                                       name=\"first_name\"\n                                                       placeholder=\"ex: Mike\"\n                                                />\n                                            </div>\n                                        </div>\n                                        <div class=\"col-md-5\">\n                                            <div class=\"form-group\">\n                                                <label class=\"control-label\">\n                                                    Last Name\n                                                </label>\n                                                <input class=\"form-control\"\n                                                       type=\"text\"\n                                                       name=\"last_name\"\n                                                       required=\"true\"\n                                                       placeholder=\"ex: Andrew\"\n                                                />\n                                            </div>\n                                        </div>\n                                    </div>\n                                    <div class=\"row\">\n                                        <div class=\"col-md-10 col-md-offset-1\">\n                                            <div class=\"form-group\">\n                                                <label class=\"control-label\">\n                                                    Email    <span class=\"star\">*</span>\n                                                </label>\n                                                <input class=\"form-control\"\n                                                       type=\"text\"\n                                                       name=\"email\"\n                                                       email=\"true\"\n                                                       placeholder=\"ex: hello@creative-tim.com\"\n                                                />\n                                            </div>\n                                        </div>\n                                    </div>\n                                </div>\n                                <div class=\"tab-pane\" id=\"tab2\">\n                                    <h5 class=\"text-center\">Please give us more details about your platform.</h5>\n                                    <div class=\"row\">\n                                        <div class=\"col-md-10 col-md-offset-1\">\n                                            <div class=\"form-group\">\n                                                <label class=\"control-label\">Your Website    <span class=\"star\">*</span></label>\n                                                <input class=\"form-control\"\n                                                       type=\"text\"\n                                                       name=\"website\"\n                                                       url=\"true\"\n                                                       placeholder=\"ex: http://www.creative-tim.com\"\n                                                />\n                                            </div>\n                                        </div>\n                                    </div>\n                                    <div class=\"row\">\n                                        <div class=\"col-md-5 col-md-offset-1\">\n                                            <div class=\"form-group\">\n                                                <label class=\"control-label\">Framework Type</label>\n                                                <input class=\"form-control\"\n                                                       type=\"text\"\n                                                       name=\"framework\"\n                                                       placeholder=\"ex: http://www.creative-tim.com\"\n                                                />\n                                            </div>\n                                        </div>\n                                        <div class=\"col-md-5\">\n                                            <div class=\"form-group\">\n                                                <label class=\"control-label\">\n                                                    Language    <span class=\"star\">*</span>\n                                                </label>\n                                                <!--     IMPORTANT! - the \"bootstrap select picker\" is not compatible with jquery.validation.js, that's why we use the \"default select\" inside this wizard. We will try to contact the guys who are responsibble for the \"bootstrap select picker\" to find a solution. Thank you for your patience.\n                                                 -->\n                                                <select name=\"cities\" class=\"form-control\">\n                                                    <option selected=\"\" disabled=\"\">- language -</option>\n                                                    <option value=\"ms\">Bahasa Melayu</option>\n                                                    <option value=\"ca\">Català</option>\n                                                    <option value=\"da\">Dansk</option>\n                                                    <option value=\"de\">Deutsch</option>\n                                                    <option value=\"en\">English</option>\n                                                    <option value=\"es\">Español</option>\n                                                    <option value=\"el\">Eλληνικά</option>\n                                                    <option value=\"fr\">Français</option>\n                                                    <option value=\"it\">Italiano</option>\n                                                    <option value=\"hu\">Magyar</option>\n                                                    <option value=\"nl\">Nederlands</option>\n                                                    <option value=\"no\">Norsk</option>\n                                                    <option value=\"pl\">Polski</option>\n                                                    <option value=\"pt\">Português</option>\n                                                    <option value=\"fi\">Suomi</option>\n                                                    <option value=\"sv\">Svenska</option>\n                                                    <option value=\"tr\">Türkçe</option>\n                                                    <option value=\"is\">Íslenska</option>\n                                                    <option value=\"cs\">Čeština</option>\n                                                    <option value=\"ru\">Русский</option>\n                                                    <option value=\"th\">ภาษาไทย</option>\n                                                    <option value=\"zh\">中文 (简体)</option>\n                                                    <option value=\"zh-TW\">中文 (繁體)</option>\n                                                    <option value=\"ja\">日本語</option>\n                                                    <option value=\"ko\">한국어</option>\n                                                </select>\n                                            </div>\n                                        </div>\n                                    </div>\n                                    <div class=\"row\">\n                                        <div class=\"col-md-5 col-md-offset-1\">\n                                            <div class=\"form-group\">\n                                                <label class=\"control-label\">\n                                                    Bootstrap Version\n                                                </label>\n                                                <!--     IMPORTANT! - the \"bootstrap select picker\" is not compatible with jquery.validation.js, that's why we use the \"default select\" inside this wizard. We will try to contact the guys who are responsibble for the \"bootstrap select picker\" to find a solution. Thank you for your patience.\n                                                 -->\n                                                <select name=\"cities\" class=\"form-control\">\n                                                    <option selected=\"\" disabled=\"\">- version -</option>\n                                                    <option value=\"1.1\">Bootstrap 1.1</option>\n                                                    <option value=\"2.0\">Bootstrap 2.0</option>\n                                                    <option value=\"3.0\">Bootstrap 3.0</option>\n                                                    <option value=\"4.0\">Bootstrap 4.0(alpha)</option>\n                                                </select>\n                                            </div>\n                                        </div>\n                                        <div class=\"col-md-5\">\n                                            <div class=\"form-group\">\n                                                <label class=\"control-label\">Price</label>\n                                                <input class=\"form-control\"\n                                                       type=\"text\"\n                                                       name=\"price\"\n                                                       placeholder=\"ex: 19.00\"\n                                                />\n                                            </div>\n                                        </div>\n                                    </div>\n                                </div>\n                                <div class=\"tab-pane\" id=\"tab3\">\n                                    <h2 class=\"text-center text-space\">Yuhuuu! <br><small> \t\t\t\t\t\t\t\t\t\t\t\t\tClick on \"<b>Finish</b>\" to join our community</small></h2>\n                                </div>\n                            </div>\n                        </div>\n                        <div class=\"card-footer\">\n                            <button type=\"button\" class=\"btn btn-default btn-fill btn-wd btn-back pull-left\">Back</button>\n                            <button type=\"button\" class=\"btn btn-info btn-fill btn-wd btn-next pull-right\">Next</button>\n                            <button type=\"button\" class=\"btn btn-info btn-fill btn-wd btn-finish pull-right\" (click)=\"onFinishWizard()\">Finish</button>\n                            <div class=\"clearfix\"></div>\n                        </div>\n                    </form>\n                </div>\n            </div>\n        </div>\n    </div>\n</div>\n"

/***/ }),

/***/ "./src/app/forms/wizard/wizard.component.ts":
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return WizardComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__("./node_modules/@angular/core/esm5/core.js");
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var WizardComponent = /** @class */ (function () {
    function WizardComponent() {
    }
    WizardComponent.prototype.readURL = function (input) {
        if (input.files && input.files[0]) {
            var reader = new FileReader();
            reader.onload = function (e) {
                $('#wizardPicturePreview').attr('src', e.target.result).fadeIn('slow');
            };
            reader.readAsDataURL(input.files[0]);
        }
    };
    WizardComponent.prototype.onFinishWizard = function () {
        //here you can do something, sent the form to server via ajax and show a success message with swal
        swal("Good job!", "You clicked the finish button!", "success");
    };
    WizardComponent.prototype.ngOnInit = function () {
        var $validator = $("#wizardForm").validate({
            rules: {
                email: {
                    required: true,
                    email: true,
                    minlength: 5
                },
                first_name: {
                    required: false,
                    minlength: 5
                },
                last_name: {
                    required: false,
                    minlength: 5
                },
                website: {
                    required: true,
                    minlength: 5,
                    url: true
                },
                framework: {
                    required: false,
                    minlength: 4
                },
                cities: {
                    required: true
                },
                price: {
                    number: true
                }
            }
        });
        // you can also use the nav-pills-[blue | azure | green | orange | red] for a different color of wizard
        $('#wizardCard').bootstrapWizard({
            tabClass: 'nav nav-pills',
            nextSelector: '.btn-next',
            previousSelector: '.btn-back',
            onNext: function (tab, navigation, index) {
                var $valid = $('#wizardForm').valid();
                if (!$valid) {
                    $validator.focusInvalid();
                    return false;
                }
            },
            onInit: function (tab, navigation, index) {
                //check number of tabs and fill the entire row
                var $total = navigation.find('li').length;
                var $width = 100 / $total;
                var $display_width = $(document).width();
                if ($display_width < 600 && $total > 3) {
                    $width = 50;
                }
                navigation.find('li').css('width', $width + '%');
            },
            onTabClick: function (tab, navigation, index) {
                // Disable the posibility to click on tabs
                return false;
            },
            onTabShow: function (tab, navigation, index) {
                var $total = navigation.find('li').length;
                var $current = index + 1;
                var wizard = navigation.closest('.card-wizard');
                // If it's the last tab then hide the last button and show the finish instead
                if ($current >= $total) {
                    $(wizard).find('.btn-next').hide();
                    $(wizard).find('.btn-finish').show();
                }
                else if ($current == 1) {
                    $(wizard).find('.btn-back').hide();
                }
                else {
                    $(wizard).find('.btn-back').show();
                    $(wizard).find('.btn-next').show();
                    $(wizard).find('.btn-finish').hide();
                }
            }
        });
    };
    WizardComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["o" /* Component */])({
            moduleId: module.i,
            selector: 'wizard-cmp',
            template: __webpack_require__("./src/app/forms/wizard/wizard.component.html")
        })
    ], WizardComponent);
    return WizardComponent;
}());



/***/ })

});
//# sourceMappingURL=forms.module.chunk.js.map