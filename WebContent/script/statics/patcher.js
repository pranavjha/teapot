/**
 * @fileoverview this file patches the basic javascript objects and functions for browser compatibility and enhancements
 */
// strict mode
"use strict";
(function() {
    /**
     * Sets the prototype of an Object to the given prototype
     * @param {*} object the object whose prototype is to be set
     * @param {*} prototype the prototype to be used
     * @expose
     */
    this.Object.setPrototypeOf = function(object, prototype) {
        object.__proto__ = prototype;
    };

    /**
     * a private static month names array
     * @private
     */
    this.Date.__monthNames__ = new Array('January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October',
            'November', 'December');

    /**
     * a private static day names array
     * @private
     */
    this.Date.__dayNames__ = new Array('Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday');

    /**
     * A date formatter implementation. Formats a date with the format specified in the string Available format options are:
     * <ul>
     * <li> yyyy - full year (ex:1986)</li>
     * <li>yy - 2 digit year (ex: 86)</li>
     * <li>MMMM - full month name (ex: January)</li>
     * <li>MMM - short month name (ex: Jan)</li>
     * <li>MM - 2 digit month number (ex: 01)</li>
     * <li>M - month number (ex:1)</li>
     * <li>dddd - Day name (ex: Monday)</li>
     * <li>ddd - Short day name (ex: Mon)</li>
     * <li>dd - 2 digit date (ex: 01)</li>
     * <li>d - date (ex: 1)</li>
     * <li>HH - 24 hour clock hours (ex:00, 23)</li>
     * <li>hh - 12 hour clock hours (ex: 01, 11)</li>
     * <li>mm - 2 digit minutes (ex:00, 59)</li>
     * <li>ss - 2 digit seconds (ex:00, 59)</li>
     * <li>SSS - 3 digit year milliseconds (ex:999, 012)</li>
     * <li>S - milliseconds (ex: 1, 23, 234)</li>
     * <li>a\p - am / pm</li>
     * </ul>
     * @param {string} format the date format
     * @returns {string} the string representation of the formatter date
     * @expose
     */
    this.Date.prototype.format = function(format) {
        if (!this.valueOf())
            return ' ';
        var d = this;
        // Zero-Fill
        var zf = function(value, len) {
            return '0000'.substr(0, len - value.toString().length) + value;
        };

        return format.replace(/(yyyy|yy|MMMM|MMM|MM|M|dddd|ddd|dd|d|HH|hh|mm|ss|SSS|S|a\/p)/g, function(val) {
            switch (val) {
            case 'yy':
                return ('' + d.getFullYear()).substr(2);
            case 'yyyy':
                return ('' + d.getFullYear());
            case 'MMMM':
                return Date.__monthNames__[d.getMonth()];
            case 'MMM':
                return Date.__monthNames__[d.getMonth()].substr(0, 3);
            case 'MM':
                return zf((d.getMonth() + 1), 2);
            case 'M':
                return '' + (d.getMonth() + 1);
            case 'dddd':
                return Date.__dayNames__[d.getDay()];
            case 'ddd':
                return Date.__dayNames__[d.getDay()].substr(0, 3);
            case 'dd':
                return zf(d.getDate(), 2);
            case 'd':
                return d.getDate();
            case 'HH':
                var h = d.getHours();
                return zf(h, 2);
            case 'hh':
                var h = d.getHours() % 12;
                return zf((h ? h : 12), 2);
            case 'mm':
                return zf(d.getMinutes(), 2);
            case 'ss':
                return zf(d.getSeconds(), 2);
            case 's':
                return d.getSeconds();
            case 'SSS':
                return zf(d.getMilliseconds(), 3);
            case 'S':
                return d.getMilliseconds();
            case 'a/p':
                return d.getHours() < 12 ? 'am' : 'pm';
            }
        });
    };

    /**
     * If called with no parameters, the function trims space from both side of the string If the length parameter is specified, it trims the string
     * to a specified length removing characters from right if the length is positive and from left if the length is negative. If the string is
     * smaller than the specified length value, this function will return the string itself
     * @param {number=} optional length the length to trim to
     * @return {string} the trimmed string
     * @expose
     */
    this.String.prototype.forceTrim = function(length) {
        if (length == undefined) {
            return this.replace(/^\s+|\s+$/g, "");
        } else if (length > 0) {
            return (this.length > length) ? this.substr(0, length) : this;
        } else {
            return (this.length > -length) ? this.substr(this.length + length) : this;
        }
    };

    /**
     * pads the string to a specified length adding characters to the left if the length is negative and to the right if the length is positive. If
     * the string is longer than the specified length value or if the length is undefined, this function will return the string itself
     * @param {string} padChar the character to use for padding
     * @param {number=} optional length the length to trim to
     * @return {string} the padded string
     * @expose
     */
    this.String.prototype.pad = function(padChar, length) {
        if(length == undefined) {
            return this;
        } else if(length > 0){
            var str = this;
            for ( var i = str.length; i < length; i++)
                str = str + padChar;
            return str;
        } else {
            var str = this;
            for ( var i = str.length; i < -length; i++) {
                str = padChar + str;
            }
            return str;
        }
    };
}).call(window);