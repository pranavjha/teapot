/**
 * @fileoverview the base class declaration for the teapot application.<br/> Inspired by Simple JavaScript Inheritance By John Resig
 *               http://ejohn.org/ MIT Licensed.
 */
// strict mode
"use strict";
(function() {
    /**
     * map of classname and classType for classes that are loaded
     * @type Object.<string, Class.ClassType>
     * @private
     */
    var loadedClass = {};

    /**
     * The base Class implementation (does nothing) All classes must extend the Class class
     * @expose
     */
    this.Class = function() {
        // do nothing here
    };
    // Enforce the constructor to be what we expect
    this.Class.prototype.constructor = this.Class;
    /**
     * Creates a new class that extends from this class.
     * @param {object} prop the extending class properties. The init property is used as the constructor. The rest of the properties are added to the
     *        new class
     * @param {object} statics the extending class static properties.
     * @returns the new class created
     * @expose
     */
    this.Class.extend = function(prop, statics) {
        // create a new class
        var newClass = function() {
            if (this.init) {
                this.init.apply(this, arguments);
            }
        };
        // attach static declaration
        statics = statics || {};
        // the __proto__ of the new class should be created from the prototype of the base class.
        // this makes static functions of base class accessible.
        Object.setPrototypeOf(newClass, Object.create(Object.getPrototypeOf(this)));
        // add the static declarations in the __proto__ object
        for ( var name in statics) {
            // if there is an override, make _super available
            if ((typeof statics[name] == "function" && typeof Object.getPrototypeOf(newClass)[name] == "function")) {
                Object.getPrototypeOf(newClass)[name] = (function(name, baseFn, superFn) {
                    return function() {
                        newClass._super = superFn;
                        // The method only need to be bound temporarily, so we remove it when we're done executing
                        var ret = baseFn.apply(newClass, arguments);
                        delete newClass._super;
                        return ret;
                    };
                })(name, statics[name], Object.getPrototypeOf(newClass)[name]);
            } else {
                Object.getPrototypeOf(newClass)[name] = statics[name];
            }
        }
        // attach public declarations
        prop = prop || {};
        // extend the prototype of the base class
        newClass.prototype = Object.create(this.prototype);
        // Copy the new properties over onto the prototype
        for ( var name in prop) {
            // if there is an override, make _super available
            if (typeof prop[name] == "function" && typeof newClass.prototype[name] == "function") {
                newClass.prototype[name] = (function(name, baseFn, superFn) {
                    return function() {
                        this._super = superFn;
                        var ret = baseFn.apply(this, arguments);
                        delete this._super;
                        return ret;
                    };
                })(name, prop[name], newClass.prototype[name]);
            } else {
                newClass.prototype[name] = prop[name];
            }
        }
        // Enforce the constructor to be what we expect
        newClass.prototype.constructor = newClass;
        // add the extend function
        newClass.extend = this.extend;
        return newClass;
    };

    /**
     * Imports a specific class. If a callback is specified, the class import is asynchronous. Without callback, the control pauses till the class is
     * imported. classType and callback parameters can be sent in any order or ignored.<br/> <b>Note:</b> if the class is already in the registry,
     * the postConstruct function is not called. Hence, if a class is compiled and merged into a static script and loaded, the corresponding
     * postconstruct activities should be explicitly done
     * @param {string} namespace the class to import
     * @param {Class.ClassType=} optional classType the type of class that is loaded. Classes can be a JavaScript class, a template or a stylesheet
     * @param {function()=} optional callback to be called after the class has been loaded
     * @returns the class corresponding to the namespace if the import is asynchronous or if the class already exists
     * @expose
     */
    this.Class.load = function(namespace) {
        // sanitize the arguments
        var namespace = arguments[0], classType, callback;
        if (arguments[1] instanceof Function) {
            callback = arguments[1];
            classType = arguments[2];
        } else {
            callback = arguments[2];
            classType = arguments[1];
        }
        // if no classType is specified assume it to be a script
        classType = classType || Class.ClassType.CLASS;
        // if the classtype is a dwr class, the namespace will be the last part of the name
        if(classType === Class.ClassType.DWR){
            namespace = namespace.split('.').pop();
        } 
        // if the namespace is in memory and not registered, register it
        try {
            var _ns = eval(namespace);
            if (_ns) {
                loadedClass[namespace] = classType;
            }
        } catch (e) {
            // do nothing here
        }
        if (!loadedClass[namespace]) {
            // if the namespace is not in memory, it needs to be loaded
            $.ajax({
                url : (classType.urlPrefix + namespace.split('.').join('/') + classType.urlPostfix),
                // if callback is provided, load asynchronously
                async : (callback ? true : false),
                dataType : classType.resourceType
            }).done(function(data) {
                // if there is a postConstruct corresponding to the class type, call it
                classType.postConstruct && classType.postConstruct(namespace, data);
                // if the class itself has a postConstruct, call it
                eval(namespace).postConstruct && eval(namespace).postConstruct();
                // cache the loaded class type. this is used for unloading later
                loadedClass[namespace] = classType;
                // call the callback if there is one
                if (callback) {
                    callback.apply(document, arguments);
                }
            }).fail(function(jqXHR, textStatus, errorThrown) {
                $.error("failed to load class of namespace '" + namespace + "'.<br/>Response Status : '" + errorThrown + "'");
            });
        } else if (callback) {
            // if the namespace is already in memory, and if there is a callback to be called, call it
            callback.apply(document, arguments);
        }
    };
    /**
     * Garbage collects a specified class. All the objects of the class must be destroyed before the class is garbage collected.
     * @param {string} namespace the class to be garbage collected
     * @expose
     */
    this.Class.unLoad = function(namespace) {
        // if the class is loaded, remove it from memory
        if (loadedClass[namespace]) {
            // if the class itself has a preDestroy, call it
            eval(namespace).preDestroy && eval(namespace).preDestroy();
            // if there is a predestroy associated, use it
            loadedClass[namespace].preDestroy && loadedClass[namespace].preDestroy(namespace);
            // clear the namespace
            delete loadedClass[namespace];
            var _parentNs = eval(namespace.split(".").slice(0, -1).join('.'));
            _parentNs[namespace.split(".").slice(-1).join('')] = undefined;
        }
    };

    /**
     * Creates a package with the given name if it does not already exist
     * @param {string} className the package to use
     * @expose
     */
    this.Class.use = function(className) {
        var packages = className.split('.');
        var obj = window;
        for ( var i = 0; i < packages.length; i++) {
            if (!obj[packages[i]]) {
                obj[packages[i]] = {};
            }
            obj = obj[packages[i]];
        }
        ;
        // prevent the class from loading again
        loadedClass[className] = Class.ClassType.CLASS;
    };

    /**
     * Enum for the different types of classes. Classes can be a JavaScript class, a template or a stylesheet
     * @enum {{urlPrefix: string, urlPrefix: string, urlPrefix: string, postConstruct: function(string, string), preDestroy: function(string}}
     * @expose
     */
    this.Class.ClassType = {
        /**
         * a JavaScript class
         * @expose
         */
        CLASS : {
            urlPrefix : 'script/',
            urlPostfix : '.js',
            resourceType : 'script'
        },
        /**
         * a StyleSheet class
         * @expose
         */
        STYLE : {
            urlPrefix : '',
            urlPostfix : '.gss',
            resourceType : 'text',
            postConstruct : function(namespace, data) {
                $('head').append('<style id="' + namespace + '" type="text/css">' + data + '</style>');
                Class.use(namespace);
            },
            preDestroy : function(namespace) {
                $('head').find('#' + namespace.replace(/\./g, "\\.")).remove();
            }
        },
        /**
         * a template class
         * @expose
         */
        TEMPLATE : {
            urlPrefix : '',
            urlPostfix : '.soy',
            resourceType : 'script'
        },
        /**
         * a dwr class
         * @expose
         */
        DWR : {
            urlPrefix : '',
            urlPostfix : '.js',
            resourceType : 'script'
        }
    };
}).call(window);