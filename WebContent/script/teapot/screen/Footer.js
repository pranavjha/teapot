/**
 * @fileoverview Defines the class for the home screen
 */
// strict mode
"use strict";
Class.load("teapot.base.UiElement");
Class.load("teapot.util.Logger");
Class.use("teapot.screen.Footer");
/**
 * Defines the class for the builder screen
 */
teapot.screen.Footer = teapot.base.UiElement.extend(
/*
 * public declarations
 */
{
    /**
     * Initializes Home screen
     * @constructor
     * @param {baseElement} $element the jquery base element where the element is to be created
     * @extends {teapot.base.UiElement}
     */
    init : function(baseElement) {
        this._super(baseElement);
    },

    /**
     * disposes off the scene for the dom to be garbage collected
     * @param {jQuery} $element the jQuery element referring to the DOM to be destroyed
     * @returns the jQuery element corresponding to the UI element
     * @override
     */
    destroy : function() {
        this._super();
    }
},
/*
 * static declarations
 */
{
    /**
     * @type {string} the style namespace
     * @private
     */
    styleNamespace : "styles.teapot.screen.Footer",

    /**
     * @type {string} the template namespace
     * @private
     */
    templateNamespace : "templates.teapot.screen.Footer",

    /**
     * the logger utility object
     */
    LOG : teapot.util.Logger.getLogger('teapot.screen.Footer')
});