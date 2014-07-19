/**
 * @fileoverview Defines the Logger
 */
// strict mode
"use strict";
Class.use("teapot.util.Logger");
/**
 * @class Defines the logger class
 */
teapot.util.Logger = Class.extend({},
/*
 * static declarations
 */
{
    /**
     * Static variable to save all logger configurations
     * @private
     */
    _loggers : {},
    /**
     * This function configures the logging levels and log type for different namespaces
     * @param {Array<{namespace:{string},type:{teapot.util.Logger.Type},level:{teapot.util.Logger.Level},output:{string}},additivity:{boolean}>}
     *        options
     * @expose
     */
    configure : function(options) {
        // clear the loggers array
        this._loggers = {};
        // keep the configuration in the options
        for ( var i in options) {
            this._loggers['.' + options[i].namespace] = options[i];
            // initialize the particular logger if it is required. do it only once
            !options[i].type.__init && options[i].type.init && options[i].type.init();
            options[i].type.__init = true;
        }
    },
    /**
     * Returns an array of logger configuration to be used for the namespace.
     * @param namespace the namespace
     * @returns {Array<{namespace:{string},type:{teapot.util.Logger.Type},level:{teapot.util.Logger.Level},output:{string}},additivity:{boolean}>}
     * @private
     */
    _getUsedLoggers : function(namespace) {
        var _namespaces = namespace.split(".");
        var _currNs = "";
        var _usedLoggers = [];
        // push the default namespace logger if it exists
        if (this._loggers["."]) {
            _usedLoggers.push(this._loggers["."]);
        }
        for ( var i = 0; i < _namespaces.length; i++) {
            _currNs = _currNs + '.' + _namespaces[i];
            this._loggers[_currNs] && _usedLoggers.push(this._loggers[_currNs]);
        }
        return _usedLoggers.reverse();
    },

    /**
     * This function returns the logger configured for the given namespace
     * @param {string} namespace the namespace used to get the proper logger configuration
     * @public
     */
    getLogger : function(namespace) {
        // if there are no loggers registered, use the console
        if (this._loggers.length == 0) {
            return console;
        }
        // list of loggers to be invoked
        var _usedLoggers = this._getUsedLoggers(namespace);

        /**
         * formats the log message in the specified format
         * @param {string} format the format string used to format the message
         * @param {string} levelName the logger level name
         * @param {string} message the output log message
         * @private
         */
        var _format = function(format, level, message) {
            return format.replace(/%([+-]?[0-9]+)?([\.][+-]?[0-9]+)?[a-zA-Z]{1}(\{.*\})?/g, function(val) {
                var fmt = val.match(/([+-]?[0-9]+)?([\.]([+-]?[0-9]+))?([a-zA-Z]{1})(\{(.*)\})?/);
                var minLength = fmt[1] && parseInt(fmt[1]), maxlength = fmt[3] && parseInt(fmt[3]), variable = fmt[4], format = fmt[6];
                var _paramVal = '';
                // make a list of parameters
                var _params = {
                    'p' : level,
                    'c' : namespace,
                    'd' : new Date(),
                    'm' : message
                };
                // get the new variable in the specified format
                if (format && _params[variable].format) {
                    _paramVal = _params[variable].format(format);
                } else {
                    _paramVal = _params[variable].toString();
                }
                // trim to max length
                _paramVal = _paramVal.pad(' ', minLength);
                // trim to max length
                _paramVal = _paramVal.forceTrim(maxlength);
                return _paramVal;
            });
        };

        /**
         * This function does the actual logging of messages, the exposed functions call this function for logging
         * @param {teapot.util.Logger.Level} level the logging level used
         * @param {string} levelName the logger level name
         * @param {Argument} the arguments passed to the log command
         * @private
         */
        var _logFn = function(level, levelName, logArgs) {
            // loop through all the available loggers satisfying the namespace
            for ( var i = 0; i < _usedLoggers.length; i++) {
                // log only if logging level configured is not greater than the current log level
                if (_usedLoggers[i].level <= level) {
                    // call the corresponding log function for the type
                    _usedLoggers[i].type[levelName].call(_usedLoggers[i].type, _format(_usedLoggers[i].output, levelName, Array.prototype.join.call(
                            logArgs, " ")));
                }
                // if additivity is false, do not log beyond this point
                if (!_usedLoggers[i].additivity) {
                    return;
                }
            }
        };
        return {

            /**
             * debug message logging function
             * @expose
             */
            debug : function() {
                _logFn(teapot.util.Logger.Level.DEBUG, 'debug', arguments);
            },

            /**
             * info message logging function
             * @expose
             */
            info : function() {
                _logFn(teapot.util.Logger.Level.INFO, 'info', arguments);
            },

            /**
             * warning message logging function
             * @expose
             */
            warn : function() {
                _logFn(teapot.util.Logger.Level.WARN, 'warn', arguments);
            },

            /**
             * error message logging function
             * @expose
             */
            error : function() {
                _logFn(teapot.util.Logger.Level.ERROR, 'error', arguments);
            }
        };
    },

    /**
     * Enum for the different Logging types. Logger types can be CONSOLE, POPUP or SERVER
     * @enum {{init: function, debug: function(string), info: function(string), warn: function(string), error: function(string}}
     * @expose
     */
    Type : {

        /**
         * CONSOLE type is used to log to console
         * @expose
         */
        CONSOLE : {

            /**
             * generates a debug log on console
             * @param message the message to log
             * @expose
             */
            debug : function(message) {
                console.debug(message);
            },

            /**
             * generates an info log on console
             * @param message the message to log
             * @expose
             */
            info : function(message) {
                console.info(message);
            },

            /**
             * generates a warning log on console
             * @param message the message to log
             * @expose
             */
            warn : function(message) {
                console.warn(message);
            },

            /**
             * generates an error log on console
             * @param message the message to log
             * @expose
             */
            error : function(message) {
                console.error(message);
            }
        },

        /**
         * POPUP type is used to log on a separate popup window
         * @expose
         */
        POPUP : {

            /**
             * The element to append logs to
             * @private
             */
            _logElement : null,

            /**
             * The log popup window
             * @private
             */
            _logWin : null,

            /**
             * initializes the popup logger
             * @expose
             */
            init : function() {
                // load the template and style
                Class.load("templates.teapot.util.Logger", Class.ClassType.TEMPLATE);
                Class.load("styles.teapot.util.Logger", Class.ClassType.STYLE);
                // create a popup window if it doesnot exist
                var _logDoc = window
                        .open(
                                "about:blank",
                                "popup",
                                "directories=no,height=300,left=0,location=no,menubar=no,resizable=yes,status=no,titlebar=no,titlebar=no,toolbar=no,top=0,width=800;scrollbars=yes",
                                true).document;
                _logDoc.open();
                _logDoc.write(templates.teapot.util.Logger({
                    style : $('#styles\\.teapot\\.util\\.Logger').html()
                }));
                Class.unLoad("styles.teapot.util.Logger");
                _logDoc.close();
                this._logDocument = $(_logDoc);
                this._logElement = $('#logContainer', this._logDocument);
            },

            /**
             * generates a info log on a popup
             * @param message the message to log
             * @expose
             */
            info : function(message) {
                this._logElement.prepend($('<div>', this._logDocument).addClass('log info').html(message));
            },

            /**
             * generates a info log on a popup
             * @param message the message to log
             * @expose
             */
            debug : function(message) {
                this._logElement.prepend($('<div>', this._logDocument).addClass('log debug').html(message));
            },

            /**
             * generates a warning log on a popup
             * @param message the message to log
             * @expose
             */
            warn : function(message) {
                this._logElement.prepend($('<div>', this._logDocument).addClass('log warn').html(message));
            },

            /**
             * generates a error log on a popup
             * @param message the message to log
             * @expose
             */
            error : function(message) {
                this._logElement.prepend($('<div>', this._logDocument).addClass('log error').html(message));
            }
        },

        /**
         * SERVER type is used to log to the server
         * @expose
         */
        SERVER : {

            /**
             * Initialises the server logger
             * @expose
             */
            init : function() {
                Class.load("dwr.engine");
                Class.load("dwr.interface.ScriptLogger", Class.ClassType.DWR);
            },

            /**
             * generates a debug log on the server
             * @param message the message to log
             * @expose
             */
            debug : function(message) {
                ScriptLogger.debug(message);
            },

            /**
             * generates a info log on the server
             * @param message the message to log
             * @expose
             */
            info : function(message) {
                ScriptLogger.info(message);
            },

            /**
             * generates a warning log on the server
             * @param message the message to log
             * @expose
             */
            warn : function(message) {
                ScriptLogger.warn(message);
            },

            /**
             * generates an error log on the server
             * @param message the message to log
             * @expose
             */
            error : function(message) {
                ScriptLogger.error(message);
            }
        }
    },
    /**
     * Enum for the different Logging levels. Logger types can be DEBUG, INFO, WARN or ERROR
     * @enum {number}
     * @expose
     */
    Level : {
        /**
         * DEBUG log level
         */
        DEBUG : 1,
        /**
         * INFO log level
         */
        INFO : 2,
        /**
         * WARN log level
         */
        WARN : 3,
        /**
         * ERROR log level
         */
        ERROR : 4
    }
});