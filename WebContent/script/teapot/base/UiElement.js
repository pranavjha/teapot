/**
 * @fileoverview Defines the base class for all UI Elements
 */
// strict mode
"use strict";
Class.load("teapot.util.StaticUtils");
Class.load("teapot.util.Logger");
Class.use("teapot.base.UiElement");
/**
 * @class Abstract Base class for all ui elements
 */
teapot.base.UiElement = Class.extend(
/*
 * public declarations
 */
{
    /**
     * Loads the template and style corresponding to the element if there are any creates the element and appends it to the body adds a data 'class'
     * corresponding to the javascript object of the element
     * @constructor
     * @param {string} baseElement the base element where the UI is to be rendered
     * @param {string} templateNamespace the template namespace of the SOY template
     * @param {string=} optional styleNamespace the namespace of the CSS file or the GSS template
     */
    init : function(baseElement) {
        this._id = teapot.util.StaticUtils.uniqueId();
        // add it to the base element
        this.constructor.LOG.debug('creating element: ', this.constructor.templateNamespace);
        baseElement.append($(eval(this.constructor.templateNamespace)(this)).attr('id', this._id).data('class', this));
    },

    /**
     * destroys the UI element for garbage collection
     * @public
     */
    destroy : function() {
        // remove the object from the view
        $('#' + this._id).remove();
    }
},
/*
 * static declarations
 */
{
    /**
     * @type {string} the style namespace. to be overridden if the new element extending this UI element needs a style namespace
     * @protected
     */
    styleNamespace : null,

    /**
     * @type {string} the template namespace. to be overridden if the new element extending this UI element needs a template namespace
     * @protected
     */
    templateNamespace : null,

    /**
     * This function is called immediately after the class is loaded in memory, if the styleNamespace and templateNamespace are defined, the
     * corresponding Classes are loaded
     * @expose
     */
    postConstruct : function() {
        // load the style if there is a style namespace
        this.LOG.debug('loading style form namespace: ', this.styleNamespace);
        this.styleNamespace && Class.load(this.styleNamespace, Class.ClassType.STYLE);
        // load the template class
        this.LOG.debug('loading template form namespace: ', this.templateNamespace);
        this.templateNamespace && Class.load(this.templateNamespace, Class.ClassType.TEMPLATE);
    },

    /**
     * This function is called immediately before the class is released form memory Here, if the styleNamespace and templateNamespace are defined, the
     * corresponding Classes are unloaded
     * @expose
     */
    preDestroy : function() {
        // unload the template
        Class.unLoad(this.templateNamespace);
        // unload the style
        this.styleNamespace && Class.unLoad(this.styleNamespace);
    },

    /**
     * the logger utility object
     */
    LOG : teapot.util.Logger.getLogger('teapot.base.UiElement')
});